package me.msamoilov.example.log4j2.stackdriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @author Maksim Samoilov <samoylov.md@gmail.com>
 * @since 10.06.19
 */
@RestController
@RequestMapping("/")
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @RequestMapping("/log")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void logMessage(@RequestParam String message) {
        LOGGER.info(message);
    }
}
