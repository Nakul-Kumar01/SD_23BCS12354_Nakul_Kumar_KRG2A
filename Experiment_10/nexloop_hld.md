# NexLoop — High Level Design (HLD)
> MERN-Based Coding Platform with AI Features, Judge0 Code Execution, and Contest Management

### LIVE LINK:
https://nexloops.xyz/

---

## 1. Functional Requirements (FRs)

### User Features
- Register, login, logout (JWT-based auth)
- Browse and solve coding problems (filter by tag, difficulty)
- Submit code → get verdict (AC, WA, TLE, RE, CE)
- View submission history per problem
- Watch editorial/solution videos
- Participate in contests organised by Admin
- View contest leaderboard and personal score
- Use AI Ask → get hints/intuition (NOT direct solution, layered approach)
- Use AI Interview simulator → practice mock interviews

### Admin Features
- Create / edit / delete problems
- Upload editorial videos
- Create and manage contests
- View all submissions and detect plagiarism

### Contest Features
- Admin creates contest with a set of problems and a time window
- Users register for contest
- Live leaderboard during contest
- Score calculation based on correctness + time

---

## 2. Non-Functional Requirements (NFRs)

| Category | Requirement |
|---|---|
| **Scalability** | Handle thousands of concurrent code submissions without degrading performance |
| **Availability** | 99.9% uptime for problem browsing and submission |
| **Low Latency** | Code verdict returned within 5 seconds for typical cases |
| **Security** | JWT blacklisting on logout; rate limiting to prevent spam submissions; sandboxed code execution via Judge0 |
| **Performance** | Redis caching for frequently accessed problems and recent results; pagination for large datasets; Cloudinary CDN for video delivery (no DB hit) |
| **Fault Tolerance** | Code execution isolated in Judge0 — malicious code cannot affect main servers |
| **Consistency** | Eventual consistency acceptable for leaderboard; strong consistency required for submission verdicts |
| **Maintainability** | Modular service-based structure (Auth, Problem, Submission, Contest, AI, Media) |

---

## 3. API Design

### Auth Service (`/api/auth`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/register` | Register new user | ❌ |
| POST | `/login` | Login → returns JWT | ❌ |
| POST | `/logout` | Blacklist token in Redis | ✅ |
| GET | `/me` | Get current user profile | ✅ |

### Problem Service (`/api/problems`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/` | List all problems (paginated, filterable) | ✅ |
| GET | `/:id` | Get single problem detail | ✅ |
| POST | `/` | Create problem (Admin only) | ✅ Admin |
| PUT | `/:id` | Update problem (Admin only) | ✅ Admin |
| DELETE | `/:id` | Delete problem (Admin only) | ✅ Admin |

### Submission Service (`/api/submissions`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/` | Submit code `{ problemId, code, language }` | ✅ |
| GET | `/:id` | Get submission result | ✅ |
| GET | `/user/me` | All submissions by current user | ✅ |
| GET | `/problem/:problemId` | Submissions for a problem (Admin) | ✅ Admin |

### Contest Service (`/api/contests`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/` | Create contest (Admin) | ✅ Admin |
| GET | `/` | List all contests | ✅ |
| GET | `/:id` | Get contest details + problems | ✅ |
| POST | `/:id/register` | Register user for contest | ✅ |
| GET | `/:id/leaderboard` | Live leaderboard | ✅ |
| GET | `/:id/submissions` | All submissions in contest | ✅ Admin |

### AI Service (`/api/ai`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/ask` | `{ problemId, userQuery }` → hint/intuition | ✅ |
| POST | `/interview/start` | Start AI mock interview session | ✅ |
| POST | `/interview/respond` | Send answer → get next question/feedback | ✅ |

### Media Service (`/api/media`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/upload` | Upload video to Cloudinary (Admin) | ✅ Admin |
| GET | `/video/:problemId` | Get signed video URL | ✅ |

---

## 4. Core Entities

### User
Represents a registered user on the platform.
- `_id`, `username`, `email`, `passwordHash`, `role` (user/admin), `createdAt`

### Problem
A coding problem with test cases.
- `_id`, `title`, `description`, `difficulty` (easy/medium/hard), `tags[]`, `testCases[]`, `constraints`, `createdBy`, `videoEditorialId`

### Submission
A code submission by a user for a problem.
- `_id`, `userId`, `problemId`, `code`, `language`, `verdict` (AC/WA/TLE/RE/CE), `executionTime`, `memoryUsed`, `contestId` (null if practice), `submittedAt`

### Contest
An admin-organised timed coding contest.
- `_id`, `title`, `description`, `startTime`, `endTime`, `problems[]`, `createdBy`

### ContestUser
Join table linking users to contests.
- `_id`, `contestId`, `userId`, `score`, `registeredAt`

### Editorial / Media
Stores video reference for a problem.
- `_id`, `problemId`, `cloudinaryUrl`, `publicId`, `uploadedBy`, `createdAt`

---

## 5. Database Schema Design

> **Database:** MongoDB
> **Optimisation:** Compound indexing on `(userId, problemId)` in Submissions for fast query performance.

