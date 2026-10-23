package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.Avaliacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    boolean existsByAgendamentoId(Long agendamentoId);
    @Query("select av from Avaliacao av where av.agendamento.cliente.id = :clienteId")
    List<Avaliacao> doCliente(@Param("clienteId") Long clienteId);
    @Query("select avg(av.nota) from Avaliacao av where av.agendamento.barbeiro.id = :barbeiroId")
    Double mediaDoBarbeiro(@Param("barbeiroId") Long barbeiroId);
    @Query("select avg(av.nota) from Avaliacao av")
    Double mediaGeral();
}
