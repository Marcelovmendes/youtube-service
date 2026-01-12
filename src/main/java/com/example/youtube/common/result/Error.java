package com.example.youtube.common.result;

public sealed interface Error permits
    Error.AuthenticationError,
    Error.InvalidStateError,
    Error.TokenExchangeError,
    Error.ExternalServiceError,
    Error.InvalidInputError,
    Error.ResourceNotFoundError,
    Error.QuotaExceededError {

    record AuthenticationError(String message, String details) implements Error {}
    record InvalidStateError(String message) implements Error {}
    record TokenExchangeError(String message, String reason) implements Error {}
    record ExternalServiceError(String service, String message, Throwable cause) implements Error {}
    record InvalidInputError(String field, String message) implements Error {}
    record ResourceNotFoundError(String resource, String identifier) implements Error {}
    record QuotaExceededError(long currentUsage, long dailyLimit) implements Error {}

    static AuthenticationError authenticationError(String message, String details) {
        return new AuthenticationError(message, details);
    }

    static InvalidStateError invalidStateError(String message) {
        return new InvalidStateError(message);
    }

    static TokenExchangeError tokenExchangeError(String message, String reason) {
        return new TokenExchangeError(message, reason);
    }

    static ExternalServiceError externalServiceError(String service, String message, Throwable cause) {
        return new ExternalServiceError(service, message, cause);
    }

    static InvalidInputError invalidInputError(String field, String message) {
        return new InvalidInputError(field, message);
    }

    static ResourceNotFoundError resourceNotFoundError(String resource, String identifier) {
        return new ResourceNotFoundError(resource, identifier);
    }

    static QuotaExceededError quotaExceededError(long currentUsage, long dailyLimit) {
        return new QuotaExceededError(currentUsage, dailyLimit);
    }

    default String message() {
        return switch (this) {
            case AuthenticationError e -> e.message();
            case InvalidStateError e -> e.message();
            case TokenExchangeError e -> e.message();
            case ExternalServiceError e -> e.message();
            case InvalidInputError e -> e.message();
            case ResourceNotFoundError e -> "Resource not found: " + e.resource();
            case QuotaExceededError e -> "Quota exceeded: " + e.currentUsage() + "/" + e.dailyLimit();
        };
    }
}
