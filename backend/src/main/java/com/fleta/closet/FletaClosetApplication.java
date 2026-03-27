package com.fleta.closet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FletaClosetApplication {
    public static void main(String[] args) {
        SpringApplication.run(FletaClosetApplication.class, args);
    }
}
