package com.example.youtube.auth.api.dto;

import com.example.youtube.common.result.Error;

public record ErrorResponse(String type, String message, String details) {

    public static ErrorResponse fromError(Error error) {
        return switch (error) {
            case Error.AuthenticationError(var message, var details) ->
                new ErrorResponse("AUTHENTICATION_ERROR", message, details);
            case Error.InvalidStateError(var message) ->
                new ErrorResponse("INVALID_STATE", message, null);
            case Error.TokenExchangeError(var message, var reason) ->
                new ErrorResponse("TOKEN_EXCHANGE_FAILED", message, reason);
            case Error.ExternalServiceError(var service, var message, _) ->
                new ErrorResponse("EXTERNAL_SERVICE_ERROR", message, "Service: " + service);
            case Error.InvalidInputError(var field, var message) ->
                new ErrorResponse("INVALID_INPUT", message, "Field: " + field);
            case Error.ResourceNotFoundError(var resource, var identifier) ->
                new ErrorResponse("RESOURCE_NOT_FOUND", resource + " not found", "ID: " + identifier);
            case Error.QuotaExceededError(var currentUsage, var dailyLimit) ->
                new ErrorResponse("QUOTA_EXCEEDED", "YouTube API quota exceeded", "Usage: " + currentUsage + "/" + dailyLimit);
        };
    }
}
