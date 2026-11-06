package com.yourlifeone.planner.testing;

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
 * One shared application context for every planner integration test: a real PostgreSQL and RabbitMQ, a
 * clock the test sets, stubbed prayer and knowledge services, and the scheduled daily reset out of the way.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.datasource.hikari.data-source-properties.sslmode=disable",
        "app.planner.tasks.rollover-interval=PT24H"})
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class, TestClock.class, UpstreamStubs.class})
public @interface PlannerTest {
}
