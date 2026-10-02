package com.codegym.exception;

public class InvalidBorrowCodeException extends RuntimeException {
    public InvalidBorrowCodeException() {
        super("Mã mượn sách không hợp lệ hoặc sách đã được hoàn trả trước đó!");
    }

    public InvalidBorrowCodeException(String message) {
        super(message);
    }
}
