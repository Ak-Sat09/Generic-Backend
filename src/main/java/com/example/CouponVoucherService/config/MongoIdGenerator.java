package com.example.CouponVoucherService.config;

import java.util.concurrent.atomic.AtomicLong;

/** Generates process-local numeric IDs while keeping the existing API ID type. */
public final class MongoIdGenerator {
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() * 1000);

    private MongoIdGenerator() { }

    public static long next() {
        return SEQUENCE.incrementAndGet();
    }
}
