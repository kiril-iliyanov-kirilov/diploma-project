package com.diploma.bookapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BookApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookApiApplication.class, args);
    }
}
