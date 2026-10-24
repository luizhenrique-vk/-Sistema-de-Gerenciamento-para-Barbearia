package br.edu.unigran.barbearia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BarbeariaApplication {

    public static void main(String[] args) {
        // Um único fuso em toda a JVM (banco, relógio e datas). Evita deslocamentos de horário entre a aplicação e o MySQL.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "America/Campo_Grande")));
        SpringApplication.run(BarbeariaApplication.class, args);
    }
}