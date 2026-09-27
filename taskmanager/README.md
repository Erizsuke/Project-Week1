# Fieldnote — Project Workspace

Spring Boot API and React web client for team project information and document sharing.

## Stack
- Java 17, Spring Boot 3.3
- Spring Security + JWT (access token + rotating refresh token)
- PostgreSQL + Flyway migrations
- Swagger / OpenAPI (springdoc)
- Docker / docker-compose

## Features
- JWT signup, login, rotating refresh tokens, and logout
- Roles: `ADMIN`, `OWNER`, and `USER`; admins manage accounts and all projects
- Project CRUD and project membership management
- Upload, list, search, download, and delete project documents
- Swagger UI at `http://localhost:8080/swagger-ui/index.html`
- React web app at `http://localhost:3000`

## Local setup

### 1. Database
Open pgAdmin 4, create a database named `taskmanager_dev` on your local PostgreSQL instance (matches `application-dev.yml`). No manual table creation needed — Flyway runs the migrations automatically on app startup.

### 2. Bootstrap an admin account
Set these environment variables before the first backend startup. The app replaces the placeholder admin from V2 or creates an admin if one does not exist:

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@yourcompany.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "Choose-a-strong-password-123"
$env:BOOTSTRAP_ADMIN_NAME = "Workspace Admin"
```

The password is BCrypt-hashed and is never logged. Do not commit credentials or put production secrets in YAML.

### 3. Run the app
```bash
mvn spring-boot:run
```
Run this command from the `taskmanager` directory. The default profile is `dev`; it reads `DB_USERNAME`/`DB_PASSWORD` from the environment. Uploaded files are stored in `./uploads` by default; set `UPLOAD_DIR` to change it. Document, image, and video size limits default to 20 MB, 10 MB, and 300 MB respectively. For example, set `$env:MAX_DOCUMENT_UPLOAD_SIZE = "50MB"` before starting the backend to allow larger documents. To exceed the 300 MB request ceiling, also raise `MAX_MULTIPART_FILE_SIZE` and `MAX_MULTIPART_REQUEST_SIZE`.

### 4. Run the web app
```bash
cd web
npm install
npm run dev
```
The Vite server uses port 3000, which is allowed by the backend CORS configuration.

### 5. API routes
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
GET    /api/projects
POST   /api/projects
GET    /api/projects/{projectId}
PUT    /api/projects/{projectId}
DELETE /api/projects/{projectId}
GET/POST/PATCH/DELETE /api/projects/{projectId}/members
GET/POST /api/projects/{projectId}/documents
GET/DELETE /api/projects/{projectId}/documents/{documentId}
GET /api/admin/users
PATCH/DELETE /api/admin/users/{userId}
```
Signup is the only account creation flow and always assigns the `USER` role. Admins can assign roles to registered accounts or deactivate them. The web UI logs in with `/api/auth/login`, refreshes expired access tokens, and attaches `Authorization: Bearer <accessToken>` to protected API calls. Project membership is limited to existing accounts; email delivery for external invitations is not configured.

## Docker
```bash
cp .env.example .env   # fill in real values, especially JWT_SECRET
docker compose up --build
```

## Notes
- `ddl-auto: validate` — schema is fully owned by Flyway migrations, Hibernate never auto-generates tables.
- Refresh tokens rotate on every `/refresh` call (old one is revoked, a new one issued) to reduce replay risk.
- `SystemRole` (ADMIN/OWNER/USER) is the account's system-wide role; `ProjectRole` (OWNER/MEMBER) is per-project — a user can be OWNER of one project and MEMBER of another.
