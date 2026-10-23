package br.edu.unigran.barbearia.security;

import br.edu.unigran.barbearia.repository.UsuarioRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Lê o token Bearer e carrega o usuário atual do banco a cada requisição (contas desativadas perdem acesso imediatamente). */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwt, UsuarioRepository usuarios) { this.jwt = jwt; this.usuarios = usuarios; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwt.validar(h.substring(7)).flatMap(usuarios::findById).filter(u -> u.isAtivo()).ifPresent(u -> {
                UsuarioPrincipal p = UsuarioPrincipal.de(u);
                var auth = new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(req, res);
    }
}
