# Backend

Java 17 Spring backend for the photo storage platform.

## Package Layout

- `controller`: REST API endpoints
- `service`: business logic
- `repository`: database access
- `model`: JPA entities and enums
- `dto`: request and response records
- `storage`: local, AWS S3, and GCP Cloud Storage storage boundary
- `config`: Spring configuration
- `common`: shared exceptions and request helpers

## Implemented MVP

1. Signup, login, and social login
2. Token-based authentication
3. Photo upload, list, download, and delete
4. Share link creation and download
5. Upload size limit
6. Simple admin statistics
7. Local storage implementation

## Social Login

Google, Naver, and Kakao login are wired through Spring Security OAuth2 Client.
Create OAuth apps in each provider console and register this redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/naver
http://localhost:8080/login/oauth2/code/kakao
```

Run the backend with the `oauth` profile and provider credentials:

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export NAVER_CLIENT_ID=...
export NAVER_CLIENT_SECRET=...
export KAKAO_CLIENT_ID=...
export FRONTEND_URL=http://localhost:5173
mvn spring-boot:run -Dspring-boot.run.profiles=oauth
```

If Vite starts on `5174`, set `FRONTEND_URL=http://localhost:5174` before running the backend.
