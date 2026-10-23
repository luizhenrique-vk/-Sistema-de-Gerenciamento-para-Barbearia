package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.config.AppProperties;
import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class HorarioService {
    private final HorarioFuncionamentoRepository funcionamento;
    private final DisponibilidadeRepository disponibilidades;
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;
    private final AppProperties props;
    private final Clock clock;

    public HorarioService(HorarioFuncionamentoRepository funcionamento, DisponibilidadeRepository disponibilidades, BarbeiroRepository barbeiros,
                          AgendamentoRepository agendamentos, AppProperties props, Clock clock) {
        this.funcionamento = funcionamento; this.disponibilidades = disponibilidades; this.barbeiros = barbeiros;
        this.agendamentos = agendamentos; this.props = props; this.clock = clock;
    }

    // ---------- Horário de funcionamento (administrador) ----------
    @Transactional(readOnly = true)
    public List<HorarioFuncionamentoItem> funcionamento() {
        return funcionamento.findAllByOrderByDiaSemanaAsc().stream()
                .map(h -> new HorarioFuncionamentoItem(h.getDiaSemana(), h.isAberto(), h.getAbertura(), h.getFechamento())).toList();
    }

    @Transactional
    public List<HorarioFuncionamentoItem> salvarFuncionamento(HorarioFuncionamentoRequest r) {
        Set<Integer> vistos = new HashSet<>();
        for (HorarioFuncionamentoItem i : r.dias()) {
            if (!vistos.add(i.diaSemana())) throw NegocioException.invalido("Dia da semana repetido.");
            if (i.aberto() && !i.fechamento().isAfter(i.abertura())) throw NegocioException.invalido("O fechamento deve ser após a abertura (dia " + i.diaSemana() + ").");
            HorarioFuncionamento h = funcionamento.findById(i.diaSemana()).orElseGet(HorarioFuncionamento::new);
            h.setDiaSemana(i.diaSemana()); h.setAberto(i.aberto()); h.setAbertura(i.abertura()); h.setFechamento(i.fechamento());
            funcionamento.save(h);
        }
        return funcionamento();
    }

    // ---------- Disponibilidade do barbeiro ----------
    @Transactional(readOnly = true)
    public List<DisponibilidadeItem> disponibilidade(Long barbeiroId) {
        return disponibilidades.findByBarbeiroIdOrderByDiaSemanaAscHoraInicioAsc(barbeiroId).stream()
                .map(d -> new DisponibilidadeItem(d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim())).toList();
    }

    /** Substitui a disponibilidade. Cada janela deve estar dentro do horário de funcionamento e não pode se sobrepor. */
    @Transactional
    public List<DisponibilidadeItem> salvarDisponibilidade(Long barbeiroId, DisponibilidadeRequest r) {
        Barbeiro b = barbeiros.findById(barbeiroId).orElseThrow(() -> NegocioException.naoEncontrado("Barbeiro não encontrado."));
        Map<Integer, HorarioFuncionamento> func = new HashMap<>();
        funcionamento.findAll().forEach(h -> func.put(h.getDiaSemana(), h));
        List<DisponibilidadeItem> itens = new ArrayList<>(r.itens());
        itens.sort(Comparator.comparingInt(DisponibilidadeItem::diaSemana).thenComparing(DisponibilidadeItem::horaInicio));
        DisponibilidadeItem anterior = null;
        for (DisponibilidadeItem i : itens) {
            if (!i.horaFim().isAfter(i.horaInicio())) throw NegocioException.invalido("O horário final deve ser após o inicial.");
            HorarioFuncionamento h = func.get(i.diaSemana());
            if (h == null || !h.isAberto()) throw NegocioException.invalido("A barbearia está fechada no dia " + i.diaSemana() + " (1 = segunda).");
            if (i.horaInicio().isBefore(h.getAbertura()) || i.horaFim().isAfter(h.getFechamento()))
                throw NegocioException.invalido("A disponibilidade do dia " + i.diaSemana() + " deve estar entre " + h.getAbertura() + " e " + h.getFechamento() + " (horário de funcionamento).");
            if (anterior != null && anterior.diaSemana() == i.diaSemana() && i.horaInicio().isBefore(anterior.horaFim()))
                throw NegocioException.invalido("Há janelas sobrepostas no dia " + i.diaSemana() + ".");
            anterior = i;
        }
        disponibilidades.apagarDoBarbeiro(barbeiroId);
        disponibilidades.flush();
        for (DisponibilidadeItem i : itens) {
            Disponibilidade d = new Disponibilidade();
            d.setBarbeiro(b); d.setDiaSemana(i.diaSemana()); d.setHoraInicio(i.horaInicio()); d.setHoraFim(i.horaFim());
            disponibilidades.save(d);
        }
        return disponibilidade(barbeiroId);
    }

    // ---------- Cálculo de horários livres (fonte única de verdade para consulta e para validação do agendamento) ----------
    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long barbeiroId, int duracaoMinutos, LocalDate data) {
        LocalDateTime agora = LocalDateTime.now(clock);
        if (data.isBefore(agora.toLocalDate())) return List.of();
        Optional<HorarioFuncionamento> func = funcionamento.findById(data.getDayOfWeek().getValue()).filter(HorarioFuncionamento::isAberto);
        if (func.isEmpty()) return List.of();
        List<Agendamento> ocupados = agendamentos.doBarbeiroNoPeriodo(barbeiroId, data.atStartOfDay().minusHours(12), data.plusDays(1).atStartOfDay())
                .stream().filter(a -> a.getStatus() == StatusAgendamento.AGENDADO).toList();
        int passo = props.intervaloSlotMinutos();
        TreeSet<LocalTime> livres = new TreeSet<>();
        for (Disponibilidade d : disponibilidades.findByBarbeiroIdAndDiaSemana(barbeiroId, data.getDayOfWeek().getValue())) {
            LocalTime ini = d.getHoraInicio().isBefore(func.get().getAbertura()) ? func.get().getAbertura() : d.getHoraInicio();
            LocalTime fim = d.getHoraFim().isAfter(func.get().getFechamento()) ? func.get().getFechamento() : d.getHoraFim();
            for (LocalTime t = ini; !t.plusMinutes(duracaoMinutos).isAfter(fim) && t.plusMinutes(duracaoMinutos).isAfter(t); t = t.plusMinutes(passo)) {
                LocalDateTime inicio = data.atTime(t);
                LocalDateTime termino = inicio.plusMinutes(duracaoMinutos);
                if (!inicio.isAfter(agora)) continue;
                boolean conflito = ocupados.stream().anyMatch(a -> a.getDataHora().isBefore(termino) && a.getDataHoraFim().isAfter(inicio));
                if (!conflito) livres.add(t);
                if (t.plusMinutes(passo).isBefore(t)) break;
            }
        }
        return new ArrayList<>(livres);
    }
}
