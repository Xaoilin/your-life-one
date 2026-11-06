package com.yourlifeone.prayer.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrationInterceptor;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.application.port.PrayerDatabaseHealthCheck;
import com.yourlifeone.prayer.infrastructure.theshia.PrayerLocationResolver;
import com.yourlifeone.prayer.infrastructure.theshia.TheShiaScheduleSource;
import com.yourlifeone.prayer.infrastructure.theshia.TheShiaTimetableMapper;
import com.yourlifeone.prayer.infrastructure.database.JdbcPrayerDatabaseHealthCheck;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties({PrayerProperties.class, ReminderProperties.class})
public class PrayerConfiguration implements WebMvcConfigurer {
    private static final Duration PROVIDER_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration PROVIDER_READ_TIMEOUT = Duration.ofSeconds(3);

    private final SchemaMigrator migrator;

    public PrayerConfiguration(DataSource dataSource) {
        this.migrator = new SchemaMigrator(dataSource, "prayer");
    }

    @Bean
    SchemaMigrator prayerSchemaMigrator() {
        return migrator;
    }

    /** Every data request first makes sure the prayer schema is migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SchemaMigrationInterceptor(migrator)).addPathPatterns("/api/prayer/v1/**");
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ScheduleSource scheduleSource(PrayerProperties properties, TheShiaTimetableMapper mapper, Clock clock,
            RestClient.Builder builder) {
        var httpClient = HttpClient.newBuilder().connectTimeout(PROVIDER_CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(PROVIDER_READ_TIMEOUT);
        var provider = providerClient(builder.clone(), requestFactory, properties.theShiaBaseUrl(),
                properties.providerUserAgent());
        var geocoding = providerClient(builder.clone(), requestFactory, properties.geocodingBaseUrl(),
                properties.providerUserAgent());
        return TheShiaScheduleSource.builder().provider(provider).locations(new PrayerLocationResolver(geocoding))
                .mapper(mapper).clock(clock).build();
    }

    private static RestClient providerClient(RestClient.Builder builder, JdkClientHttpRequestFactory requestFactory,
            String baseUrl, String userAgent) {
        return builder.baseUrl(baseUrl).requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent).build();
    }

    @Bean
    PrayerDatabaseHealthCheck prayerDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new JdbcPrayerDatabaseHealthCheck(jdbcTemplate);
    }
}
