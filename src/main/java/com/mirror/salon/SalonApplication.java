package com.mirror.salon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SalonApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalonApplication.class, args);
    }
}
