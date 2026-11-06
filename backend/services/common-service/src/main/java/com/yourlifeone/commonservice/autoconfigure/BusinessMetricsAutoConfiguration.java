package com.yourlifeone.commonservice.autoconfigure;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.metrics.BusinessMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Publishes the {@link BusinessGauge} beans a service declares. */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@EnableScheduling
@EnableConfigurationProperties(BusinessMetricsProperties.class)
public class BusinessMetricsAutoConfiguration {
    /** The meter registry is resolved when the bean is created, after the actuator has defined it. */
    @Bean
    @ConditionalOnBean(BusinessGauge.class)
    BusinessGaugePublisher businessGaugePublisher(List<BusinessGauge> gauges, ObjectProvider<MeterRegistry> meters,
            BusinessMetricsProperties properties) {
        return new BusinessGaugePublisher(gauges, meters.getIfAvailable(SimpleMeterRegistry::new), properties);
    }
}
