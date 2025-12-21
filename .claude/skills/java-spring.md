---
name: java-spring
description: Java and Spring Boot standards for clean, maintainable code
---

# Java & Spring Boot Standards

## Core Principles

- **Rich Domain Model**: Entities contain business logic, not just data
- **Immutability**: Prefer final fields and immutable objects
- **Value Objects**: Encapsulate primitives with domain meaning (Email, ID, etc.)
- **Type-safety**: Leverage Java's strong typing
- **No Lombok**: Explicit code over magic annotations
- **No Comments**: Code must be self-explanatory through naming
- **Interfaces as Contracts**: Use interfaces to decouple layers
- **Constructor Injection**: Never use field injection

## Architecture Patterns (Flexible)

This skill supports multiple architectures:

- Layered Architecture (Controller → Service → Repository)
- Hexagonal Architecture (Ports & Adapters)
- Clean Architecture
- Hybrid approaches

**Key Rule**: Whatever architecture you choose, keep business logic in domain entities.

## Package Structure Examples

### Option 1: Traditional Layered (Simpler)

```
com.example.playswap/
├── config/              # Spring configuration
├── controller/          # REST endpoints
├── service/             # Business orchestration
├── repository/          # Data access interfaces
├── domain/              # Rich domain entities + value objects
├── dto/                 # API request/response objects
├── exception/           # Custom exceptions
└── infrastructure/      # External integrations (Spotify, Redis)
```

### Option 2: Hexagonal (More Decoupled)

```
com.example.playswap/
├── domain/              # Pure business logic (no Spring)
│   ├── entity/
│   ├── valueobject/
│   ├── repository/      # Interfaces (ports)
│   └── exception/
├── application/         # Use cases
│   └── port/            # External service interfaces
├── infrastructure/      # Implementations (adapters)
│   ├── adapter/
│   ├── web/
│   └── config/
```

### Option 3: Feature-Based (Domain-Driven)

```
com.example.playswap/
├── playlist/
│   ├── domain/          # Playlist entity, value objects
│   ├── api/             # PlaylistController
│   ├── application/     # PlaylistService
│   └── infrastructure/  # PlaylistRepository implementation
├── user/
│   ├── domain/
│   ├── api/
│   ├── application/
│   └── infrastructure/
└── shared/              # Shared utilities, exceptions
```

**Choose what fits your project**. All are valid.

## Rich Domain Entities (Critical - Works Everywhere)

### ✅ GOOD: Entity with Business Logic

```java
public final class Playlist {
    private final PlaylistId id;
    private final String name;
    private final UserId ownerId;
    private final List<Track> tracks;
    private final boolean collaborative;

    private Playlist(PlaylistId id, String name, UserId ownerId,
                     List<Track> tracks, boolean collaborative) {
        this.id = Objects.requireNonNull(id, "Playlist ID cannot be null");
        this.name = validateName(name);
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        this.tracks = List.copyOf(tracks);
        this.collaborative = collaborative;
    }

    // Factory method for creation
    public static Playlist create(PlaylistId id, String name, UserId ownerId) {
        return new Playlist(id, name, ownerId, List.of(), false);
    }

    // Business logic: Add track
    public Playlist addTrack(Track track) {
        Objects.requireNonNull(track, "Track cannot be null");

        if (tracks.size() >= 10000) {
            throw new PlaylistFullException("Maximum 10000 tracks allowed");
        }

        if (tracks.contains(track)) {
            return this; // Already exists, no change
        }

        List<Track> newTracks = new ArrayList<>(tracks);
        newTracks.add(track);
        return new Playlist(id, name, ownerId, newTracks, collaborative);
    }

    // Business logic: Make collaborative
    public Playlist makeCollaborative() {
        if (collaborative) {
            throw new IllegalStateException("Already collaborative");
        }
        return new Playlist(id, name, ownerId, tracks, true);
    }

    // Business rule: Name validation
    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Playlist name too long (max 100)");
        }
        return name.trim();
    }

    // Only getters, no setters
    public PlaylistId getId() { return id; }
    public String getName() { return name; }
    public UserId getOwnerId() { return ownerId; }
    public List<Track> getTracks() { return List.copyOf(tracks); }
    public boolean isCollaborative() { return collaborative; }
    public int getTrackCount() { return tracks.size(); }
    public boolean isEmpty() { return tracks.isEmpty(); }
}
```

