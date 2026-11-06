package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.McpDomain;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public interface DomainTools {
    McpDomain domain();
    Object invoke(String tool, Map<String, Object> arguments, Jwt token);
}
