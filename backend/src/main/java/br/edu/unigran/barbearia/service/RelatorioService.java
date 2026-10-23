package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/** Todos os números são calculados a partir dos agendamentos persistidos. */
@Service
public class RelatorioService {
    private final AgendamentoRepository agendamentos;
    private final AvaliacaoRepository avaliacoes;
    private final Clock clock;

    public RelatorioService(AgendamentoRepository agendamentos, AvaliacaoRepository avaliacoes, Clock clock) {
        this.agendamentos = agendamentos; this.avaliacoes = avaliacoes; this.clock = clock;
    }

    private static BigDecimal soma(Collection<Agendamento> l) { return l.stream().map(Agendamento::getPrecoCobrado).reduce(BigDecimal.ZERO, BigDecimal::add); }

    @Transactional(readOnly = true)
    public FinanceiroResponse financeiro(String periodo, LocalDate ref) {
        LocalDate dia = ref != null ? ref : LocalDate.now(clock);
        LocalDate ini, fim; // fim inclusivo
        switch (periodo == null ? "" : periodo.toUpperCase()) {
            case "DIA" -> { ini = dia; fim = dia; }
            case "SEMANA" -> { ini = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); fim = ini.plusDays(6); }
            case "MES" -> { ini = dia.withDayOfMonth(1); fim = dia.with(TemporalAdjusters.lastDayOfMonth()); }
            default -> throw NegocioException.invalido("Período inválido. Use DIA, SEMANA ou MES.");
        }
        List<Agendamento> todos = agendamentos.noPeriodo(ini.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        List<Agendamento> finalizados = todos.stream().filter(a -> a.getStatus() == StatusAgendamento.FINALIZADO).toList();
        List<Agendamento> pagos = finalizados.stream().filter(Agendamento::isPago).toList();
        List<Agendamento> pendentes = finalizados.stream().filter(a -> !a.isPago()).toList();
        long cancelados = todos.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();
        BigDecimal fat = soma(pagos);
        BigDecimal ticket = pagos.isEmpty() ? BigDecimal.ZERO : fat.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);

        List<FinanceiroBucket> serie = new ArrayList<>();
        if (periodo.equalsIgnoreCase("DIA")) {
            Map<Integer, List<Agendamento>> porHora = pagos.stream().collect(Collectors.groupingBy(a -> a.getDataHora().getHour()));
            int h0 = finalizados.stream().mapToInt(a -> a.getDataHora().getHour()).min().orElse(9);
            int h1 = finalizados.stream().mapToInt(a -> a.getDataHora().getHour()).max().orElse(18);
            for (int h = Math.min(h0, 9); h <= Math.max(h1, 18); h++) {
                List<Agendamento> l = porHora.getOrDefault(h, List.of());
                serie.add(new FinanceiroBucket(String.format("%02dh", h), soma(l), l.size()));
            }
        } else {
            Map<LocalDate, List<Agendamento>> porDia = pagos.stream().collect(Collectors.groupingBy(a -> a.getDataHora().toLocalDate()));
            for (LocalDate d = ini; !d.isAfter(fim); d = d.plusDays(1)) {
                List<Agendamento> l = porDia.getOrDefault(d, List.of());
                serie.add(new FinanceiroBucket(String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue()), soma(l), l.size()));
            }
        }
        return new FinanceiroResponse(periodo.toUpperCase(), ini, fim, fat, soma(pendentes), pagos.size(), cancelados, ticket, serie);
    }

    @Transactional(readOnly = true)
    public List<ServicoRanking> servicosMaisRealizados(LocalDate de, LocalDate ate, int limite) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ini = de != null ? de : hoje.minusDays(30), fim = ate != null ? ate : hoje;
        Map<Servico, List<Agendamento>> g = agendamentos.noPeriodo(ini.atStartOfDay(), fim.plusDays(1).atStartOfDay()).stream()
                .filter(a -> a.getStatus() == StatusAgendamento.FINALIZADO).collect(Collectors.groupingBy(Agendamento::getServico));
        return g.entrySet().stream()
                .map(e -> new ServicoRanking(e.getKey().getId(), e.getKey().getNome(), e.getValue().size(), soma(e.getValue())))
                .sorted(Comparator.comparingLong(ServicoRanking::quantidade).reversed().thenComparing(ServicoRanking::nome))
                .limit(Math.max(1, limite)).toList();
    }

    private ResumoPeriodo resumo(List<Agendamento> l) {
        long ag = l.stream().filter(a -> a.getStatus() == StatusAgendamento.AGENDADO).count();
        long fi = l.stream().filter(a -> a.getStatus() == StatusAgendamento.FINALIZADO).count();
        long ca = l.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();
        return new ResumoPeriodo(l.size(), ag, fi, ca, soma(l.stream().filter(Agendamento::isPago).toList()));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate iniMes = hoje.withDayOfMonth(1);
        List<Agendamento> mes = agendamentos.noPeriodo(iniMes.atStartOfDay(), iniMes.plusMonths(1).atStartOfDay());
        List<Agendamento> doDia = mes.stream().filter(a -> a.getDataHora().toLocalDate().equals(hoje)).toList();
        List<Agendamento> ult = agendamentos.noPeriodo(hoje.minusDays(6).atStartOfDay(), hoje.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> cont = ult.stream().collect(Collectors.groupingBy(a -> a.getDataHora().toLocalDate(), Collectors.counting()));
        List<DiaContagem> dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) { LocalDate d = hoje.minusDays(i); dias.add(new DiaContagem(d, cont.getOrDefault(d, 0L))); }
        return new DashboardResponse(resumo(doDia), resumo(mes), avaliacoes.mediaGeral(), servicosMaisRealizados(iniMes, hoje, 5), dias);
    }
}
