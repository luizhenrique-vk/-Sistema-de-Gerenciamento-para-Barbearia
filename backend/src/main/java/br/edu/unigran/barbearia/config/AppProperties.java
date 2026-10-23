package br.edu.unigran.barbearia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        int jwtExpiracaoHoras,
        String corsOrigens,
        String adminInicialEmail,
        String adminInicialSenha,
        String timezone,
        int cancelamentoAntecedenciaHoras,
        int lembreteAntecedenciaHoras,
        int intervaloSlotMinutos) {
}
