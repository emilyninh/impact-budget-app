package com.impactbudget.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactbudget.common.TransactionScored;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAggregateServiceTest {

    @Mock
    ScoredTransactionRepository repository;
    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;

    BudgetAggregateService service;

    @BeforeEach
    void setUp() {
        service = new BudgetAggregateService(repository, redis, new ObjectMapper());
    }

    private ScoredTransaction row(String amount, int local, int sustainability, boolean independent) {
        ScoredTransaction st = new ScoredTransaction();
        st.setId(UUID.randomUUID());
        st.setTransactionId(UUID.randomUUID());
        st.setUserId("user-1");
        st.setYearMonth("2026-07");
        st.setAmount(new BigDecimal(amount));
        st.setLocalScore(local);
        st.setSustainabilityScore(sustainability);
        st.setLocalIndependent(independent);
        return st;
    }

    @Test
    void rebuildsSpendWeightedImpactPercentagesFromDbOnCacheMiss() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);   // cold cache
        when(repository.findByUserIdAndYearMonth("user-1", "2026-07")).thenReturn(List.of(
                row("100.00", 100, 80, true),   // fully local, quite sustainable
                row("100.00", 0, 20, false)));  // multinational, low sustainability

        BudgetAggregate agg = service.getMonthly("user-1", "2026-07");

        assertThat(agg.totalSpend()).isEqualByComparingTo("200.00");
        // localWeighted = 100*100 + 100*0 = 10000; 10000/200 = 50.0
        assertThat(agg.localImpactPct()).isEqualTo(50.0);
        // sustWeighted = 100*80 + 100*20 = 10000; 10000/200 = 50.0
        assertThat(agg.sustainabilityImpactPct()).isEqualTo(50.0);
        assertThat(agg.localIndependentSpend()).isEqualByComparingTo("100.00");
        assertThat(agg.transactionCount()).isEqualTo(2);

        // Result was written back to the cache.
        verify(valueOps).set(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void incomeAndRefundsAreNotRecorded() {
        // Negative amount = money in; must not create a scored_transaction row.
        TransactionScored refund = new TransactionScored(
                UUID.randomUUID(), "user-1", "Some Store", new BigDecimal("-25.00"), LocalDate.of(2026, 7, 3),
                "Refund", 0, false, 0, List.of(), 0.5, "FALLBACK");

        service.record(refund);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void alreadyRecordedTransactionIsSkipped() {
        UUID txnId = UUID.randomUUID();
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        when(repository.existsByTransactionId(txnId)).thenReturn(true);

        TransactionScored event = new TransactionScored(
                txnId, "user-1", "Coffee Shop", new BigDecimal("10.00"), LocalDate.of(2026, 7, 3),
                "Coffee", 90, true, 60, List.of(), 0.7, "LLM");

        service.record(event);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
