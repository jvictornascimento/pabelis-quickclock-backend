package br.com.pabelis.quickclock.adapters.in.web.finance;

import br.com.pabelis.quickclock.adapters.in.web.shared.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = FinanceController.class)
public class FinanceExceptionHandler {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("Invalid finance request, date, month or enum value."));
    }
    @ExceptionHandler(ArithmeticException.class)
    ResponseEntity<ApiErrorResponse> overflow(ArithmeticException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("Financial total exceeds the supported integer range."));
    }
}
