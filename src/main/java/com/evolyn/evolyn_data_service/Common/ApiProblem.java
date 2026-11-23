package com.evolyn.evolyn_data_service.Common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * A request that can't be done, with the HTTP status and JSON body to answer.
 * Throw it from a service; ApiProblemHandler turns it into the response.
 * Example: throw ApiProblem.of(HttpStatus.NOT_FOUND, "Tribe not found");
 */
public class ApiProblem extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> body;

    public ApiProblem(HttpStatus status, Map<String, Object> body) {
        super(String.valueOf(body.get("message")));
        this.status = status;
        this.body = body;
    }

    public static ApiProblem of(HttpStatus status, String message) {
        return new ApiProblem(status, Map.of("message", message));
    }

    public static ApiProblem of(HttpStatus status, String message, String code) {
        return new ApiProblem(status, Map.of("message", message, "code", code));
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getBody() { return body; }
}