### ❌ BAD: Anemic Entity (Just Data)

```java
// This is NOT a domain entity, it's just a data holder
public class Playlist {
    private String id;
    private String name;
    private List<Track> tracks = new ArrayList<>();

    // Just getters/setters - NO business logic
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public void addTrack(Track track) { tracks.add(track); } // No validation!
}
```

## Value Objects (Critical - Works Everywhere)

### ✅ GOOD: Value Object with Validation

```java
public record Email(String value) {
    private static final Pattern PATTERN =
        Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}

public record PlaylistId(String value, UUID internalId) {
    public static PlaylistId fromSpotifyId(String spotifyId) {
        Objects.requireNonNull(spotifyId, "Spotify ID cannot be null");
        UUID internalId = UUID.nameUUIDFromBytes(("spotify:" + spotifyId).getBytes());
        return new PlaylistId(spotifyId, internalId);
    }

    public static PlaylistId generate() {
        UUID id = UUID.randomUUID();
        return new PlaylistId(id.toString(), id);
    }
}
```

### ❌ BAD: Primitive Obsession

```java
public class User {
    private String email; // Just a string - no validation, no type safety
    private String id;    // Could be any string
}
```

## Immutability (Critical - Works Everywhere)

### ✅ GOOD: Immutable with Factory Methods

```java
public final class User {
    private final UserId id;
    private final Email email;
    private final String name;

    private User(UserId id, Email email, String name) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.name = Objects.requireNonNull(name);
    }

    public static User create(UserId id, Email email, String name) {
        return new User(id, email, name);
    }

    // Return new instance instead of mutating
    public User withName(String newName) {
        return new User(id, email, newName);
    }

    // Only getters
    public UserId getId() { return id; }
    public Email getEmail() { return email; }
    public String getName() { return name; }
}
```

### ❌ BAD: Mutable with Setters

```java
public class User {
    private String id;
    private String email;
    private String name;

    public void setName(String name) {
        this.name = name; // Mutates state - dangerous
    }
}
```

## Service Layer (Orchestration, Not Business Logic)

### ✅ GOOD: Service Orchestrates, Entity Decides

```java
@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final SpotifyClient spotifyClient;
    private final CacheService cacheService;

    public PlaylistService(PlaylistRepository playlistRepository,
                          SpotifyClient spotifyClient,
                          CacheService cacheService) {
        this.playlistRepository = playlistRepository;
        this.spotifyClient = spotifyClient;
        this.cacheService = cacheService;
    }

    public Playlist addTrackToPlaylist(PlaylistId playlistId, TrackId trackId) {
        // 1. Fetch data
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

        Track track = spotifyClient.getTrack(trackId);

        // 2. Entity makes business decision
        Playlist updated = playlist.addTrack(track);

        // 3. Persist and cache
        Playlist saved = playlistRepository.save(updated);
        cacheService.invalidate("playlist:" + playlistId.value());

        return saved;
    }
}
```

### ❌ BAD: Business Logic in Service

```java
@Service
public class PlaylistService {
    public void addTrackToPlaylist(String playlistId, String trackId) {
        Playlist playlist = repository.findById(playlistId);
        Track track = spotifyClient.getTrack(trackId);

        // Business logic in service - WRONG!
        if (playlist.getTracks().size() >= 10000) {
            throw new Exception("Too many tracks");
        }

        if (playlist.getTracks().contains(track)) {
            return;
        }

        playlist.getTracks().add(track); // Mutating state - WRONG!
        repository.save(playlist);
    }
}
```

## Interfaces as Contracts (Flexible)

### ✅ GOOD: Repository Interface