### Collection: `users`
```json
{
  "_id": "ObjectId",
  "username": "string (unique)",
  "email": "string (unique)",
  "passwordHash": "string",
  "role": "enum['user', 'admin']",
  "createdAt": "Date"
}
```

### Collection: `problems`
```json
{
  "_id": "ObjectId",
  "title": "string",
  "description": "string",
  "difficulty": "enum['easy', 'medium', 'hard']",
  "tags": ["array of strings"],
  "testCases": [
    { "input": "string", "expectedOutput": "string", "isHidden": "boolean" }
  ],
  "constraints": "string",
  "createdBy": "ObjectId (ref: users)",
  "videoEditorialId": "ObjectId (ref: editorials) | null",
  "createdAt": "Date"
}
```

### Collection: `submissions`
```json
{
  "_id": "ObjectId",
  "userId": "ObjectId (ref: users)",
  "problemId": "ObjectId (ref: problems)",
  "code": "string",
  "language": "string (cpp, python, java, js...)",
  "verdict": "enum['AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'Pending']",
  "executionTimeMs": "number",
  "memoryKB": "number",
  "contestId": "ObjectId | null",
  "submittedAt": "Date"
}
// Index: { userId: 1, problemId: 1 } — compound index for fast lookup
```

### Collection: `contests`
```json
{
  "_id": "ObjectId",
  "title": "string",
  "description": "string",
  "startTime": "Date",
  "endTime": "Date",
  "problems": ["ObjectId (ref: problems)"],
  "createdBy": "ObjectId (ref: users)"
}
```

### Collection: `contestusers`
```json
{
  "_id": "ObjectId",
  "contestId": "ObjectId (ref: contests)",
  "userId": "ObjectId (ref: users)",
  "score": "number",
  "registeredAt": "Date"
}
// Index: { contestId: 1, score: -1 } for leaderboard queries
```

### Collection: `editorials`
```json
{
  "_id": "ObjectId",
  "problemId": "ObjectId (ref: problems)",
  "cloudinaryUrl": "string",
  "cloudinaryPublicId": "string",
  "uploadedBy": "ObjectId (ref: users)",
  "createdAt": "Date"
}
```

---

## 6. High Level Design (HLD)

> See attached `nexloop_hld.drawio` for the full architecture diagram.

### Architecture Overview

```
[Client: React SPA / Mobile]
         |
         ▼
[API Gateway / Load Balancer]
  - JWT Auth Middleware
  - Rate Limiting
  - Request Routing
         |
  ┌──────┴──────────────────────────────────┐
  ▼      ▼         ▼         ▼       ▼      ▼
Auth  Problem  Submission Contest   AI   Media
 SVC    SVC      SVC       SVC     SVC    SVC
  |      |         |         |             |
  └──────┴────┬────┴────┬────┘             |
              ▼         ▼                  ▼
           MongoDB    Redis          Cloudinary CDN
           (primary  (cache +       (video storage +
           DB)        JWT blacklist)  delivery)
                       |
              Submission SVC also calls:
                       ▼
                   Judge0 API
              (sandboxed execution)
                       |
              Contest SVC calls:
                       ▼
             Plagiarism Detection Module
             (hash + KMP string matching)
```

### Key Design Decisions

#### JWT Blacklisting with Redis
Standard JWT is stateless — once issued, you can't invalidate it before expiry. NexLoop solves this by storing a blacklist of logged-out tokens in Redis with a TTL matching the token expiry. On every request, middleware checks Redis before proceeding. Redis is ideal here because:
- In-memory → microsecond lookups
- Native TTL support → tokens auto-expire, no manual cleanup needed

#### Judge0 for Code Execution
Instead of running user code directly on the server (security nightmare), all submissions go to Judge0:
- Supports 50+ languages
- Handles async execution
- Fully sandboxed — malicious code cannot escape the container
- Returns verdict, execution time, memory usage

#### Redis Caching Strategy
- Problem list: cached with TTL (invalidated on create/update)
- Submission results: cached per submissionId
- Rate limiting counters per userId stored in Redis

#### Cloudinary for Video
Videos cannot be stored in MongoDB (BSON limit). Cloudinary provides:
- Scalable video storage
- CDN delivery → video requests don't hit our servers
- Signed URLs → only authenticated users can access content

#### Plagiarism Detection (Contest Submissions)
Challenge: compare N² submission pairs efficiently.
Solution:
1. Normalize code (remove comments, whitespace, rename variables)
2. Generate hash fingerprints (rolling hash / Rabin-Karp)
3. Use KMP string matching for O(n) comparison instead of O(n²) brute force
4. Flag submissions above a similarity threshold

#### Performance Optimizations
- **Pagination** on all list endpoints → handles large datasets
- **Compound indexing** on `(userId, problemId)` in submissions → fast user submission history
- **Compound indexing** on `(contestId, score DESC)` in contestusers → fast leaderboard
- **Cloudinary CDN** → video/image requests bypass application servers entirely

---

*NexLoop HLD — Prepared for system design walkthrough*
