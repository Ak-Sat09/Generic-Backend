package com.buytogether.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<User, String> {

    Optional<User> findByPhone(String phone);
}
