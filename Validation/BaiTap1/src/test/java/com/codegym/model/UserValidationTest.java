package com.codegym.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

public class UserValidationTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void testValidUser() {
        user.setFirstName("Alexander");
        user.setLastName("Hamilton");
        user.setPhoneNumber("0912345678");
        user.setAge(25);
        user.setEmail("alexander.hamilton@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertFalse(errors.hasErrors(), "Valid user should not have errors");
    }

    @Test
    void testInvalidFirstName_EmptyAndTooShort() {
        user.setFirstName("");
        user.setLastName("Hamilton");
        user.setPhoneNumber("0912345678");
        user.setAge(25);
        user.setEmail("test@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("firstName"));

        // Test length < 5
        user.setFirstName("Alex");
        errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);
        assertTrue(errors.hasFieldErrors("firstName"));
    }

    @Test
    void testInvalidLastName_TooLong() {
        user.setFirstName("Alexander");
        user.setLastName("A".repeat(46));
        user.setPhoneNumber("0912345678");
        user.setAge(25);
        user.setEmail("test@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("lastName"));
    }

    @Test
    void testInvalidPhoneNumber_NotStartWithZero() {
        user.setFirstName("Alexander");
        user.setLastName("Hamilton");
        user.setPhoneNumber("1912345678");
        user.setAge(25);
        user.setEmail("test@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("phoneNumber"));
    }

    @Test
    void testInvalidPhoneNumber_ContainsLetters() {
        user.setFirstName("Alexander");
        user.setLastName("Hamilton");
        user.setPhoneNumber("0912345abc");
        user.setAge(25);
        user.setEmail("test@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("phoneNumber"));
    }

    @Test
    void testInvalidAge_Under18OrNull() {
        user.setFirstName("Alexander");
        user.setLastName("Hamilton");
        user.setPhoneNumber("0912345678");
        user.setAge(17);
        user.setEmail("test@example.com");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("age"));

        user.setAge(null);
        errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);
        assertTrue(errors.hasFieldErrors("age"));
    }

    @Test
    void testInvalidEmail() {
        user.setFirstName("Alexander");
        user.setLastName("Hamilton");
        user.setPhoneNumber("0912345678");
        user.setAge(20);
        user.setEmail("invalid-email");

        Errors errors = new BeanPropertyBindingResult(user, "user");
        user.validate(user, errors);

        assertTrue(errors.hasFieldErrors("email"));
    }
}
