package com.impactbudget.ingestion;

import com.impactbudget.common.PlaidProperties;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.InstitutionsGetByIdRequest;
import com.plaid.client.model.InstitutionsGetByIdResponse;
import com.plaid.client.model.ItemGetRequest;
import com.plaid.client.model.ItemGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

/**
 * Thin wrapper around the Plaid API that owns the low-level Retrofit calls and error
 * handling. Every method is annotated with {@code @Retry} so transient failures are
 * retried with exponential backoff (see {@code resilience4j.retry.instances.plaid}).
 *
 * <p>Kept as its own Spring bean so the retry AOP proxy actually applies — self-invoked
 * methods inside a single bean would bypass it.
 */
@Component
public class PlaidGateway {

    private static final Logger log = LoggerFactory.getLogger(PlaidGateway.class);

    private final PlaidApi plaidApi;
    private final PlaidProperties props;

    public PlaidGateway(PlaidApi plaidApi, PlaidProperties props) {
        this.plaidApi = plaidApi;
        this.props = props;
    }

    @Retry(name = "plaid")
    public String createLinkToken(String userId) {
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(new LinkTokenCreateRequestUser().clientUserId(userId))
                .clientName("Impact Budget")
                .products(List.of(Products.TRANSACTIONS))
                .countryCodes(List.of(CountryCode.US))
                .language("en");
        if (StringUtils.hasText(props.webhookUrl())) {
            request.webhook(props.webhookUrl());
        }
        // Required for OAuth banks (Chase, Capital One, …): Plaid redirects the browser here
        // after the user authenticates, and Link resumes with the same link token. Must match a
        // redirect URI registered in the Plaid Dashboard exactly, or link/token/create is rejected.
        if (StringUtils.hasText(props.redirectUri())) {
            request.redirectUri(props.redirectUri());
        }
        LinkTokenCreateResponse body = execute(request, plaidApi.linkTokenCreate(request));
        return body.getLinkToken();
    }

    @Retry(name = "plaid")
    public ExchangeResult exchangePublicToken(String publicToken) {
        ItemPublicTokenExchangeRequest request =
                new ItemPublicTokenExchangeRequest().publicToken(publicToken);
        ItemPublicTokenExchangeResponse body = execute(request, plaidApi.itemPublicTokenExchange(request));
        return new ExchangeResult(body.getAccessToken(), body.getItemId());
    }

    /**
     * Best-effort lookup of the human institution name for an item (e.g. "Chase", "Capital One"),
     * used to label transactions by account. Two Plaid calls: item/get → institution_id, then
     * institutions/get_by_id → name. Returns null on any failure (a missing label must never break
     * linking or a re-categorization run).
     */
    public String fetchInstitutionName(String accessToken) {
        try {
            ItemGetResponse itemResp = execute(null,
                    plaidApi.itemGet(new ItemGetRequest().accessToken(accessToken)));
            String institutionId = itemResp.getItem() != null ? itemResp.getItem().getInstitutionId() : null;
            if (!StringUtils.hasText(institutionId)) {
                return null;
            }
            InstitutionsGetByIdResponse instResp = execute(null,
                    plaidApi.institutionsGetById(new InstitutionsGetByIdRequest()
                            .institutionId(institutionId)
                            .countryCodes(List.of(CountryCode.US))));
            return instResp.getInstitution() != null ? instResp.getInstitution().getName() : null;
        } catch (RuntimeException e) {
            log.warn("Could not fetch institution name: {}", e.toString());
            return null;
        }
    }

    @Retry(name = "plaid")
    public TransactionsSyncResponse syncTransactions(String accessToken, String cursor) {
        TransactionsSyncRequest request = new TransactionsSyncRequest().accessToken(accessToken);
        if (StringUtils.hasText(cursor)) {
            request.cursor(cursor);
        }
        return execute(request, plaidApi.transactionsSync(request));
    }

    private <T> T execute(Object request, retrofit2.Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful() || response.body() == null) {
                String err = response.errorBody() != null ? errorBodyString(response) : "empty body";
                throw new PlaidException("Plaid call failed (HTTP " + response.code() + "): " + err);
            }
            return response.body();
        } catch (IOException e) {
            throw new PlaidException("Plaid call I/O error", e);
        }
    }

    private String errorBodyString(Response<?> response) {
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            return "<unreadable error body>";
        }
    }

    /** Result of exchanging a Link public token for a persistent access token. */
    public record ExchangeResult(String accessToken, String itemId) {
    }
}
