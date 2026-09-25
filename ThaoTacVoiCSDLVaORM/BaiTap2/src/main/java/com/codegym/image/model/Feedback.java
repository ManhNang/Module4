package com.codegym.image.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int mark; // Thang điểm từ 1 đến 5

    @Column(nullable = false, length = 100)
    private String author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String feedback;

    @Column(nullable = false)
    private int likes = 0;

    @Column(nullable = false)
    private LocalDate date;

    public Feedback() {
        this.date = LocalDate.now();
        this.likes = 0;
    }

    public Feedback(int mark, String author, String feedback) {
        this.mark = mark;
        this.author = author;
        this.feedback = feedback;
        this.likes = 0;
        this.date = LocalDate.now();
    }

    public Feedback(Long id, int mark, String author, String feedback, int likes, LocalDate date) {
        this.id = id;
        this.mark = mark;
        this.author = author;
        this.feedback = feedback;
        this.likes = likes;
        this.date = date != null ? date : LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
