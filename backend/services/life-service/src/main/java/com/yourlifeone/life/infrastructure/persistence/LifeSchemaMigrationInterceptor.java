package com.yourlifeone.life.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure every life schema is migrated before a data request is handled. */
public class LifeSchemaMigrationInterceptor implements HandlerInterceptor {
    private final LifeSchemas schemas;

    public LifeSchemaMigrationInterceptor(LifeSchemas schemas) {
        this.schemas = schemas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        schemas.ensureMigrated();
        return true;
    }
}
