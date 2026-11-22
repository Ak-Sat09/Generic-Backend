package com.buytogether.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("bt_users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String phone;

    private String name;

    private String city;

    private Instant createdAt;

    private Instant updatedAt;

    public String displayName() {
        return name == null || name.isBlank() ? "Member" : name;
    }
}
