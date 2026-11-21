package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single application entry point for all backend features.
 * Component scanning covers com.example (user, blog, coupon and chat) and
 * com.buytogether (the Buy Together module, served under /buy-together).
 */
@SpringBootApplication(scanBasePackages = {"com.example", "com.buytogether"})
public class UnifiedBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedBackendApplication.class, args);
    }
}
