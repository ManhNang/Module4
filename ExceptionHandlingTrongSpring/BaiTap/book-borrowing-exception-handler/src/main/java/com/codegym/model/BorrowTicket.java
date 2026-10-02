package com.codegym.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_tickets")
public class BorrowTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 5)
    private String borrowCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDateTime borrowDate;

    @Column(nullable = false)
    private boolean returned = false;

    private LocalDateTime returnDate;

    public BorrowTicket() {
    }

    public BorrowTicket(String borrowCode, Book book, LocalDateTime borrowDate) {
        this.borrowCode = borrowCode;
        this.book = book;
        this.borrowDate = borrowDate;
        this.returned = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBorrowCode() {
        return borrowCode;
    }

    public void setBorrowCode(String borrowCode) {
        this.borrowCode = borrowCode;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public boolean isReturned() {
        return returned;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }
}