```java
// Can be in domain/ or repository/ package
public interface PlaylistRepository {
    Optional<Playlist> findById(PlaylistId id);
    Playlist save(Playlist playlist);
    List<Playlist> findByOwnerId(UserId ownerId);
    void delete(PlaylistId id);
    boolean exists(PlaylistId id);
}

// Implementation in infrastructure/
@Repository
public class RedisCachePlaylistRepository implements PlaylistRepository {
    private final RedisCacheService cache;

    public RedisCachePlaylistRepository(RedisCacheService cache) {
        this.cache = cache;
    }

    @Override
    public Optional<Playlist> findById(PlaylistId id) {
        return cache.get("playlist:" + id.value(), Playlist.class);
    }

    @Override
    public Playlist save(Playlist playlist) {
        cache.put("playlist:" + playlist.getId().value(), playlist);
        return playlist;
    }
}
```

### ✅ GOOD: External Service Interface

```java
// Interface (can be in application/ or service/)
public interface SpotifyClient {
    List<Playlist> getUserPlaylists(String accessToken);
    Track getTrack(TrackId trackId, String accessToken);
}

// Implementation in infrastructure/
@Component
public class SpotifyApiClient implements SpotifyClient {
    private final SpotifyApi spotifyApi;

    public SpotifyApiClient(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    @Override
    public List<Playlist> getUserPlaylists(String accessToken) {
        // Implementation details
    }
}
```

## Never Use Lombok

### ✅ GOOD: Explicit Code

```java
public final class UserProfile {
    private final UserId id;
    private final Email email;
    private final String displayName;

    private UserProfile(UserId id, Email email, String displayName) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.displayName = Objects.requireNonNull(displayName);
    }

    public static UserProfile create(UserId id, Email email, String displayName) {
        return new UserProfile(id, email, displayName);
    }

    public UserId getId() { return id; }
    public Email getEmail() { return email; }
    public String getDisplayName() { return displayName; }
}
```

### ❌ BAD: Lombok Magic

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private String id;
    private String email;
    private String displayName;
}
```

## Never Add Comments

### ✅ GOOD: Self-Explanatory Code

```java
public Playlist addTrack(Track track) {
    validateTrackNotNull(track);
    validatePlaylistNotFull();
    validateTrackNotDuplicated(track);

    return createNewPlaylistWithTrack(track);
}

private void validateTrackNotNull(Track track) {
    Objects.requireNonNull(track, "Track cannot be null");
}

private void validatePlaylistNotFull() {
    if (tracks.size() >= MAX_TRACKS) {
        throw new PlaylistFullException();
    }
}

private void validateTrackNotDuplicated(Track track) {
    if (tracks.contains(track)) {
        throw new DuplicateTrackException(track.getId());
    }
}

private Playlist createNewPlaylistWithTrack(Track track) {
    List<Track> newTracks = new ArrayList<>(tracks);
    newTracks.add(track);
    return new Playlist(id, name, ownerId, newTracks, collaborative);
}
```

### ❌ BAD: Comments Explaining Bad Code

```java
public Playlist addTrack(Track track) {
    // Check if track is null
    if (track == null) throw new Exception();

    // Check if playlist has space
    if (tracks.size() >= 10000) throw new Exception();

    // Check if track already exists
    for (Track t : tracks) {
        if (t.getId().equals(track.getId())) return this;
    }

    // Add track to list
    List<Track> newTracks = new ArrayList<>(tracks);
    newTracks.add(track);
    return new Playlist(id, name, ownerId, newTracks, collaborative);
}
```

## DTOs at API Boundaries

### ✅ GOOD: Separate DTOs from Domain

```java
// Domain entity (internal)
public final class Playlist {
    private final PlaylistId id;
    private final String name;
    private final UserId ownerId;
    private final List<Track> tracks;
    // ... business logic
}

// API Response (external)
public record PlaylistResponse(
    String id,
    String name,
    String ownerName,
    int trackCount,
    boolean collaborative
) {
    public static PlaylistResponse fromDomain(Playlist playlist, String ownerName) {
        return new PlaylistResponse(
            playlist.getId().value(),
            playlist.getName(),
            ownerName,
            playlist.getTrackCount(),
            playlist.isCollaborative()
        );
    }
}

// Controller
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable String id) {
        Playlist playlist = service.findById(PlaylistId.of(id));
        PlaylistResponse response = PlaylistResponse.fromDomain(playlist, "Owner Name");
        return ResponseEntity.ok(response);
    }
}
```

### ❌ BAD: Exposing Domain Entity

```java
@GetMapping("/{id}")
public ResponseEntity<Playlist> getPlaylist(@PathVariable String id) {
    return ResponseEntity.ok(service.findById(id)); // Exposes internal structure!
}
```

## Testing

### ✅ GOOD: Test Domain Behavior

```java
class PlaylistTest {

