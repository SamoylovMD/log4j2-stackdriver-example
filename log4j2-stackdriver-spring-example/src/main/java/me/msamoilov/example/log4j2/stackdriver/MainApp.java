package me.msamoilov.example.log4j2.stackdriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Maksim Samoilov <samoylov.md@gmail.com>
 * @since 10.06.19
 */
@SpringBootApplication
public class MainApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Spring Boot application: {}", MainApp.class);
        SpringApplication.run(MainApp.class, args);
    }
}
