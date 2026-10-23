package br.edu.unigran.barbearia.dto;

import br.edu.unigran.barbearia.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/** Contratos de entrada e saída da API. Nenhuma resposta contém senha. */
public final class Dtos {
    private Dtos() {}

    // ---------- Autenticação / usuários ----------
    public record LoginRequest(@NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
                               @NotBlank(message = "Informe a senha") String senha) {}

    public record CadastroRequest(@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
                                  @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
                                  @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Telefone inválido") String telefone,
                                  @NotBlank(message = "Informe a senha") @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha) {}

    public record UsuarioResponse(Long id, String nome, String email, String telefone, TipoUsuario tipo, boolean ativo, Long barbeiroId) {
        public static UsuarioResponse de(Usuario u) { return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getTipo(), u.isAtivo(), null); }
        public UsuarioResponse comBarbeiro(Long id) { return new UsuarioResponse(this.id, nome, email, telefone, tipo, ativo, id); }
    }

    public record AuthResponse(String token, UsuarioResponse usuario) {}

    public record PerfilRequest(@NotBlank @Size(max = 120) String nome,
                                @NotBlank @Email @Size(max = 160) String email,
                                @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Telefone inválido") String telefone,
                                String senhaAtual,
                                @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres") String novaSenha) {}

    public record UsuarioAdminRequest(@NotBlank @Size(max = 120) String nome,
                                      @NotBlank @Email @Size(max = 160) String email,
                                      @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Telefone inválido") String telefone,
                                      @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha,
                                      @NotNull(message = "Informe o tipo") TipoUsuario tipo,
                                      Boolean ativo) {}

    // ---------- Barbeiros / serviços ----------
    public record BarbeiroRequest(@NotBlank @Size(max = 120) String nome,
                                  @NotBlank @Email @Size(max = 160) String email,
                                  @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Telefone inválido") String telefone,
                                  @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha,
                                  @NotBlank(message = "Informe a especialidade") @Size(max = 120) String especialidade,
                                  Boolean ativo) {}

    public record BarbeiroResponse(Long id, Long usuarioId, String nome, String email, String telefone, String especialidade, boolean ativo, Double mediaAvaliacao) {}

    public record PerfilBarbeiroRequest(@NotBlank(message = "Informe a especialidade") @Size(max = 120) String especialidade) {}

    public record ServicoRequest(@NotBlank @Size(max = 100) String nome,
                                 @Size(max = 400) String descricao,
                                 @NotNull(message = "Informe o preço") @DecimalMin(value = "0.00", message = "Preço inválido") @Digits(integer = 8, fraction = 2) BigDecimal preco,
                                 @Min(value = 5, message = "Duração mínima de 5 minutos") @Max(value = 480) int duracao,
                                 Boolean ativo) {}

    public record ServicoResponse(Long id, String nome, String descricao, BigDecimal preco, int duracao, boolean ativo) {
        public static ServicoResponse de(Servico s) { return new ServicoResponse(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(), s.getDuracao(), s.isAtivo()); }
    }

    // ---------- Horários ----------
    public record DisponibilidadeItem(@Min(1) @Max(7) int diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFim) {}
    public record DisponibilidadeRequest(@NotNull @Valid List<@Valid DisponibilidadeItem> itens) {}
    public record HorarioFuncionamentoItem(@Min(1) @Max(7) int diaSemana, boolean aberto, @NotNull LocalTime abertura, @NotNull LocalTime fechamento) {}
    public record HorarioFuncionamentoRequest(@NotNull @Size(min = 7, max = 7, message = "Informe os 7 dias da semana") @Valid List<@Valid HorarioFuncionamentoItem> dias) {}
    public record SlotsResponse(LocalDate data, List<LocalTime> horarios) {}

    // ---------- Agendamentos ----------
    public record AgendamentoRequest(@NotNull(message = "Informe o barbeiro") Long barbeiroId,
                                     @NotNull(message = "Informe o serviço") Long servicoId,
                                     @NotNull(message = "Informe a data e o horário") LocalDateTime dataHora,
                                     @Size(max = 400) String observacoes,
                                     Long clienteId) {}

    public record StatusRequest(@NotNull StatusAgendamento status) {}
    public record PagamentoRequest(@NotNull(message = "Informe a forma de pagamento") FormaPagamento forma) {}

    public record AgendamentoResponse(Long id, Long clienteId, String clienteNome, String clienteTelefone,
                                      Long barbeiroId, String barbeiroNome, Long servicoId, String servicoNome, int duracao,
                                      LocalDateTime dataHora, LocalDateTime dataHoraFim, StatusAgendamento status, String observacoes,
                                      BigDecimal preco, boolean pago, FormaPagamento formaPagamento, LocalDateTime dataPagamento,
                                      boolean avaliado) {}

    // ---------- Avaliações / notificações ----------
    public record AvaliacaoRequest(@NotNull Long agendamentoId,
                                   @Min(value = 1, message = "Nota de 1 a 5") @Max(value = 5, message = "Nota de 1 a 5") int nota,
                                   @Size(max = 500) String comentario) {}
    public record AvaliacaoResponse(Long id, Long agendamentoId, int nota, String comentario, LocalDateTime criadoEm) {}
    public record NotificacaoResponse(Long id, String mensagem, boolean lida, LocalDateTime criadaEm) {}

    // ---------- Relatórios ----------
    public record FinanceiroBucket(String rotulo, BigDecimal faturamento, long atendimentos) {}
    public record FinanceiroResponse(String periodo, LocalDate inicio, LocalDate fim, BigDecimal faturamento, BigDecimal pendenteRecebimento,
                                     long atendimentos, long cancelados, BigDecimal ticketMedio, List<FinanceiroBucket> serie) {}
    public record ServicoRanking(Long servicoId, String nome, long quantidade, BigDecimal valor) {}
    public record ResumoPeriodo(long total, long agendados, long finalizados, long cancelados, BigDecimal faturamento) {}
    public record DiaContagem(LocalDate data, long agendamentos) {}
    public record DashboardResponse(ResumoPeriodo hoje, ResumoPeriodo mes, Double mediaAvaliacao, List<ServicoRanking> topServicos, List<DiaContagem> ultimosDias) {}

    public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long total, int totalPaginas) {
        public static <T> Pagina<T> de(org.springframework.data.domain.Page<T> p) {
            return new Pagina<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