    @Test
    void addsTrackToEmptyPlaylist() {
        Playlist playlist = Playlist.create(
            PlaylistId.generate(),
            "My Playlist",
            UserId.generate()
        );
        Track track = createTestTrack();

        Playlist updated = playlist.addTrack(track);

        assertThat(updated.getTrackCount()).isEqualTo(1);
        assertThat(updated.getTracks()).contains(track);
    }

    @Test
    void throwsExceptionWhenPlaylistFull() {
        Playlist playlist = createPlaylistWithMaxTracks();
        Track track = createTestTrack();

        assertThatThrownBy(() -> playlist.addTrack(track))
            .isInstanceOf(PlaylistFullException.class)
            .hasMessage("Maximum 10000 tracks allowed");
    }

    @Test
    void rejectsNullTrack() {
        Playlist playlist = createTestPlaylist();

        assertThatThrownBy(() -> playlist.addTrack(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Track cannot be null");
    }
}
```

## Naming Conventions

### Classes

- **Entities**: Noun (Playlist, Track, User)
- **Value Objects**: Noun (Email, PlaylistId, TrackId)
- **Services**: Noun + Service (PlaylistService, AuthService)
- **Repositories**: Noun + Repository (PlaylistRepository)
- **DTOs**: Noun + Request/Response (PlaylistResponse, ConversionRequest)
- **Exceptions**: Noun + Exception (PlaylistNotFoundException)

### Methods

- **Domain**: Business language (addTrack, makeCollaborative, convertToYouTube)
- **Repositories**: find, save, delete, exists
- **Services**: Verbs describing use cases (syncPlaylists, convertPlaylist)
- **Boolean**: is, has, can (isCollaborative, hasExpired, canAddTrack)

### Variables

- **Descriptive names**: userRepository (not repo), playlistService (not service)
- **camelCase** for variables and methods
- **UPPER_SNAKE_CASE** for constants

## Design Patterns

### Factory Pattern (Creation)

```java
public static Playlist create(PlaylistId id, String name, UserId ownerId) {
    return new Playlist(id, name, ownerId, List.of(), false);
}
```

### Repository Pattern

```java
public interface PlaylistRepository {
    Optional<Playlist> findById(PlaylistId id);
    Playlist save(Playlist playlist);
}
```

### Builder Pattern (for complex objects with many optionals)

```java
// Use ONLY when object has 5+ optional parameters
public class SearchCriteria {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private Integer limit = 20;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(this);
        }
    }
}
```

## Avoid

### Anti-Patterns

- ❌ Anemic Domain Model (entities with only getters/setters)
- ❌ God Classes (classes doing too much)
- ❌ Primitive Obsession (using String for email, ID)
- ❌ Mutable State in domain entities

### Code Smells

- ❌ Long Parameter List (>3 parameters - use object)
- ❌ Long Method (>20 lines - extract methods)
- ❌ Large Class (>200 lines - split responsibilities)
- ❌ Comments (code should be self-explanatory)
- ❌ Duplicate Code
- ❌ Magic Numbers/Strings (extract to constants)

### Bad Practices

- ❌ Using Lombok
- ❌ Exposing domain entities at API boundaries
- ❌ Setters in domain entities
- ❌ Business logic in controllers or repositories
- ❌ Field injection (@Autowired on fields)
- ❌ Catching generic Exception
- ❌ Empty catch blocks
- ❌ System.out.println (use logger)
- ❌ Hardcoded values (use configuration)

## Summary

1. **Rich Domain**: Business logic in entities, not services
2. **Immutability**: final fields, no setters, return new instances
3. **Value Objects**: Encapsulate primitives with domain meaning
4. **Interfaces**: Define contracts between layers
5. **No Lombok**: Explicit code over magic
6. **No Comments**: Self-explanatory code through naming
7. **No Setters**: Use factory methods (withX, create)
8. **DTOs at Boundaries**: Hide internal domain structure
9. **Test Behavior**: Test what code does, not how it does it
