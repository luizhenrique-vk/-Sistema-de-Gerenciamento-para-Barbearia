package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByTipo(TipoUsuario tipo);
    long countByTipoAndAtivoTrue(TipoUsuario tipo);

    @Query("select u from Usuario u where (:tipo is null or u.tipo = :tipo) and (:q is null or lower(u.nome) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')))")
    Page<Usuario> buscar(@Param("tipo") TipoUsuario tipo, @Param("q") String q, Pageable pageable);
}
