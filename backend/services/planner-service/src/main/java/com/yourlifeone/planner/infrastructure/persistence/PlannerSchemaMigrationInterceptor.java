package com.yourlifeone.planner.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure every planner schema is migrated before a data request is handled. */
public class PlannerSchemaMigrationInterceptor implements HandlerInterceptor {
    private final PlannerSchemas schemas;

    public PlannerSchemaMigrationInterceptor(PlannerSchemas schemas) {
        this.schemas = schemas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        schemas.ensureMigrated();
        return true;
    }
}
