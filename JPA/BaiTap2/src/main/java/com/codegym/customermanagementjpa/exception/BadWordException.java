package com.codegym.customermanagementjpa.exception;

import com.codegym.customermanagementjpa.model.Feedback;
import java.time.LocalDateTime;

public class BadWordException extends RuntimeException {

    private final Feedback feedback;
    private final String badWord;
    private final LocalDateTime timestamp;

    public BadWordException(String message, Feedback feedback, String badWord) {
        super(message);
        this.feedback = feedback;
        this.badWord = badWord;
        this.timestamp = LocalDateTime.now();
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public String getBadWord() {
        return badWord;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
