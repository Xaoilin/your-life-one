package com.yourlifeone.access.identity.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Preserve account-wide browser sign-out across devices without touching separate Calendar credentials. */
@Component
@RequiredArgsConstructor
public class AccountSessionLogout implements LogoutHandler {
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            sessions.findByPrincipalName(authentication.getName()).keySet().forEach(sessions::deleteById);
        }
    }
}
