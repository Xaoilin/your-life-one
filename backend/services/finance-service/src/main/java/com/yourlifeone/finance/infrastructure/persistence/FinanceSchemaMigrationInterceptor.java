package com.yourlifeone.finance.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure every finance schema is migrated before a data request is handled. */
public class FinanceSchemaMigrationInterceptor implements HandlerInterceptor {
    private final FinanceSchemas schemas;

    public FinanceSchemaMigrationInterceptor(FinanceSchemas schemas) {
        this.schemas = schemas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        schemas.ensureMigrated();
        return true;
    }
}
