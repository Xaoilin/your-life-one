package com.yourlifeone.calendar.config;

import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.infrastructure.database.CalendarDatabaseHealthCheck;
import com.yourlifeone.calendar.infrastructure.google.GoogleCalendarRestApi;
import com.yourlifeone.calendar.infrastructure.google.GoogleEventMapper;
import com.yourlifeone.calendar.infrastructure.google.GoogleOAuthRestApi;
import com.yourlifeone.commonservice.persistence.SchemaMigrationInterceptor;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties({CalendarProperties.class, GoogleOAuthProperties.class, GoogleCredentialProperties.class})
public class CalendarConfiguration implements WebMvcConfigurer {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration GOOGLE_READ_TIMEOUT = Duration.ofSeconds(10);

    private final SchemaMigrator migrator;

    public CalendarConfiguration(DataSource dataSource) {
        this.migrator = new SchemaMigrator(dataSource, "calendar");
    }

    @Bean
    SchemaMigrator calendarSchemaMigrator() {
        return migrator;
    }

    /** Every data request first makes sure the calendar schema is migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SchemaMigrationInterceptor(migrator)).addPathPatterns("/api/calendar/v1/**");
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    GoogleCalendarApi googleCalendarApi(CalendarProperties properties, GoogleEventMapper mapper,
            RestClient.Builder builder) {
        return new GoogleCalendarRestApi(restClient(builder.clone(), properties.googleApiBaseUrl()), mapper);
    }

    @Bean
    GoogleOAuthApi googleOAuthApi(CalendarProperties calendar, GoogleOAuthProperties oauth, Clock clock,
            RestClient.Builder builder) {
        return new GoogleOAuthRestApi(restClientBuilder(builder.clone()).build(),
                restClient(builder.clone(), calendar.googleApiBaseUrl()), oauth, clock);
    }

    @Bean
    CalendarDatabaseHealthCheck calendarDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new CalendarDatabaseHealthCheck(jdbcTemplate);
    }

    private static RestClient restClient(RestClient.Builder builder, String baseUrl) {
        return restClientBuilder(builder).baseUrl(baseUrl).build();
    }

    private static RestClient.Builder restClientBuilder(RestClient.Builder builder) {
        var httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(GOOGLE_READ_TIMEOUT);
        return builder.requestFactory(requestFactory);
    }
}
