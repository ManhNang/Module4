package com.codegym.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowTicket> borrowTickets = new ArrayList<>();

    public Book() {
    }

    public Book(String title, String author, int quantity, String description) {
        this.title = title;
        this.author = author;
        this.quantity = quantity;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<BorrowTicket> getBorrowTickets() {
        return borrowTickets;
    }

    public void setBorrowTickets(List<BorrowTicket> borrowTickets) {
        this.borrowTickets = borrowTickets;
    }
}
