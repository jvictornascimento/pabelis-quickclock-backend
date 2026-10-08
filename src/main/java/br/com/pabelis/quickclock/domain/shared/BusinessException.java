package br.com.pabelis.quickclock.domain.shared;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
