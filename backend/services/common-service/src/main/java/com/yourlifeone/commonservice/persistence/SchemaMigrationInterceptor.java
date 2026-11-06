package com.yourlifeone.commonservice.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure the service schema is migrated before any data request is handled. */
public class SchemaMigrationInterceptor implements HandlerInterceptor {
    private final SchemaMigrator migrator;

    public SchemaMigrationInterceptor(SchemaMigrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        migrator.ensureMigrated();
        return true;
    }
}
