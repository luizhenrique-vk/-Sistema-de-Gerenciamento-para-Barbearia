package br.edu.unigran.barbearia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BarbeariaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BarbeariaApplication.class, args);
    }
}
