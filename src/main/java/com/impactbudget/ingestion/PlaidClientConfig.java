package com.impactbudget.ingestion;

import com.impactbudget.common.PlaidProperties;
import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the Plaid API client (Retrofit-based) from configured credentials and points it
 * at the sandbox or production adapter.
 */
@Configuration
class PlaidClientConfig {

    @Bean
    PlaidApi plaidApi(PlaidProperties props) {
        Map<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", props.clientId());
        apiKeys.put("secret", props.secret());

        ApiClient apiClient = new ApiClient(apiKeys);
        apiClient.setPlaidAdapter("production".equalsIgnoreCase(props.environment())
                ? ApiClient.Production
                : ApiClient.Sandbox);

        return apiClient.createService(PlaidApi.class);
    }
}
