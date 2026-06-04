package shift.shift_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = exception.getStatusCode().toString();
        }
        return ResponseEntity.status(exception.getStatusCode()).body(message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException() {
        return ResponseEntity.status(401).body("Неверная почта или пароль.");
    }
}
