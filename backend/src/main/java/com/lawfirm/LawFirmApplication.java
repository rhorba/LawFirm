package com.lawfirm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LawFirmApplication {
    public static void main(String[] args) {
        SpringApplication.run(LawFirmApplication.class, args);
    }
}
