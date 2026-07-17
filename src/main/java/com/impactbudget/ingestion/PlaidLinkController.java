package com.impactbudget.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the frontend calls to drive the Plaid Link flow. The account is linked to the
 * authenticated user (the JWT subject), so each user connects their own bank.
 */
@RestController
@RequestMapping("/api/v1/plaid")
class PlaidLinkController {

    private final PlaidLinkService linkService;

    PlaidLinkController(PlaidLinkService linkService) {
        this.linkService = linkService;
    }

    /** Create a Link token for the browser Plaid Link widget. */
    @PostMapping("/link-token")
    LinkTokenResponse createLinkToken(@AuthenticationPrincipal String userId) {
        return new LinkTokenResponse(linkService.createLinkToken(userId));
    }

    /** Exchange the public token returned by Link for a persistent access token. */
    @PostMapping("/exchange")
    ExchangeResponse exchange(@AuthenticationPrincipal String userId,
                              @Valid @RequestBody ExchangeRequest request) {
        String itemId = linkService.exchangePublicToken(request.publicToken(), userId);
        return new ExchangeResponse(itemId);
    }

    record LinkTokenResponse(String linkToken) {
    }

    record ExchangeRequest(@NotBlank String publicToken) {
    }

    record ExchangeResponse(String itemId) {
    }
}
