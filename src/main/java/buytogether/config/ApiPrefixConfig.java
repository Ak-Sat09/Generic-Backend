package com.buytogether.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Optional URL prefix for every endpoint in this module, e.g. "/buy-together" turns
 * /api/groups into /buy-together/api/groups. Empty (the default) when running on its own.
 */
@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

    private final String prefix;

    public ApiPrefixConfig(@Value("${buy-together.api-prefix:}") String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        if (!prefix.isBlank()) {
            configurer.addPathPrefix(prefix, HandlerTypePredicate.forBasePackage("com.buytogether"));
        }
    }
}
