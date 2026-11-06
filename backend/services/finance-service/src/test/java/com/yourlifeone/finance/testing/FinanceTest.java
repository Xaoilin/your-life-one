package com.yourlifeone.finance.testing;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * One shared application context for every finance integration test: a real PostgreSQL and RabbitMQ and a
 * clock the test sets.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class, TestClock.class})
public @interface FinanceTest {
}
