package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.api.command.CreateAccountHandler;
import com.yourlifeone.calendar.api.command.DeleteAccountCommand;
import com.yourlifeone.calendar.api.command.DeleteAccountHandler;
import com.yourlifeone.calendar.api.command.UpdateAccountCommand;
import com.yourlifeone.calendar.api.command.UpdateAccountHandler;
import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.request.ConnectGoogleAccountRequest;
import com.yourlifeone.calendar.api.request.CreateAccountRequest;
import com.yourlifeone.calendar.api.request.UpdateAccountRequest;
import com.yourlifeone.calendar.api.response.AccountResponse;
import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.calendar.application.service.GoogleAccountConnector;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Calendar accounts. Local-account writes go through the calendar command queue. */
@RestController
@RequestMapping("/api/calendar/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;
    private final GoogleAccountConnector googleAccounts;
    private final CalendarResponseMapper responses;
    private final WriteCommandGateway commands;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateAccountRequest request) {
        return WriteCommandResponses.of(commands.send(CreateAccountHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, AccountResponse.class));
    }

    /**
     * Connects (or reconnects) a Google account from a consent-popup authorization code, then syncs
     * it. Not queued: the code is single-use and the account is keyed by its Google address.
     */
    @PostMapping("/google")
    public AccountResponse connectGoogle(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectGoogleAccountRequest request) {
        return responses.toResponse(googleAccounts.connect(AuthenticatedUser.id(jwt),
                request.code(), request.redirectUri(), request.expectedEmail()));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody UpdateAccountRequest request) {
        return WriteCommandResponses.of(commands.send(UpdateAccountHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new UpdateAccountCommand(accountId, request), AccountResponse.class));
    }

    /** Deletes the account; a Google account's stored credential is also revoked. */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        var userId = AuthenticatedUser.id(jwt);
        // Looked up before deleting, so a Google account's credential can be revoked afterwards.
        var account = accounts.lookup(userId, accountId);
        var result = commands.send(DeleteAccountHandler.TYPE, userId, idempotencyKey,
                new DeleteAccountCommand(accountId), Void.class);
        account.filter(CalendarAccount::isGoogle)
                .ifPresent(google -> googleAccounts.revokeQuietly(userId, google));
        return WriteCommandResponses.of(result);
    }
}
