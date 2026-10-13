package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the AI-Assisted URL Shortener.
 *
 * <p>T1 scaffold: boots the Spring context, connects to PostgreSQL, runs the Flyway baseline,
 * and exposes the actuator health endpoint. Domain features arrive in later tasks.
 */
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
