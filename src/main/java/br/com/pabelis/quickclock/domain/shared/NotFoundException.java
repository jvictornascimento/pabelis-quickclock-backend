package br.com.pabelis.quickclock.domain.shared;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
