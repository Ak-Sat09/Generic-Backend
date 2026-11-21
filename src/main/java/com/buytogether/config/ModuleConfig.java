package com.buytogether.config;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Everything the module needs to switch itself on, so it works both as its own application
 * and as a package inside a larger one: MongoDB repositories and documents under
 * com.buytogether are registered, settings records are bound, and the nightly job runs.
 */
@Configuration
@AutoConfigurationPackage(basePackages = "com.buytogether")
@ConfigurationPropertiesScan("com.buytogether")
@EnableScheduling
public class ModuleConfig {
}
