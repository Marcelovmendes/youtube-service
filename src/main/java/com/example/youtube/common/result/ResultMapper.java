package com.example.youtube.common.result;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public final class ResultMapper {

    private ResultMapper() {}

    public static <T> ResponseEntity<?> toResponse(
            Result<T, Error> result,
            Function<T, ?> successMapper
    ) {
        return result.fold(
                value -> ResponseEntity.ok(successMapper.apply(value)),
                error -> ResponseEntity.status(statusFor(error)).body(mapError(error))
        );
    }

    public static <T> ResponseEntity<?> toResponse(Result<T, Error> result) {
        return toResponse(result, Function.identity());
    }

    public static <T> ResponseEntity<?> ok(Result<T, Error> result) {
        return toResponse(result, Function.identity());
    }

    public static <T> ResponseEntity<?> created(Result<T, Error> result, Function<T, ?> successMapper) {
        return result.fold(
                value -> ResponseEntity.status(HttpStatus.CREATED).body(successMapper.apply(value)),
                error -> ResponseEntity.status(statusFor(error)).body(mapError(error))
        );
    }

    private static HttpStatus statusFor(Error error) {
        return switch (error) {
            case Error.AuthenticationError _, Error.InvalidStateError _, Error.TokenExchangeError _ -> HttpStatus.UNAUTHORIZED;
            case Error.ResourceNotFoundError _ -> HttpStatus.NOT_FOUND;
            case Error.InvalidInputError _ -> HttpStatus.BAD_REQUEST;
            case Error.ExternalServiceError _ -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    private static ErrorDTO mapError(Error error) {
        return switch (error) {
            case Error.AuthenticationError(var message, var details) ->
                new ErrorDTO("AUTHENTICATION_ERROR", message, details);
            case Error.InvalidStateError(var message) ->
                new ErrorDTO("INVALID_STATE", message, null);
            case Error.TokenExchangeError(var message, var reason) ->
                new ErrorDTO("TOKEN_EXCHANGE_FAILED", message, reason);
            case Error.ExternalServiceError(var service, var message, _) ->
                new ErrorDTO("EXTERNAL_SERVICE_ERROR", message, "Service: " + service);
            case Error.InvalidInputError(var field, var message) ->
                new ErrorDTO("INVALID_INPUT", message, "Field: " + field);
            case Error.ResourceNotFoundError(var resource, var identifier) ->
                new ErrorDTO("RESOURCE_NOT_FOUND", resource + " not found", "ID: " + identifier);
        };
    }

    public record ErrorDTO(String type, String message, String details) {}
}
