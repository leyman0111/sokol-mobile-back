package com.sokolmeteo.back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.sokolmeteo.back", "com.sokolmeteo.sokol"})
@EnableJpaRepositories(basePackages = "com.sokolmeteo.dao")
@EntityScan(basePackages = "com.sokolmeteo.dao")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
