package edu.historyjourney.web;

import edu.historyjourney.model.GameException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorView(String code,String message,String action) {}
    @ExceptionHandler(GameException.class) public ResponseEntity<ErrorView> game(GameException e) {
        return ResponseEntity.status(e.status()).body(new ErrorView(e.code(),e.getMessage(),e.action()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class}) public ResponseEntity<ErrorView> input(Exception e) {
        return game(GameException.invalid("Проверьте обязательные поля, длину имени, аватар и режим."));
    }
}
