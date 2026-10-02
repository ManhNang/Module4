package com.codegym.service;

import com.codegym.model.Book;
import com.codegym.model.BorrowTicket;

import java.util.List;

public interface IBookService {
    List<Book> findAll();
    Book findById(Long id);
    void save(Book book);
    void delete(Long id);
    BorrowTicket borrowBook(Long bookId);
    Book returnBook(String borrowCode);
}
