package com.codegym.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException() {
        super("Không tìm thấy thông tin cuốn sách trong hệ thống!");
    }

    public BookNotFoundException(String message) {
        super(message);
    }
}
