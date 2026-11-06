package com.yourlifeone.finance.security;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.config.AgentAccessProperties;
import com.yourlifeone.finance.infrastructure.database.AgentApprovalRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Keeps each AI agent inside the domains its user approved. A token with a {@code client_id} claim
 * was issued to an OAuth client (an agent), not to the Your Life One app: it may reach
 * {@code /api/finance/v1/<domain>} only when that domain has an approval table and the user's approval
 * of the client there is active. The app's own tokens have no {@code client_id} and pass.
 */
public class AgentAccessInterceptor implements HandlerInterceptor {
    private static final String PREFIX = "/api/finance/v1/";
    private static final String CLIENT_ID_CLAIM = "client_id";

    private final AgentAccessProperties properties;
    private final AgentApprovalRepository approvals;

    public AgentAccessInterceptor(AgentAccessProperties properties, AgentApprovalRepository approvals) {
        this.properties = properties;
        this.approvals = approvals;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            return true;
        }
        var clientId = token.getToken().getClaimAsString(CLIENT_ID_CLAIM);
        if (clientId == null || clientId.isBlank()) return true;
        var table = properties.approvals().get(domainOf(request));
        if (table == null || !approvals.isApproved(table, AuthenticatedUser.id(token.getToken()), clientId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "agent_not_approved",
                    "This agent is not approved for this part of Your Life One.");
        }
        return true;
    }

    private static String domainOf(HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith(PREFIX)) return "";
        var rest = path.substring(PREFIX.length());
        var slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
