# Unified Backend

This is the single Spring Boot backend for all the apps. Each one is built as a separate service, then deployed together on one AWS EC2 instance to keep hosting costs low. It combines:

| Module | Package | Paths |
|---|---|---|
| Users: register and login | `com.example.userservice` | `/api/users` |
| Blog: blogs, comments, likes, live editing | `com.example.Blog_Service` | `/blogs`, `/comments`, `/likes`, `/share`, WebSocket `/ws` |
| Coupons: seller and admin workflows, chat | `com.example.CouponVoucherService` | `/api/seller`, `/api/coupons`, `/api/coupon-requests`, `/api/chats`, `/api/admin` |
| Buy Together: car buyers forming groups | `com.buytogether` | everything under `/buy-together` |

## Buy Together module

The `com.buytogether` package is the same code as the standalone [buy-together](https://github.com/Ak-Sat09/buy-together) repository. To update it, copy that repository's `src/main/java/com/buytogether` over this one, leaving out `BuyTogetherApplication.java`.

How it fits in without affecting the other modules:

- **Its own URL prefix.** `buy-together.api-prefix: /buy-together` serves `/api/groups` as `/buy-together/api/groups`.
- **Security limited to its paths.** It uses Spring Security with phone OTP login and its own JWT. The security chain only matches `/buy-together/**`, so the blog, coupon and user endpoints work exactly as before.
- **Separate settings.** Everything is under `buy-together:` in `application.yaml`, with its own JWT secret (`BT_JWT_SECRET`).
- **Its own error format.** Its error handler only covers `com.buytogether`, so Buy Together errors keep their `problem+json` format, and the coupon error handler is unchanged.
- **Separate collections.** Data lives in the same MongoDB database: `users`, `groups`, `group_members`, `group_messages` and `group_reports`. None of these names clash with the existing collections.

Frontend: [buy-together-frontend](https://github.com/Ak-Sat09/buy-together-frontend). Point its `VITE_API_URL` at `https://<this server>/buy-together`.

## Run

```bash
mvn spring-boot:run
```

The backend listens on `http://localhost:8080` by default. Set `SERVER_PORT`, `SPRING_DATA_MONGODB_URI` and the other values through environment variables when deploying.

| Variable | Used by | Purpose |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | all | MongoDB connection string |
| `JWT_SECRET` | users, blog, coupons | Signing secret for their tokens |
| `BT_JWT_SECRET` | Buy Together | Signing secret for Buy Together tokens, at least 32 characters |
| `BT_CORS_ALLOWED_ORIGINS` | Buy Together | Allowed frontend origins (default: localhost and `https://*.vercel.app`) |
| `BT_OTP_SENDER` | Buy Together | `log` prints OTPs to the server log (development) |

## Build and tests

```bash
mvn verify
```

The tests start the whole application and check that the modules don't get in each other's way: existing endpoints stay open, and Buy Together endpoints sit under their prefix and require login. They also run Buy Together's unit tests. GitHub Actions runs them on every push.
