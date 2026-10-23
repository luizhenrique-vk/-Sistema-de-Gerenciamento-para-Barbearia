package br.edu.unigran.barbearia.web;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import br.edu.unigran.barbearia.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/** Todas as rotas /api/admin/** exigem ROLE_ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UsuarioService usuarios;
    private final BarbeiroService barbeiros;
    private final ServicoService servicos;
    private final HorarioService horarios;
    private final AgendamentoService agendamentos;
    private final RelatorioService relatorios;

    public AdminController(UsuarioService usuarios, BarbeiroService barbeiros, ServicoService servicos, HorarioService horarios, AgendamentoService agendamentos, RelatorioService relatorios) {
        this.usuarios = usuarios; this.barbeiros = barbeiros; this.servicos = servicos; this.horarios = horarios; this.agendamentos = agendamentos; this.relatorios = relatorios;
    }

    // Usuários e clientes
    @GetMapping("/usuarios")
    public Pagina<UsuarioResponse> usuarios(@RequestParam(required = false) TipoUsuario tipo, @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanho) { return usuarios.listar(tipo, q, pagina, tamanho); }

    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criarUsuario(@Valid @RequestBody UsuarioAdminRequest r) { return usuarios.criar(r); }

    @PutMapping("/usuarios/{id}")
    public UsuarioResponse atualizarUsuario(@AuthenticationPrincipal UsuarioPrincipal p, @PathVariable Long id, @Valid @RequestBody UsuarioAdminRequest r) { return usuarios.atualizar(p.id(), id, r); }

    @DeleteMapping("/usuarios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirUsuario(@AuthenticationPrincipal UsuarioPrincipal p, @PathVariable Long id) { usuarios.excluir(p.id(), id); }

    // Barbeiros
    @GetMapping("/barbeiros")
    public List<BarbeiroResponse> barbeiros() { return barbeiros.listar(false); }

    @PostMapping("/barbeiros") @ResponseStatus(HttpStatus.CREATED)
    public BarbeiroResponse criarBarbeiro(@Valid @RequestBody BarbeiroRequest r) { return barbeiros.criar(r); }

    @PutMapping("/barbeiros/{id}")
    public BarbeiroResponse atualizarBarbeiro(@PathVariable Long id, @Valid @RequestBody BarbeiroRequest r) { return barbeiros.atualizar(id, r); }

    @DeleteMapping("/barbeiros/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirBarbeiro(@PathVariable Long id) { barbeiros.excluir(id); }

    @GetMapping("/barbeiros/{id}/disponibilidade")
    public List<DisponibilidadeItem> disponibilidade(@PathVariable Long id) { return horarios.disponibilidade(id); }

    @PutMapping("/barbeiros/{id}/disponibilidade")
    public List<DisponibilidadeItem> salvarDisponibilidade(@PathVariable Long id, @Valid @RequestBody DisponibilidadeRequest r) { return horarios.salvarDisponibilidade(id, r); }

    // Serviços
    @GetMapping("/servicos")
    public List<ServicoResponse> servicos() { return servicos.listar(false); }

    @PostMapping("/servicos") @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponse criarServico(@Valid @RequestBody ServicoRequest r) { return servicos.salvar(null, r); }

    @PutMapping("/servicos/{id}")
    public ServicoResponse atualizarServico(@PathVariable Long id, @Valid @RequestBody ServicoRequest r) { return servicos.salvar(id, r); }

    @DeleteMapping("/servicos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirServico(@PathVariable Long id) { servicos.excluir(id); }

    // Horário de funcionamento
    @GetMapping("/funcionamento")
    public List<HorarioFuncionamentoItem> funcionamento() { return horarios.funcionamento(); }

    @PutMapping("/funcionamento")
    public List<HorarioFuncionamentoItem> salvarFuncionamento(@Valid @RequestBody HorarioFuncionamentoRequest r) { return horarios.salvarFuncionamento(r); }

    // Agendamentos e pagamentos
    @GetMapping("/agendamentos")
    public Pagina<AgendamentoResponse> agendamentos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                                    @RequestParam(required = false) StatusAgendamento status, @RequestParam(required = false) Long barbeiroId,
                                                    @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "15") int tamanho) {
        return agendamentos.buscarAdmin(de, ate, status, barbeiroId, pagina, tamanho);
    }

    @PatchMapping("/agendamentos/{id}/pagamento")
    public AgendamentoResponse pagamento(@PathVariable Long id, @Valid @RequestBody PagamentoRequest r) { return agendamentos.registrarPagamento(id, r.forma()); }

    // Relatórios
    @GetMapping("/relatorios/dashboard")
    public DashboardResponse dashboard() { return relatorios.dashboard(); }

    @GetMapping("/relatorios/financeiro")
    public FinanceiroResponse financeiro(@RequestParam(defaultValue = "DIA") String periodo, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) { return relatorios.financeiro(periodo, data); }

    @GetMapping("/relatorios/servicos-mais-realizados")
    public List<ServicoRanking> ranking(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                        @RequestParam(defaultValue = "10") int limite) { return relatorios.servicosMaisRealizados(de, ate, limite); }
}
