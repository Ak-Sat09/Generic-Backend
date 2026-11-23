package com.buytogether.user;

import com.buytogether.common.email.EmailAddress;
import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.phone.PhoneNumber;
import com.buytogether.common.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository users;
    private final Clock clock;

    /**
     * Every login doubles as sign up: the first verified login creates the account.
     */
    public SignInResult signIn(PhoneNumber phone, String nameOnSignUp) {
        return signIn(() -> users.findByPhone(phone.value()),
                builder -> builder.phone(phone.value()), nameOnSignUp);
    }

    /**
     * Email OTP and Google both land here, so the same inbox always opens the same account.
     */
    public SignInResult signIn(EmailAddress email, String nameOnSignUp) {
        return signIn(() -> users.findByEmail(email.value()),
                builder -> builder.email(email.value()), nameOnSignUp);
    }

    public User get(String userId) {
        return users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User updateProfile(String userId, String name, String city) {
        User user = get(userId);
        user.setName(Text.clean(name));
        user.setCity(Text.clean(city));
        user.setUpdatedAt(clock.instant());
        return users.save(user);
    }

    public Map<String, User> findAllById(Collection<String> ids) {
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private SignInResult signIn(Supplier<Optional<User>> lookup, UnaryOperator<User.UserBuilder> identity,
                                String nameOnSignUp) {
        Optional<User> existing = lookup.get();
        if (existing.isPresent()) {
            return new SignInResult(existing.get(), false);
        }
        Instant now = clock.instant();
        User user = identity.apply(User.builder())
                .name(Text.clean(nameOnSignUp))
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            return new SignInResult(users.save(user), true);
        } catch (DuplicateKeyException raceWithAnotherLogin) {
            return new SignInResult(lookup.get().orElseThrow(() -> raceWithAnotherLogin), false);
        }
    }

    public record SignInResult(User user, boolean newUser) {
    }
}
