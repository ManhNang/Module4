package com.codegym.exception;

public class DuplicateEmailException extends Exception {
    public DuplicateEmailException() {
        super("Email đã tồn tại trong hệ thống!");
    }

    public DuplicateEmailException(String message) {
        super(message);
    }
}