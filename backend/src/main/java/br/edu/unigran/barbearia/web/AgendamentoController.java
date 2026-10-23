package br.edu.unigran.barbearia.web;

import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import br.edu.unigran.barbearia.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AgendamentoController {
    private final AgendamentoService agendamentos;
    private final AvaliacaoService avaliacoes;

    public AgendamentoController(AgendamentoService agendamentos, AvaliacaoService avaliacoes) { this.agendamentos = agendamentos; this.avaliacoes = avaliacoes; }

    @PostMapping("/agendamentos") @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody AgendamentoRequest r) { return agendamentos.criar(p, r); }

    /** Agendamentos do cliente autenticado (o identificador nunca é recebido do cliente HTTP). */
    @GetMapping("/agendamentos/meus")
    public List<AgendamentoResponse> meus(@AuthenticationPrincipal UsuarioPrincipal p) { return agendamentos.doCliente(p.id(), false); }

    @GetMapping("/agendamentos/historico")
    public List<AgendamentoResponse> historico(@AuthenticationPrincipal UsuarioPrincipal p) { return agendamentos.doCliente(p.id(), true); }

    @PostMapping("/agendamentos/{id}/cancelar")
    public AgendamentoResponse cancelar(@AuthenticationPrincipal UsuarioPrincipal p, @PathVariable Long id) { return agendamentos.cancelar(p, id); }

    @PatchMapping("/agendamentos/{id}/status")
    public AgendamentoResponse status(@AuthenticationPrincipal UsuarioPrincipal p, @PathVariable Long id, @Valid @RequestBody StatusRequest r) { return agendamentos.atualizarStatus(p, id, r.status()); }

    @PostMapping("/avaliacoes") @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse avaliar(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody AvaliacaoRequest r) { return avaliacoes.avaliar(p, r); }

    @GetMapping("/avaliacoes/minhas")
    public List<AvaliacaoResponse> minhas(@AuthenticationPrincipal UsuarioPrincipal p) { return avaliacoes.doCliente(p.id()); }
}
