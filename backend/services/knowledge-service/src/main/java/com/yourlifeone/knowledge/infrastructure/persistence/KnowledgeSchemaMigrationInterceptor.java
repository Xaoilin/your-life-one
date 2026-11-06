package com.yourlifeone.knowledge.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure every knowledge schema is migrated before a data request is handled. */
public class KnowledgeSchemaMigrationInterceptor implements HandlerInterceptor {
    private final KnowledgeSchemas schemas;

    public KnowledgeSchemaMigrationInterceptor(KnowledgeSchemas schemas) {
        this.schemas = schemas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        schemas.ensureMigrated();
        return true;
    }
}
