package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.Barbeiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    Optional<Barbeiro> findByUsuarioId(Long usuarioId);
    List<Barbeiro> findByUsuarioAtivoTrueOrderByUsuarioNomeAsc();
    List<Barbeiro> findAllByOrderByUsuarioNomeAsc();

    /** Bloqueio pessimista: serializa a criação de agendamentos por barbeiro e evita conflitos concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    Optional<Barbeiro> findByIdForUpdate(@Param("id") Long id);
}
