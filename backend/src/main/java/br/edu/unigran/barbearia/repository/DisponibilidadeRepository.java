package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.Disponibilidade;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByBarbeiroIdOrderByDiaSemanaAscHoraInicioAsc(Long barbeiroId);
    List<Disponibilidade> findByBarbeiroIdAndDiaSemana(Long barbeiroId, int diaSemana);
    @Modifying @Query("delete from Disponibilidade d where d.barbeiro.id = :barbeiroId")
    void apagarDoBarbeiro(@Param("barbeiroId") Long barbeiroId);
}
