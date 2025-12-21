---
name: create-feature
description: Creates branch, plans, and implements a Java/Spring Boot feature
---

# Create Feature Command

Creates a new Git branch and implements a feature following Java/Spring Boot best practices.

## Usage

```
/create-feature Add playlist conversion endpoint
```

## Workflow

1. **Create branch**

   - Pattern: `feat/add-playlist-conversion-endpoint`
   - Based on current branch (usually `main` or `develop`)

2. **Analyze codebase**

   - Review existing controllers, services, and repositories
   - Identify patterns and conventions
   - Check for similar implementations

3. **Plan implementation**

   - Define required classes (Controller, Service, DTO, etc.)
   - Identify dependencies
   - Plan test coverage
   - Consider error scenarios

4. **Implement with best practices**

   - Follow package structure
   - Use constructor injection
   - Create DTOs for requests/responses
   - Add proper logging
   - Handle exceptions
   - Write unit tests

5. **Stage changes**
   - `git add` all new/modified files
   - Ready for `/review-staged`

## Example Output

```java
// Controller
@RestController
@RequestMapping("/api/v1/conversions")
public class ConversionController {
    private final ConversionService service;

    public ConversionController(ConversionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConversionResponse> convert(
        @Valid @RequestBody ConversionRequest request
    ) {
        ConversionResponse response = service.convertPlaylist(request);
        return ResponseEntity.ok(response);
    }
}

// Service
@Service
public class ConversionService {
    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    private final SpotifyPort spotifyPort;
    private final YouTubePort youtubePort;
    private final RedisCacheService cache;

    // Constructor injection
    public ConversionService(
        SpotifyPort spotifyPort,
        YouTubePort youtubePort,
        RedisCacheService cache
    ) {
        this.spotifyPort = spotifyPort;
        this.youtubePort = youtubePort;
        this.cache = cache;
    }

    public ConversionResponse convertPlaylist(ConversionRequest request) {
        log.info("Converting playlist: spotifyId={}", request.spotifyId());

        // Implementation...
    }
}

// Tests
@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {
    @Mock
    private SpotifyPort spotifyPort;

    @Mock
    private YouTubePort youtubePort;

    @InjectMocks
    private ConversionService service;

    @Test
    void convertsPlaylistSuccessfully() {
        // Test implementation
    }
}
```

## Quality Checks

Before staging, verify:

- [ ] Constructor injection used
- [ ] Proper package placement
- [ ] DTOs created for requests/responses
- [ ] Logging added
- [ ] Exceptions handled
- [ ] Unit tests written
- [ ] No `@Autowired` field injection
- [ ] No hardcoded values
