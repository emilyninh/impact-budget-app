package com.impactbudget.ingestion;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the frontend calls to drive the Plaid Link flow. There is no auth in this
 * portfolio build, so a {@code userId} may be supplied (defaults to {@code demo-user}).
 */
@RestController
@RequestMapping("/api/plaid")
class PlaidLinkController {

    private static final String DEFAULT_USER = "demo-user";

    private final PlaidLinkService linkService;

    PlaidLinkController(PlaidLinkService linkService) {
        this.linkService = linkService;
    }

    /** Create a Link token for the browser Plaid Link widget. */
    @PostMapping("/link-token")
    LinkTokenResponse createLinkToken(@RequestBody(required = false) UserRequest request) {
        String userId = (request != null && StringUtils.hasText(request.userId()))
                ? request.userId() : DEFAULT_USER;
        return new LinkTokenResponse(linkService.createLinkToken(userId));
    }

    /** Exchange the public token returned by Link for a persistent access token. */
    @PostMapping("/exchange")
    ExchangeResponse exchange(@RequestBody ExchangeRequest request) {
        String userId = StringUtils.hasText(request.userId()) ? request.userId() : DEFAULT_USER;
        String itemId = linkService.exchangePublicToken(request.publicToken(), userId);
        return new ExchangeResponse(itemId);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record UserRequest(String userId) {
    }

    record LinkTokenResponse(String linkToken) {
    }

    record ExchangeRequest(@NotBlank String publicToken, String userId) {
    }

    record ExchangeResponse(String itemId) {
    }
}
