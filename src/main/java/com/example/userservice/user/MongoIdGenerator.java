package com.example.userservice.user;

import java.util.concurrent.atomic.AtomicLong;

/** Generates process-local numeric IDs while keeping the existing API ID type. */
final class MongoIdGenerator {
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() * 1000);

    private MongoIdGenerator() { }

    static long next() {
        return SEQUENCE.incrementAndGet();
    }
}
