package com.yourlifeone.finance.ledger.api.controller;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.finance.ledger.api.mapper.LedgerMapper;
import com.yourlifeone.finance.ledger.api.response.LedgerResponse;
import com.yourlifeone.finance.ledger.application.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The whole manual ledger: accounts, transactions, budgets and savings goals. */
@RestController
@RequestMapping("/api/finance/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledger;
    private final LedgerMapper mapper;

    @GetMapping
    public LedgerResponse ledger(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(ledger.ledger(AuthenticatedUser.id(jwt)));
    }
}
