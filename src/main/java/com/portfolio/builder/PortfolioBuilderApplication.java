package com.portfolio.builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortfolioBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioBuilderApplication.class, args);
    }
}
