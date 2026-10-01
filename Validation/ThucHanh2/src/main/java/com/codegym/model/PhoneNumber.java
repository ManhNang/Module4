package com.codegym.model;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class PhoneNumber implements Validator {
    private String number;

    public PhoneNumber() {
    }

    public PhoneNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return PhoneNumber.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PhoneNumber phoneNumber = (PhoneNumber) target;
        String number = phoneNumber.getNumber();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "number", "number.empty");
        if (number != null && !number.isEmpty()) {
            if (!number.startsWith("0")) {
                errors.rejectValue("number", "number.startsWith");
            }
            if (number.length() < 10 || number.length() > 11) {
                errors.rejectValue("number", "number.length");
            }
            if (!number.matches("(^$|[0-9]*$)")) {
                errors.rejectValue("number", "number.matches");
            }
        }
    }
}