package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.Usuario;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.UsuarioRepository;
import br.edu.unigran.barbearia.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    // Hash fictício para igualar o tempo de resposta quando o e-mail não existe (evita enumeração de contas).
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, UsuarioService usuarioService, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios; this.usuarioService = usuarioService; this.encoder = encoder; this.jwt = jwt;
        this.hashFalso = encoder.encode("senha-fictícia-para-tempo-constante");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        boolean ok = encoder.matches(req.senha(), u != null ? u.getSenha() : hashFalso) && u != null && u.isAtivo();
        if (!ok) throw new NegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        return new AuthResponse(jwt.gerar(u.getId()), usuarioService.resposta(u));
    }

    @Transactional
    public AuthResponse cadastrar(CadastroRequest req) {
        Usuario u = usuarioService.cadastrarCliente(req);
        return new AuthResponse(jwt.gerar(u.getId()), usuarioService.resposta(u));
    }
}
