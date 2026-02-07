package com.boilerplate.presentation.exception;

public class GroupHasUsersException extends RuntimeException {
    public GroupHasUsersException(String message) {
        super(message);
    }
}
