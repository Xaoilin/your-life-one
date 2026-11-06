package com.yourlifeone.profile.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Makes sure every profile service schema is migrated before a data request is handled. */
public class ProfileSchemaMigrationInterceptor implements HandlerInterceptor {
    private final ProfileSchemas schemas;

    public ProfileSchemaMigrationInterceptor(ProfileSchemas schemas) {
        this.schemas = schemas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        schemas.ensureMigrated();
        return true;
    }
}
