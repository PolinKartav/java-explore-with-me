package ru.practicum.ewm.main.util.exception;

public class AlreadyUsedException extends RuntimeException {
    public AlreadyUsedException(String message) {
        super(message);
    }
}
