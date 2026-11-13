package br.com.keyfalls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
public class KeyFallsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeyFallsApplication.class, args);
    }

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
