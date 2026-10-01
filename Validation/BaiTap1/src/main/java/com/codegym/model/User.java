package com.codegym.model;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class User implements Validator {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Integer age;
    private String email;

    public User() {
    }

    public User(String firstName, String lastName, String phoneNumber, Integer age, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstname() {
        return firstName;
    }

    public void setFirstname(String firstname) {
        this.firstName = firstname;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastname() {
        return lastName;
    }

    public void setLastname(String lastname) {
        this.lastName = lastname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhonenumber() {
        return phoneNumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phoneNumber = phonenumber;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;

        // Validate firstName: bắt buộc, độ dài từ 5 đến 45 ký tự
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstName", "firstName.empty");
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            if (user.getFirstName().length() < 5 || user.getFirstName().length() > 45) {
                errors.rejectValue("firstName", "firstName.length");
            }
        }

        // Validate lastName: bắt buộc, độ dài từ 5 đến 45 ký tự
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "lastName", "lastName.empty");
        if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
            if (user.getLastName().length() < 5 || user.getLastName().length() > 45) {
                errors.rejectValue("lastName", "lastName.length");
            }
        }

        // Validate phoneNumber: bắt đầu bằng 0, độ dài 10-11 số, chỉ chứa chữ số
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "phoneNumber", "phoneNumber.empty");
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (!user.getPhoneNumber().startsWith("0")) {
                errors.rejectValue("phoneNumber", "phoneNumber.startsWith");
            } else if (user.getPhoneNumber().length() < 10 || user.getPhoneNumber().length() > 11) {
                errors.rejectValue("phoneNumber", "phoneNumber.length");
            } else if (!user.getPhoneNumber().matches("^[0-9]+$")) {
                errors.rejectValue("phoneNumber", "phoneNumber.matches");
            }
        }

        // Validate age: bắt buộc, tuổi >= 18
        if (user.getAge() == null) {
            errors.rejectValue("age", "age.empty");
        } else if (user.getAge() < 18) {
            errors.rejectValue("age", "age.min");
        }

        // Validate email: bắt buộc, đúng định dạng email
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "email.empty");
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!user.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                errors.rejectValue("email", "email.matches");
            }
        }
    }
}
