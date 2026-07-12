package com.impactbudget.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactbudget.common.TransactionScored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Maintains monthly impact aggregates per user.
 *
 * <p>Redis holds the computed aggregate as a rebuildable cache. It is invalidated whenever
 * a new scored transaction is recorded (event-driven), and a cold read rebuilds it from the
 * budget-owned {@link ScoredTransaction} table. Redis being down never breaks reads — the
 * service just recomputes from Postgres.
 */
@Service
public class BudgetAggregateService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAggregateService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ScoredTransactionRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public BudgetAggregateService(ScoredTransactionRepository repository,
                                  StringRedisTemplate redis,
                                  ObjectMapper mapper) {
        this.repository = repository;
        this.redis = redis;
        this.mapper = mapper;
    }

    /** Record a scored transaction into the budget projection and invalidate its month cache. */
    public void record(TransactionScored event) {
        // Only outflow spend contributes to the aggregate; skip income/refunds.
        if (event.amount() == null || event.amount().signum() <= 0) {
            return;
        }
        if (repository.existsByTransactionId(event.transactionId())) {
            return; // idempotent on re-delivery
        }

        String yearMonth = YearMonth.from(event.txnDate()).toString();
        ScoredTransaction st = new ScoredTransaction();
        st.setId(UUID.randomUUID());
        st.setTransactionId(event.transactionId());
        st.setUserId(event.userId());
        st.setMerchantName(event.merchantName());
        st.setYearMonth(yearMonth);
        st.setTxnDate(event.txnDate());
        st.setAmount(event.amount());
        st.setLocalScore(event.localScore());
        st.setSustainabilityScore(event.sustainabilityScore());
        st.setLocalIndependent(event.localIndependent());

        try {
            repository.save(st);
        } catch (DataIntegrityViolationException race) {
            return; // another consumer recorded it first
        }
        invalidate(event.userId(), yearMonth);
    }

    /** Read the aggregate for a user/month, serving from Redis or rebuilding from Postgres. */
    public BudgetAggregate getMonthly(String userId, String yearMonth) {
        String key = key(userId, yearMonth);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return mapper.readValue(cached, BudgetAggregate.class);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {} ({}); rebuilding from DB", key, e.toString());
        }

        BudgetAggregate aggregate = rebuild(userId, yearMonth);

        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(aggregate), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis write failed for {} ({}); serving uncached", key, e.toString());
        }
        return aggregate;
    }

    /** Trend of the last {@code months} monthly aggregates, oldest first (for the UI line chart). */
    public List<BudgetAggregate> trend(String userId, int months) {
        YearMonth current = YearMonth.now();
        java.util.List<BudgetAggregate> out = new java.util.ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            out.add(getMonthly(userId, current.minusMonths(i).toString()));
        }
        return out;
    }

    /** Recent scored transactions for a month, newest first (for the UI list). */
    public List<ScoredTransactionView> recentTransactions(String userId, String yearMonth) {
        return repository.findByUserIdAndYearMonthOrderByTxnDateDesc(userId, yearMonth).stream()
                .map(ScoredTransactionView::from)
                .toList();
    }

    private BudgetAggregate rebuild(String userId, String yearMonth) {
        List<ScoredTransaction> rows = repository.findByUserIdAndYearMonth(userId, yearMonth);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal localWeighted = BigDecimal.ZERO;        // sum(amount * localScore)
        BigDecimal sustainabilityWeighted = BigDecimal.ZERO;
        BigDecimal localIndependentSpend = BigDecimal.ZERO;

        for (ScoredTransaction st : rows) {
            BigDecimal amount = st.getAmount();
            total = total.add(amount);
            localWeighted = localWeighted.add(amount.multiply(BigDecimal.valueOf(st.getLocalScore())));
            sustainabilityWeighted =
                    sustainabilityWeighted.add(amount.multiply(BigDecimal.valueOf(st.getSustainabilityScore())));
            if (st.isLocalIndependent()) {
                localIndependentSpend = localIndependentSpend.add(amount);
            }
        }

        // localImpactPct = sum(amount*score/100)/total*100 = sum(amount*score)/total.
        double localPct = pct(localWeighted, total);
        double sustainabilityPct = pct(sustainabilityWeighted, total);

        return new BudgetAggregate(
                userId,
                yearMonth,
                total.setScale(2, RoundingMode.HALF_UP),
                localPct,
                sustainabilityPct,
                localIndependentSpend.setScale(2, RoundingMode.HALF_UP),
                rows.size());
    }

    private double pct(BigDecimal weighted, BigDecimal total) {
        if (total.signum() <= 0) {
            return 0.0;
        }
        double raw = weighted.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.round(raw * 10.0) / 10.0;   // one decimal place
    }

    private void invalidate(String userId, String yearMonth) {
        try {
            redis.delete(key(userId, yearMonth));
        } catch (Exception e) {
            log.warn("Redis invalidate failed for {} ({})", key(userId, yearMonth), e.toString());
        }
    }

    private String key(String userId, String yearMonth) {
        return "budget:" + userId + ":" + yearMonth;
    }
}
