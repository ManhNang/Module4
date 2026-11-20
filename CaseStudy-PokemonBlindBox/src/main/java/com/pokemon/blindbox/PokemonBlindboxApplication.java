package com.pokemon.blindbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PokemonBlindboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(PokemonBlindboxApplication.class, args);
    }
}
