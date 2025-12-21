## Git Workflow

- Do not include "Claude Code" in commit messages
- Use conventional commits: `feat: add playlist conversion`, `fix: handle null track`

## Critical Rules

### NEVER

- Use Lombok annotations
- Add comments to code (code must be self-explanatory)
- Use setters in domain entities
- Expose domain entities at API boundaries
- Put business logic outside entities (belongs in domain)

### ALWAYS

- Make domain entities immutable (final fields, factory methods)
- Use Value Objects for IDs, Email, etc. (not primitive String/Long)
- Use interfaces to decouple layers
- Use DTOs at API boundaries (requests/responses)
- Constructor injection (never @Autowired on fields)
- Test domain behavior, not implementation details

## Important Concepts

- **Rich Domain Model**: Entities contain business logic
- **Immutability**: All domain objects are immutable
- **Value Objects**: Encapsulate primitives (Email, PlaylistId)
- **Type-safety**: Leverage Java's strong typing
- **Self-documenting code**: Names explain intent, no comments needed
- **Interfaces as contracts**: Decouple layers with interfaces

All detailed coding guidelines are in the skill:

- Use `java-spring` skill for Java/Spring Boot standards
- Use `reviewing-code` skill for code reviews
- Use `writing` skill for documentation and commit messages

## Project Context

- **Stack**: Java 21, Spring Boot 3.x, Maven
- **Architecture**: Layered with rich domain model
- **Database**: Redis (cache only, stateless)
- **External APIs**: Spotify API, YouTube Data API
- **Patterns**: Repository, Factory, Value Objects
