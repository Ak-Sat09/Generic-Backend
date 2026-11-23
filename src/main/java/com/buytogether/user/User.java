package com.buytogether.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
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

    /** Set for phone logins. Unique when present, see {@link UserIndexes}. */
    private String phone;

    /** Set for email and Google logins. Unique when present, see {@link UserIndexes}. */
    private String email;

    private String name;

    private String city;

    private Instant createdAt;

    private Instant updatedAt;

    public String displayName() {
        return name == null || name.isBlank() ? "Member" : name;
    }
}
