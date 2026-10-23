package br.edu.unigran.barbearia.security;

import br.edu.unigran.barbearia.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final AppProperties props;
    private final Clock clock;
    private SecretKey chave;

    public JwtService(AppProperties props, Clock clock) { this.props = props; this.clock = clock; }

    @PostConstruct
    void iniciar() {
        String segredo = props.jwtSecret();
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException("Defina a variável de ambiente APP_JWT_SECRET com pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerar(Long usuarioId) {
        Date agora = Date.from(clock.instant());
        Date exp = new Date(agora.getTime() + props.jwtExpiracaoHoras() * 3_600_000L);
        return Jwts.builder().subject(String.valueOf(usuarioId)).issuedAt(agora).expiration(exp).signWith(chave).compact();
    }

    public Optional<Long> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(c.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
