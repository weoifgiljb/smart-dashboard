package com.selfdiscipline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.selfdiscipline.repository")
public class SelfDisciplineApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelfDisciplineApplication.class, args);
    }
}






