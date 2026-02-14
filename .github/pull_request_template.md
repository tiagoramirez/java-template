# Pull Request

## Description

<!-- Provide a clear and concise description of the changes in this PR -->
<!-- Explain the motivation and context behind these changes -->

## Type of Change

- [ ] New feature (non-breaking change which adds functionality)
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Code refactor (no functional changes)
- [ ] Dependency update
- [ ] Performance improvement

## Architecture Compliance

- [ ] Changes follow hexagonal architecture pattern (ports & adapters)
- [ ] New features organized in feature packages (`com.tiagoramirez.template.<feature>/`)
- [ ] Controllers depend on input ports, not services directly
- [ ] Services implement input ports and depend on output ports
- [ ] Adapters implement output ports
- [ ] Domain layer remains framework-agnostic

## Testing Checklist

- [ ] Unit tests added for new services and adapters
- [ ] Integration tests added for new REST endpoints
- [ ] All existing tests pass locally
- [ ] Test coverage remains at 100%
- [ ] DTOs end with `Dto.java` suffix (for coverage exclusion)
- [ ] Exceptions placed in `exceptions/` package (for coverage exclusion)
- [ ] Constants placed in `constants/` package (for coverage exclusion)

## Code Quality

- [ ] Code follows project code style guidelines
- [ ] Constructor injection used (not field injection)
- [ ] JSON follows snake_case convention
- [ ] Lombok used appropriately for boilerplate reduction
- [ ] No new compiler warnings introduced

## Documentation

- [ ] README updated (if needed)
- [ ] ARCHITECTURE.md updated (if architectural changes)
- [ ] API documentation updated (Swagger/OpenAPI)
- [ ] Inline code comments added where logic is complex
- [ ] MONITORING.md updated (if new custom metrics added)

## Monitoring

- [ ] Custom metrics added (if applicable) with appropriate tags
- [ ] Metrics follow naming convention
- [ ] Grafana dashboard updated (if new metrics)

## Additional Notes

<!-- Add any additional information that reviewers should know -->
<!-- Include screenshots for UI changes -->
<!-- Mention any breaking changes or migration steps -->

## Reviewer Checklist

**For Reviewers:**
- [ ] Code follows hexagonal architecture principles
- [ ] Test coverage is adequate and at 100%
- [ ] No security vulnerabilities introduced
- [ ] Performance impact considered
- [ ] Documentation is clear and complete
- [ ] Changes align with project goals
