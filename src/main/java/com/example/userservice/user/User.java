package com.example.userservice.user;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Document(collection = "app_users")
@Getter @Setter @NoArgsConstructor
public class User {
    private Long id = MongoIdGenerator.next();
    private String name;
    @Indexed(unique = true) private String email;
    private String passwordHash;
    private Instant createdAt;

    public User(String name, String email, String passwordHash) {
        this.name = name; this.email = email; this.passwordHash = passwordHash; this.createdAt = Instant.now();
    }
}
