package br.edu.unigran.barbearia.web;

import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import br.edu.unigran.barbearia.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final UsuarioService usuarios;
    private final NotificacaoService notificacoes;

    public AuthController(AuthService auth, UsuarioService usuarios, NotificacaoService notificacoes) { this.auth = auth; this.usuarios = usuarios; this.notificacoes = notificacoes; }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody CadastroRequest r) { return auth.cadastrar(r); }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioPrincipal p) { return usuarios.perfil(p.id()); }

    @PutMapping("/me")
    public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody PerfilRequest r) { return usuarios.atualizarPerfil(p.id(), r); }

    @GetMapping("/notificacoes")
    public Map<String, Object> notificacoes(@AuthenticationPrincipal UsuarioPrincipal p) {
        return Map.of("naoLidas", notificacoes.naoLidas(p.id()), "itens", notificacoes.listar(p.id()));
    }

    @PostMapping("/notificacoes/{id}/lida") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lida(@AuthenticationPrincipal UsuarioPrincipal p, @PathVariable Long id) { notificacoes.marcarLida(p.id(), id); }

    @PostMapping("/notificacoes/lidas") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void todasLidas(@AuthenticationPrincipal UsuarioPrincipal p) { notificacoes.marcarTodasLidas(p.id()); }
}
