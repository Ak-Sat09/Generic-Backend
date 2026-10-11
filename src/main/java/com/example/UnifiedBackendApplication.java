package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single application entry point for all backend features.
 * Component scanning starts at com.example so the existing user, blog,
 * coupon, and chat packages are loaded into the same Spring context.
 */
@SpringBootApplication
public class UnifiedBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedBackendApplication.class, args);
    }
}
