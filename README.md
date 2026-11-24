# Unified Backend

This is the single Spring Boot backend for all the apps. Each one is built as a separate service, then deployed together on one AWS EC2 instance to keep hosting costs low. It combines:

| Module | Package | Paths |
|---|---|---|
| Users: register and login | `com.example.userservice` | `/api/users` |
| Blog: blogs, comments, likes, live editing | `com.example.Blog_Service` | `/blogs`, `/comments`, `/likes`, `/share`, WebSocket `/ws` |
| Coupons: seller and admin workflows, chat | `com.example.CouponVoucherService` | `/api/seller`, `/api/coupons`, `/api/coupon-requests`, `/api/chats`, `/api/admin` |
| Buy Together: car buyers forming groups | `com.buytogether` | everything under `/buy-together` |
| Email outreach: nightly reply tracking | `com.example.emailoutreach` | no endpoints, a scheduled job |

## Buy Together module

The `com.buytogether` package is the same code as the standalone [buy-together](https://github.com/Ak-Sat09/buy-together) repository. To update it, copy that repository's `src/main/java/com/buytogether` over this one, leaving out `BuyTogetherApplication.java`.

How it fits in without affecting the other modules:

- **Its own URL prefix.** `buy-together.api-prefix: /buy-together` serves `/api/groups` as `/buy-together/api/groups`.
- **Security limited to its paths.** It uses Spring Security with phone OTP login and its own JWT. The security chain only matches `/buy-together/**`, so the blog, coupon and user endpoints work exactly as before.
- **Separate settings.** Everything is under `buy-together:` in `application.yaml`, with its own JWT secret (`BT_JWT_SECRET`).
- **Its own error format.** Its error handler only covers `com.buytogether`, so Buy Together errors keep their `problem+json` format, and the coupon error handler is unchanged.
- **Copied without its main class.** When syncing code from the `buy-together` repo, copy `src/main/java/com/buytogether` but leave out `BuyTogetherApplication.java`. A second main class stops the jar from being built.
- **Separate collections.** Data lives in the same MongoDB database, in collections that all start with `bt_`: `bt_users`, `bt_groups`, `bt_group_members`, `bt_group_messages` and `bt_group_reports`. The prefix keeps them clear of older collections. For example, an existing `users` collection has a unique email index that Buy Together's phone-only users would break.

Frontend: [buy-together-frontend](https://github.com/Ak-Sat09/buy-together-frontend). Point its `VITE_API_URL` at `https://<this server>/buy-together`.

## Email outreach module

Tracks replies to job-application emails. The contact list (`email_contacts`) was filled once by a backfill that ran locally. This module only adds the nightly job that keeps reply status up to date.

**What the job does, every night at 2 AM (Asia/Kolkata):**

1. Connects to Gmail over IMAP, read-only and headers only. Mail bodies and attachments are never downloaded, and nothing in the mailbox changes.
2. Reads **All Mail** and **Bin** from the last successful run (the last 24 hours on the first run). All Mail includes the inbox and archived mail. Folders are found by their IMAP special-use flag, so a Gmail account in any language works ("Bin" or "Trash").
3. Ignores your own mail, bounces (`mailer-daemon`, `no-reply`) and automatic replies (out-of-office, detected through `Auto-Submitted`, `X-Autoreply` and `Precedence` headers, plus common subjects).
4. A reply from a known contact marks them `RESPONDED`, with `respondedAt` set to the first reply. If someone else at the company answers (for example, you wrote to `hr@tcs.com` and `priya@tcs.com` replied), the `In-Reply-To` header leads back to your original mail, and the person you wrote to is marked.
5. Saves the result in `sync_state` (`_id: "reply-sync"`: `lastSuccessAt`, `lastScanned`, `lastMarked`, `lastError`). If a night is missed, the next run starts from the last success, so nothing is skipped.

Other details:

- **Data.** It uses its own database (`email_outreach`, the same Atlas cluster), not `coupon_voucher`. It never creates contacts, and it never changes a contact that is already `RESPONDED`.
- **Waits for the backfill.** It skips the run while `sync_state` `backfill` is `RUNNING`.
- **Schedule.** `email-outreach.reply-sync.enabled` and `cron` are set in `application.yaml` (on, 2 AM). If no Gmail credentials are set, the job logs an error and does nothing.

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
| `BT_OTP_SENDER` | Buy Together | `log` prints OTPs to the server log; `twilio` sends a real SMS |
| `BT_TWILIO_ACCOUNT_SID` | Buy Together | Twilio account SID, needed with `twilio` |
| `BT_TWILIO_AUTH_TOKEN` | Buy Together | Twilio auth token, needed with `twilio` |
| `BT_TWILIO_FROM_NUMBER` | Buy Together | Twilio phone number, e.g. `+15551234567` |
| `BT_EMAIL_OTP_SENDER` | Buy Together | `log` prints email OTPs to the server log; `smtp` sends real emails |
| `BT_MAIL_USERNAME` / `BT_MAIL_PASSWORD` | Buy Together | SMTP login (for Gmail, the address and an app password) |
| `BT_MAIL_HOST` / `BT_MAIL_PORT` | Buy Together | SMTP server, default `smtp.gmail.com` / `587` (AWS blocks port 25) |
| `BT_GOOGLE_CLIENT_ID` | Buy Together | OAuth client id for Google login; Google login is off while empty |
| `EMAIL_OUTREACH_GMAIL_USERNAME` / `EMAIL_OUTREACH_GMAIL_APP_PASSWORD` | Email outreach | Gmail used for IMAP. If unset, falls back to `BT_MAIL_USERNAME` / `BT_MAIL_PASSWORD` (the same account) |
| `EMAIL_OUTREACH_DB` | Email outreach | Database holding `email_contacts` and `sync_state`, default `email_outreach` |

## Build and tests

```bash
mvn verify
```

The tests start the whole application and check that the modules don't get in each other's way: existing endpoints stay open, and Buy Together endpoints sit under their prefix and require login. They also run Buy Together's unit tests. GitHub Actions runs them on every push.
