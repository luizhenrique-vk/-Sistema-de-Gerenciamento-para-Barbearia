package br.edu.unigran.barbearia.config;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig {
    /** Relógio único da aplicação (fuso da barbearia). Facilita testes de regras dependentes de horário. */
    @Bean
    public Clock clock(AppProperties props) {
                String tz = props.timezone();
        return Clock.system(tz == null || tz.isBlank() ? ZoneId.systemDefault() : ZoneId.of(tz));
    }
}
