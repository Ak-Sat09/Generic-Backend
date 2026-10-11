# Unified Backend

This is the single Spring Boot backend for the application. It combines:

- user registration and login (`/api/users`)
- blog, comments, likes, and blog WebSocket endpoints
- coupons, seller/admin workflows, and chat

Run it from this directory with:

```powershell
./mvnw spring-boot:run
```

The backend listens on `http://localhost:8080` by default. Set `SERVER_PORT`,
`SPRING_DATA_MONGODB_URI`, and other values through environment variables when
deploying.
