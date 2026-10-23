package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.config.AppProperties;
import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AgendamentoService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;
    private final AvaliacaoRepository avaliacoes;
    private final HorarioService horarios;
    private final NotificacaoService notificacoes;
    private final AppProperties props;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentos, BarbeiroRepository barbeiros, ServicoRepository servicos, UsuarioRepository usuarios,
                              AvaliacaoRepository avaliacoes, HorarioService horarios, NotificacaoService notificacoes, AppProperties props, Clock clock) {
        this.agendamentos = agendamentos; this.barbeiros = barbeiros; this.servicos = servicos; this.usuarios = usuarios;
        this.avaliacoes = avaliacoes; this.horarios = horarios; this.notificacoes = notificacoes; this.props = props; this.clock = clock;
    }

    public AgendamentoResponse resp(Agendamento a) {
        return new AgendamentoResponse(a.getId(), a.getCliente().getId(), a.getCliente().getNome(), a.getCliente().getTelefone(),
                a.getBarbeiro().getId(), a.getBarbeiro().getUsuario().getNome(), a.getServico().getId(), a.getServico().getNome(), a.getServico().getDuracao(),
                a.getDataHora(), a.getDataHoraFim(), a.getStatus(), a.getObservacoes(), a.getPrecoCobrado(), a.isPago(), a.getFormaPagamento(),
                a.getDataPagamento(), avaliacoes.existsByAgendamentoId(a.getId()));
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long barbeiroId, Long servicoId, LocalDate data) {
        Servico s = servicos.findById(servicoId).filter(Servico::isAtivo).orElseThrow(() -> NegocioException.naoEncontrado("Serviço não encontrado."));
        barbeiros.findById(barbeiroId).filter(b -> b.getUsuario().isAtivo()).orElseThrow(() -> NegocioException.naoEncontrado("Barbeiro não encontrado."));
        return horarios.horariosLivres(barbeiroId, s.getDuracao(), data);
    }

    @Transactional
    public AgendamentoResponse criar(UsuarioPrincipal quem, AgendamentoRequest req) {
        // O cliente é sempre derivado da identidade autenticada; somente o administrador pode agendar em nome de um cliente.
        Usuario cliente;
        switch (quem.tipo()) {
            case CLIENTE -> cliente = usuarios.findById(quem.id()).orElseThrow();
            case ADMIN -> {
                if (req.clienteId() == null) throw NegocioException.invalido("Informe o cliente.");
                cliente = usuarios.findById(req.clienteId()).filter(u -> u.getTipo() == TipoUsuario.CLIENTE && u.isAtivo())
                        .orElseThrow(() -> NegocioException.invalido("Cliente inválido ou inativo."));
            }
            default -> throw NegocioException.proibido("Seu perfil não pode criar agendamentos.");
        }
        // Bloqueio do barbeiro: duas requisições simultâneas para o mesmo barbeiro são processadas em sequência.
        Barbeiro barbeiro = barbeiros.findByIdForUpdate(req.barbeiroId()).filter(b -> b.getUsuario().isAtivo())
                .orElseThrow(() -> NegocioException.invalido("Barbeiro inválido ou inativo."));
        Servico servico = servicos.findById(req.servicoId()).filter(Servico::isAtivo)
                .orElseThrow(() -> NegocioException.invalido("Serviço inválido ou inativo."));
        LocalDateTime inicio = req.dataHora().withSecond(0).withNano(0);
        if (!inicio.isAfter(LocalDateTime.now(clock))) throw NegocioException.invalido("Escolha um horário futuro.");

        if (!horarios.horariosLivres(barbeiro.getId(), servico.getDuracao(), inicio.toLocalDate()).contains(inicio.toLocalTime()))
            throw NegocioException.conflito("Este horário não está mais disponível para o barbeiro escolhido. Selecione outro horário.");
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracao());
        if (!agendamentos.conflitosCliente(cliente.getId(), inicio, fim).isEmpty())
            throw NegocioException.conflito("O cliente já possui um agendamento neste mesmo período.");

        Agendamento a = new Agendamento();
        a.setCliente(cliente); a.setBarbeiro(barbeiro); a.setServico(servico);
        a.setDataHora(inicio); a.setDataHoraFim(fim); a.setPrecoCobrado(servico.getPreco());
        a.setObservacoes(req.observacoes() == null || req.observacoes().isBlank() ? null : req.observacoes().trim());
        agendamentos.save(a);
        notificacoes.notificar(cliente, "Agendamento confirmado: " + servico.getNome() + " com " + barbeiro.getUsuario().getNome() + " em " + inicio.format(FMT) + ".");
        notificacoes.notificar(barbeiro.getUsuario(), "Novo agendamento: " + servico.getNome() + " com " + cliente.getNome() + " em " + inicio.format(FMT) + ".");
        return resp(a);
    }

    private Agendamento carregar(Long id) {
        return agendamentos.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Agendamento não encontrado."));
    }

    /** Retorna 404 (e não 403) quando o registro pertence a outra pessoa, para não revelar a existência de dados alheios. */
    private Agendamento carregarComAcesso(UsuarioPrincipal quem, Long id) {
        Agendamento a = carregar(id);
        boolean ok = switch (quem.tipo()) {
            case ADMIN -> true;
            case CLIENTE -> a.getCliente().getId().equals(quem.id());
            case BARBEIRO -> a.getBarbeiro().getUsuario().getId().equals(quem.id());
        };
        if (!ok) throw NegocioException.naoEncontrado("Agendamento não encontrado.");
        return a;
    }

    @Transactional
    public AgendamentoResponse cancelar(UsuarioPrincipal quem, Long id) {
        Agendamento a = carregarComAcesso(quem, id);
        if (a.getStatus() != StatusAgendamento.AGENDADO) throw NegocioException.invalido("Somente agendamentos com status Agendado podem ser cancelados.");
        LocalDateTime agora = LocalDateTime.now(clock);
        if (quem.tipo() == TipoUsuario.CLIENTE && a.getDataHora().isBefore(agora.plusHours(props.cancelamentoAntecedenciaHoras())))
            throw NegocioException.invalido("O cancelamento deve ser feito com pelo menos " + props.cancelamentoAntecedenciaHoras() + " horas de antecedência. Entre em contato com a barbearia.");
        a.setStatus(StatusAgendamento.CANCELADO);
        String quando = a.getDataHora().format(FMT);
        if (quem.tipo() == TipoUsuario.CLIENTE) {
            notificacoes.notificar(a.getBarbeiro().getUsuario(), a.getCliente().getNome() + " cancelou o agendamento de " + quando + ".");
        } else {
            notificacoes.notificar(a.getCliente(), "Seu agendamento de " + quando + " (" + a.getServico().getNome() + ") foi cancelado pela barbearia.");
        }
        return resp(a);
    }

    /** Barbeiro (apenas os próprios) e administrador alteram o status: AGENDADO -> FINALIZADO ou CANCELADO. */
    @Transactional
    public AgendamentoResponse atualizarStatus(UsuarioPrincipal quem, Long id, StatusAgendamento novo) {
        if (quem.tipo() == TipoUsuario.CLIENTE) throw NegocioException.proibido("Seu perfil não pode alterar o status do atendimento.");
        if (novo == StatusAgendamento.CANCELADO) return cancelar(quem, id);
        if (novo != StatusAgendamento.FINALIZADO) throw NegocioException.invalido("Transição de status inválida.");
        Agendamento a = carregarComAcesso(quem, id);
        if (a.getStatus() != StatusAgendamento.AGENDADO) throw NegocioException.invalido("Somente agendamentos com status Agendado podem ser finalizados.");
        if (a.getDataHora().isAfter(LocalDateTime.now(clock))) throw NegocioException.invalido("O atendimento só pode ser finalizado a partir do horário agendado.");
        a.setStatus(StatusAgendamento.FINALIZADO);
        notificacoes.notificar(a.getCliente(), "Atendimento finalizado. Avalie sua experiência em Histórico.");
        return resp(a);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> doCliente(Long clienteId, boolean somenteFinalizados) {
        return agendamentos.doCliente(clienteId).stream()
                .filter(a -> !somenteFinalizados || a.getStatus() == StatusAgendamento.FINALIZADO).map(this::resp).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> agendaDoBarbeiro(Long usuarioId, LocalDate de, int dias) {
        Barbeiro b = barbeiros.findByUsuarioId(usuarioId).orElseThrow(() -> NegocioException.naoEncontrado("Perfil de barbeiro não encontrado."));
        LocalDate ini = de != null ? de : LocalDate.now(clock);
        return agendamentos.doBarbeiroNoPeriodo(b.getId(), ini.atStartOfDay(), ini.plusDays(Math.min(Math.max(dias, 1), 31)).atStartOfDay()).stream().map(this::resp).toList();
    }

    @Transactional(readOnly = true)
    public Pagina<AgendamentoResponse> buscarAdmin(LocalDate de, LocalDate ate, StatusAgendamento status, Long barbeiroId, int pagina, int tamanho) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ini = de != null ? de : hoje.minusDays(30);
        LocalDate fim = ate != null ? ate : hoje.plusDays(60);
        Pageable p = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 50));
        return Pagina.de(agendamentos.buscar(ini.atStartOfDay(), fim.plusDays(1).atStartOfDay(), status, barbeiroId, p).map(this::resp));
    }

    @Transactional
    public AgendamentoResponse registrarPagamento(Long id, FormaPagamento forma) {
        Agendamento a = carregar(id);
        if (a.getStatus() != StatusAgendamento.FINALIZADO) throw NegocioException.invalido("Só é possível registrar pagamento de atendimentos finalizados.");
        if (a.isPago()) throw NegocioException.conflito("O pagamento deste atendimento já foi registrado.");
        a.setPago(true); a.setFormaPagamento(forma); a.setDataPagamento(LocalDateTime.now(clock));
        return resp(a);
    }

    /** Lembretes: executa a cada 15 minutos e gera notificação interna para atendimentos nas próximas horas configuradas. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 900_000, initialDelay = 60_000)
    @Transactional
    public void gerarLembretes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        for (Agendamento a : agendamentos.paraLembrete(agora, agora.plusHours(props.lembreteAntecedenciaHoras()))) {
            notificacoes.notificar(a.getCliente(), "Lembrete: " + a.getServico().getNome() + " em " + a.getDataHora().format(FMT) + ".");
            a.setLembreteEnviado(true);
        }
    }
}
