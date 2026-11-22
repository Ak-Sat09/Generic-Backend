package com.buytogether.group;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class BrandController {

    private static final List<BrandResponse> BRANDS = Arrays.stream(CarBrand.values())
            .map(brand -> new BrandResponse(brand, brand.displayName()))
            .toList();

    @GetMapping("/api/brands")
    public List<BrandResponse> brands() {
        return BRANDS;
    }

    public record BrandResponse(CarBrand code, String name) {
    }
}
