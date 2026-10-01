package com.codegym.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class User {

    @NotEmpty(message = "Tên không được để trống")
    @Size(min = 2, max = 30, message = "Độ dài tên phải từ 2 đến 30 ký tự")
    private String name;

    @NotNull(message = "Tuổi không được để trống")
    @Min(value = 18, message = "Tuổi phải từ 18 trở lên")
    private Integer age;

    public User() {
    }

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}