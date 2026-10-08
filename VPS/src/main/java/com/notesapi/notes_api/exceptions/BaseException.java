package com.notesapi.notes_api.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {
    private final HttpStatus statusCode;

    public BaseException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
