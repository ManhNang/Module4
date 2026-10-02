package com.codegym.exception;

public class BookOutOfStockException extends RuntimeException {
    public BookOutOfStockException() {
        super("Sách này hiện tại đã hết số lượng trong thư viện, không thể mượn thêm!");
    }

    public BookOutOfStockException(String message) {
        super(message);
    }
}
