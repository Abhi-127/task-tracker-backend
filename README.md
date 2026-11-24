# Task Tracking & Management Backend

A RESTful backend service for team collaboration and task management built with **Java 25**, **Spring Boot 4.2**, **Spring Security (JWT)**, **Spring Data JPA**, and **PostgreSQL**.

Users can register and manage their profiles, organize teams/projects, create and assign tasks with filtering, sorting, and search, and collaborate through comments and file attachments.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Configuration & Environment Variables](#configuration--environment-variables)
- [Authentication Flow](#authentication-flow)
- [API Reference](#api-reference)
- [Sample API Requests](#sample-api-requests)
- [Error Handling](#error-handling)

---

## Features

- **User Authentication & Profile Management**
    - Secure user registration and login with **BCrypt** password hashing.
    - Stateless **JWT (JSON Web Token)** authentication for protected endpoints.
    - Server-side **logout** via token blacklisting so revoked tokens cannot be reused.
    - View and update authenticated user profile (`name`, `bio`).
- **Task Management**
    - Full CRUD operations for tasks (`title`, `description`, `status`, `dueDate`).
    - View tasks assigned to the logged-in user (`/api/tasks/my`).
    - Mark tasks as `COMPLETED` and assign tasks to team members (with team membership validation).
    - Filter tasks by `status` (`OPEN`, `IN_PROGRESS`, `COMPLETED`), `teamId`, or `assignedToId`.
    - Case-insensitive keyword search across `title` and `description`, plus dynamic sorting (`dueDate`, `title`, `status`, `createdAt`, `updatedAt`).
- **Team & Project Collaboration**
    - Create teams/projects, invite existing users by email, join teams, and view team-specific tasks.
    - Add and list chronological comments on any task.
    - Upload (`multipart/form-data`), list, and download file attachments on tasks.
- **Validation & Centralized Error Handling**
    - Request payload validation with Jakarta Bean Validation (`@Valid`).
    - Standardized JSON error responses handled globally via `@RestControllerAdvice`.

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| **Language** | Java 25 (LTS) |
| **Framework** | Spring Boot 4.2.0 (Spring Web, Spring Data JPA, Spring Security, Validation) |
| **Authentication** | JJWT (`0.13.0`) + BCrypt Password Encoder |
| **Database** | PostgreSQL |
| **ORM** | Hibernate / Jakarta Persistence (JPA) |
| **Build Tool** | Apache Maven (`mvnw` / `mvnw.cmd`) |

---

## Project Structure

```text
task-tracker-backend/
├── src/
│   └── main/
│       ├── java/com/tasktracker/backend/
│       │   ├── controller/
│       │   │   ├── AuthController.java          # Register, Login, Logout
│       │   │   ├── UserController.java          # Profile view & update
│       │   │   ├── TaskController.java          # Task CRUD, filter, sort, search, assign, complete
│       │   │   ├── TeamController.java          # Team create, invite, join, team tasks
│       │   │   └── CollaborationController.java # Task comments & file attachments
│       │   ├── dto/                             # Java Records for request/response payloads
│       │   ├── exception/
│       │   │   ├── ApiException.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── model/                           # JPA Entities (User, Team, Task, Comment, Attachment, TaskStatus)
│       │   ├── repository/                      # Spring Data JPA Repositories
│       │   ├── security/
│       │   │   ├── JwtService.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── TokenBlacklistService.java
│       │   │   └── SecurityConfig.java
│       │   └── TaskTrackerBackendApplication.java
│       └── resources/
│           └── application.properties
├── pom.xml
└── README.md
```

---

## Database Schema

Hibernate automatically creates and updates the following PostgreSQL tables on startup (`spring.jpa.hibernate.ddl-auto=update`):

| Table | Purpose | Key Relationships |
| --- | --- | --- |
| `users` | Stores registered user accounts and profile info | Unique `email`, BCrypt-hashed `password` |
| `teams` | Stores teams / projects | `created_by_id` -> `users(id)` |
| `team_members` | Join table for team membership | Many-to-many between `teams` and `users` |
| `tasks` | Stores tasks with status and due dates | `team_id` -> `teams(id)`, `created_by_id` / `assigned_to_id` -> `users(id)` |
| `comments` | Stores collaboration comments on tasks | `task_id` -> `tasks(id)`, `author_id` -> `users(id)` |
| `attachments` | Stores metadata & storage path of uploaded files | `task_id` -> `tasks(id)`, `uploaded_by_id` -> `users(id)` |

---

## Getting Started

### Prerequisites

- **JDK 21+** (developed and tested on **Java 25 LTS**)
- **PostgreSQL 15+**
- **Git** and **Postman** (for testing APIs)

### Installation & Local Run

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/Abhi-127/task-tracker-backend.git](https://github.com/Abhi-127/task-tracker-backend.git)
   cd task-tracker-backend
   ```

2. **Create the PostgreSQL database:**
   ```sql
   CREATE DATABASE task_tracker_db;
   ```

3. **Set your database password (choose either option):**
    - **Option A (Recommended for local dev):** Create `src/main/resources/application-local.properties` (ignored by Git):
      ```properties
      spring.datasource.password=your_postgres_password
      ```
    - **Option B (Environment variable):**
        - Windows PowerShell: `$env:DB_PASSWORD="your_postgres_password"`
        - macOS / Linux: `export DB_PASSWORD="your_postgres_password"`

4. **Run the application:**
    - Windows PowerShell:
      ```powershell
      .\mvnw.cmd spring-boot:run
      ```
    - macOS / Linux:
      ```bash
      ./mvnw spring-boot:run
      ```

5. The backend starts at **`http://localhost:8081`**.

---

## Configuration & Environment Variables

| Variable / Property | Default | Description |
| --- | --- | --- |
| `server.port` | `8081` | HTTP server port |
| `DB_PASSWORD` | `your_postgres_password` | PostgreSQL password for `postgres` user |
| `JWT_SECRET` | Local dev fallback secret | HMAC-SHA signing key for JWT (override in production) |
| `jwt.expiration-ms` | `86400000` (24 hours) | JWT validity duration in milliseconds |
| `file.upload-dir` | `uploads` | Local directory for storing uploaded task attachments (max 10 MB) |

---

## Authentication Flow

1. Call `POST /api/auth/register` or `POST /api/auth/login` to receive a signed JWT `token`.
2. Include the token in the `Authorization` header for all protected endpoints:
   ```http
   Authorization: Bearer <your_jwt_token>
   ```
3. Call `POST /api/auth/logout` with the same `Authorization` header to revoke the token immediately.

---

## API Reference

### Authentication & User Profile

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create a new user account and return JWT |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials and return JWT |
| `POST` | `/api/auth/logout` | Bearer | Blacklist current JWT token |
| `GET` | `/api/users/me` | Bearer | Get current authenticated user's profile |
| `PUT` | `/api/users/me` | Bearer | Update current user's name and bio |

### Task Management

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/tasks` | Bearer | Create a new task (defaults assignee to creator if omitted) |
| `GET` | `/api/tasks` | Bearer | Filter, sort, and search tasks (`status`, `teamId`, `assignedToId`, `q`, `sortBy`, `sortDir`) |
| `GET` | `/api/tasks/my` | Bearer | List tasks assigned to the logged-in user |
| `GET` | `/api/tasks/{id}` | Bearer | Retrieve a single task by ID |
| `PUT` | `/api/tasks/{id}` | Bearer | Update task details |
| `PATCH` | `/api/tasks/{id}/complete` | Bearer | Mark a task as `COMPLETED` |
| `PATCH` | `/api/tasks/{id}/assign` | Bearer | Assign a task to another user (validates team membership if task belongs to a team) |
| `DELETE` | `/api/tasks/{id}` | Bearer | Delete a task |

### Teams & Projects

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/teams` | Bearer | Create a new team/project (creator is added as a member) |
| `GET` | `/api/teams` | Bearer | List all teams the logged-in user belongs to |
| `GET` | `/api/teams/{id}` | Bearer | Get team details and member list |
| `POST` | `/api/teams/{id}/invite` | Bearer | Invite/add a registered user to the team by email |
| `POST` | `/api/teams/{id}/join` | Bearer | Join an existing team |
| `GET` | `/api/teams/{id}/tasks` | Bearer | List all tasks belonging to a team |

### Comments & File Attachments

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/tasks/{taskId}/comments` | Bearer | Add a comment to a task |
| `GET` | `/api/tasks/{taskId}/comments` | Bearer | List all comments on a task (oldest first) |
| `POST` | `/api/tasks/{taskId}/attachments` | Bearer | Upload a file attachment (`multipart/form-data`, field name: `file`) |
| `GET` | `/api/tasks/{taskId}/attachments` | Bearer | List all attachments for a task |
| `GET` | `/api/tasks/{taskId}/attachments/{attachmentId}/download` | Bearer | Download a specific attachment file |

---

## Sample API Requests

### 1. Register a User (`POST /api/auth/register`)
```json
{
  "name": "Abhishek",
  "email": "abhishek@example.com",
  "password": "password123",
  "bio": "Java Backend Developer"
}
```

### 2. Create a Task (`POST /api/tasks`)
```json
{
  "title": "Build REST APIs",
  "description": "Implement task CRUD, filter and search endpoints",
  "dueDate": "2026-12-01",
  "teamId": 1
}
```

### 3. Filter, Search & Sort Tasks (`GET`)
```text
GET http://localhost:8081/api/tasks?status=OPEN&q=REST&sortBy=dueDate&sortDir=asc
```

### 4. Create a Team (`POST /api/teams`)
```json
{
  "name": "Backend Squad",
  "description": "Core Java Spring Boot API team"
}
```

### 5. Add a Comment (`POST /api/tasks/1/comments`)
```json
{
  "content": "API endpoints tested in Postman and ready for review."
}
```

### 6. Upload an Attachment (`POST /api/tasks/1/attachments`)
- Body type: **`form-data`**
- Key: **`file`** (type: `File`) -> Select any local file (up to 10 MB)

---

## Error Handling

All API and validation errors return a consistent JSON structure:

**API Error Example (`404 Not Found` / `401 Unauthorized` / `409 Conflict`):**
```json
{
  "timestamp": "2026-11-24T12:30:00.123",
  "status": 404,
  "error": "Task not found"
}
```

**Validation Error Example (`400 Bad Request`):**
```json
{
  "timestamp": "2026-11-24T12:30:00.123",
  "status": 400,
  "errors": {
    "title": "Title is required"
  }
}
```