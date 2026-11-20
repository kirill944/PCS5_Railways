package ru.mirea.railways;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.mirea.railways.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class RailwayBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RailwayBookingApplication.class, args);
    }
}