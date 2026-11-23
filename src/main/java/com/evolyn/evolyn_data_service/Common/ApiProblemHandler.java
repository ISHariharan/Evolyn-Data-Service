package com.evolyn.evolyn_data_service.Common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns an ApiProblem thrown anywhere into its JSON answer. */
@RestControllerAdvice
public class ApiProblemHandler {

    @ExceptionHandler(ApiProblem.class)
    public ResponseEntity<Object> handle(ApiProblem problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem.getBody());
    }
}
