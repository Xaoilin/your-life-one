package com.yourlifeone.commonservice.autoconfigure;

import com.yourlifeone.commonservice.persistence.DataAccessExceptionHandler;
import com.yourlifeone.commonservice.web.ApiExceptionHandler;
import com.yourlifeone.commonservice.web.HttpErrorLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonServiceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(HttpErrorLoggingFilter.class)
    HttpErrorLoggingFilter httpErrorLoggingFilter() {
        return new HttpErrorLoggingFilter();
    }

    @Bean
    @ConditionalOnMissingBean(ApiExceptionHandler.class)
    ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DataAccessException.class)
    static class DataAccessErrors {
        @Bean
        @ConditionalOnMissingBean(DataAccessExceptionHandler.class)
        DataAccessExceptionHandler dataAccessExceptionHandler() {
            return new DataAccessExceptionHandler();
        }
    }
}
