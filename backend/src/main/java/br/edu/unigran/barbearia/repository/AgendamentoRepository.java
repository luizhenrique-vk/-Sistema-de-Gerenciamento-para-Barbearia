package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("select a from Agendamento a where a.barbeiro.id = :barbeiroId and a.status = 'AGENDADO' and a.dataHora < :fim and a.dataHoraFim > :inicio")
    List<Agendamento> conflitosBarbeiro(@Param("barbeiroId") Long barbeiroId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select a from Agendamento a where a.cliente.id = :clienteId and a.status = 'AGENDADO' and a.dataHora < :fim and a.dataHoraFim > :inicio")
    List<Agendamento> conflitosCliente(@Param("clienteId") Long clienteId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select a from Agendamento a join fetch a.cliente join fetch a.barbeiro b join fetch b.usuario join fetch a.servico where a.cliente.id = :clienteId order by a.dataHora desc")
    List<Agendamento> doCliente(@Param("clienteId") Long clienteId);

    @Query("select a from Agendamento a join fetch a.cliente join fetch a.barbeiro b join fetch b.usuario join fetch a.servico where a.barbeiro.id = :barbeiroId and a.dataHora >= :inicio and a.dataHora < :fim order by a.dataHora")
    List<Agendamento> doBarbeiroNoPeriodo(@Param("barbeiroId") Long barbeiroId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query(value = "select a from Agendamento a join fetch a.cliente join fetch a.barbeiro b join fetch b.usuario join fetch a.servico where a.dataHora >= :inicio and a.dataHora < :fim and (:status is null or a.status = :status) and (:barbeiroId is null or a.barbeiro.id = :barbeiroId) order by a.dataHora desc",
           countQuery = "select count(a) from Agendamento a where a.dataHora >= :inicio and a.dataHora < :fim and (:status is null or a.status = :status) and (:barbeiroId is null or a.barbeiro.id = :barbeiroId)")
    Page<Agendamento> buscar(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("status") StatusAgendamento status, @Param("barbeiroId") Long barbeiroId, Pageable pageable);

    @Query("select a from Agendamento a join fetch a.servico where a.dataHora >= :inicio and a.dataHora < :fim")
    List<Agendamento> noPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select a from Agendamento a join fetch a.cliente join fetch a.servico where a.status = 'AGENDADO' and a.lembreteEnviado = false and a.dataHora > :agora and a.dataHora <= :limite")
    List<Agendamento> paraLembrete(@Param("agora") LocalDateTime agora, @Param("limite") LocalDateTime limite);

    boolean existsByServicoId(Long servicoId);
    boolean existsByBarbeiroIdAndStatus(Long barbeiroId, StatusAgendamento status);
    boolean existsByBarbeiroId(Long barbeiroId);
    boolean existsByClienteId(Long clienteId);
}
