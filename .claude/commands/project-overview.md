## cat > .claude/commands/project-overview.md << 'EOF'

name: project-overview
description: Analyze project against best practices

---

Analyze all Java files in src/ following CLAUDE.md rules.

Scan for violations:

1. Lombok usage (@Data, @Getter, @Setter, @AllArgsConstructor)
2. Comments in code (// or /\* \*/)
3. Setters in domain entities (public void setX)
4. String used for IDs/Email (should be Value Objects)
5. Domain entities in API responses (should use DTOs)
6. Field injection (@Autowired on fields, not constructor)
7. Business logic in services (should be in entities)
8. Mutable entities (missing final fields)

Report format:

## Critical Issues

1. [Type] in [File]:[Line]
   Problem: [Why wrong]
   Fix: [How to fix]

## High Priority

[Same format]

## Statistics

- Lombok: X files
- Comments: X files
- Setters in domain: X
- Missing Value Objects: X
- Exposed entities: X
- Field injection: X

List top 10 critical issues.
Keep response under 50 lines.
EOF
