# YouTube Service

Backend microservice for YouTube integration, part of the **PlaySwap** playlist converter ecosystem. Handles OAuth authentication, playlist management, and video search through YouTube Data API v3.

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 25 |
| Framework | Spring Boot 3.4.5 |
| Session | Spring Session + Redis |
| External APIs | YouTube Data API v3, Google OAuth 2.0 |
| Build | Maven |
| Testing | JUnit 5, Mockito, JaCoCo |

## Architecture

The project follows **Domain-Driven Design** with **Clean Architecture** principles:

```
src/main/java/com/example/youtube/
├── auth/                    # Authentication bounded context
├── playlist/                # Playlist management bounded context
├── search/                  # Video search bounded context
├── quota/                   # API quota tracking
└── common/                  # Shared infrastructure
    ├── config/              # Spring configurations
    └── result/              # Functional Result type
```

Each bounded context is organized in layers:

```
context/
├── api/                     # REST controllers, DTOs
├── application/             # Use cases, business orchestration
├── domain/                  # Entities, value objects, ports
└── infrastructure/          # External adapters (YouTube API, Redis)
```

## Design Patterns & Practices

### Functional Error Handling

Custom `Result<T, E>` sealed interface implementing the Either monad pattern:

```java
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    <U> Result<U, E> map(Function<T, U> mapper);
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure);
}
```

### Immutable Domain Entities

All domain objects are immutable with factory methods for validation:

```java
public final class Token {
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;

    public static Result<Token, Error> create(...) { }
}
```

### Value Objects

Type-safe identifiers using Java records:

```java
public record VideoId(String youtubeId, UUID internalId) { }
public record PlaylistId(String youtubeId, UUID internalId) { }
```

### Port & Adapter Pattern

Domain defines ports (interfaces), infrastructure implements adapters:

```java
// Domain port
public interface YouTubePlaylistPort {
    Result<List<YouTubePlaylist>, Error> getUserPlaylists(Token token);
}

// Infrastructure adapter
public class YouTubePlaylistAdapter implements YouTubePlaylistPort { }
```

## Key Features

### OAuth 2.0 with PKCE

Secure authorization code flow with Proof Key for Code Exchange:

- State validation prevents CSRF
- Code verifier/challenge prevents authorization code interception
- Idempotent callback handling for browser extension compatibility

### Playlist Operations

- List user's YouTube playlists
- Create new playlists
- Add videos to playlists
- Paginated video retrieval

### Smart Music Search

- Full-text video search
- Music-specific search (track + artist)
- Automatic filtering of covers, live performances, karaoke versions

### Quota Management

- Daily API quota tracking (YouTube API limits: 10,000 units/day)
- Per-operation cost calculation
- Redis-based persistence with timezone-aware expiration

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/auth` | Initiate OAuth flow |
| GET | `/v1/auth/google/callback` | OAuth callback |
| POST | `/v1/auth/refresh` | Refresh access token |
| POST | `/v1/auth/logout` | Logout |
| GET | `/v1/auth/session` | Session status |

### Playlists

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/playlists` | List user playlists |
| POST | `/v1/playlists` | Create playlist |
| GET | `/v1/playlists/{id}/videos` | Get playlist videos |
| POST | `/v1/playlists/{id}/videos` | Add videos to playlist |

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/search` | Search videos |
| GET | `/v1/search/music` | Search specific music |

## Code Quality Standards

- **No Lombok**: Explicit constructors and accessors
- **No comments**: Self-documenting code
- **No setters**: Immutable domain entities
- **No exposed entities**: DTOs at API boundaries
- **Constructor injection**: No field `@Autowired`
- **Type safety**: Records, sealed interfaces, pattern matching

## Running the Project

### Prerequisites

- Java 25
- Maven 3.9+
- Redis
- Google Cloud project with YouTube Data API enabled

### Configuration

Create `src/main/resources/application.yml`:

```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID}
  client-secret: ${GOOGLE_CLIENT_SECRET}
  redirect-uri: http://localhost:8080/v1/auth/google/callback

spring:
  data:
    redis:
      host: localhost
      port: 6379
  session:
    timeout: 30m
```

### Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

### Tests

```bash
mvn test
```

## Project Context

This service is part of a larger system for converting playlists between music streaming platforms. It works alongside:

- **Frontend**: React application for user interaction
- **Spotify Service**: Handles Spotify API integration
- **Orchestration Layer**: Coordinates playlist conversion between platforms
