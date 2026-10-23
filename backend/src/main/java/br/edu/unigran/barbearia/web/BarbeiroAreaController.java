package br.edu.unigran.barbearia.web;

import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import br.edu.unigran.barbearia.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/** Área do barbeiro: tudo é escopado ao barbeiro autenticado (rota protegida por ROLE_BARBEIRO). */
@RestController
@RequestMapping("/api/barbeiro")
public class BarbeiroAreaController {
    private final AgendamentoService agendamentos;
    private final BarbeiroService barbeiros;
    private final HorarioService horarios;

    public BarbeiroAreaController(AgendamentoService agendamentos, BarbeiroService barbeiros, HorarioService horarios) { this.agendamentos = agendamentos; this.barbeiros = barbeiros; this.horarios = horarios; }

    @GetMapping("/agenda")
    public List<AgendamentoResponse> agenda(@AuthenticationPrincipal UsuarioPrincipal p,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                            @RequestParam(defaultValue = "1") int dias) {
        return agendamentos.agendaDoBarbeiro(p.id(), data, dias);
    }

    @GetMapping("/perfil")
    public BarbeiroResponse perfil(@AuthenticationPrincipal UsuarioPrincipal p) { return barbeiros.doUsuario(p.id()); }

    @PutMapping("/perfil")
    public BarbeiroResponse atualizarPerfil(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody PerfilBarbeiroRequest r) { return barbeiros.atualizarEspecialidade(p.id(), r); }

    @GetMapping("/disponibilidade")
    public List<DisponibilidadeItem> disponibilidade(@AuthenticationPrincipal UsuarioPrincipal p) { return horarios.disponibilidade(barbeiros.porUsuario(p.id()).getId()); }

    @PutMapping("/disponibilidade")
    public List<DisponibilidadeItem> salvar(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody DisponibilidadeRequest r) {
        return horarios.salvarDisponibilidade(barbeiros.porUsuario(p.id()).getId(), r);
    }

    @GetMapping("/funcionamento")
    public List<HorarioFuncionamentoItem> funcionamento() { return horarios.funcionamento(); }
}
