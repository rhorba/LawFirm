package com.boilerplate.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCaseNumberFormatException extends RuntimeException {
    public InvalidCaseNumberFormatException(String message) {
        super(message);
    }
}
