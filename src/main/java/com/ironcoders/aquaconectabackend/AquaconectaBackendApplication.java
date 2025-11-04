package com.ironcoders.aquaconectabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AquaconectaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquaconectaBackendApplication.class, args);
    }

}
                                                                                               