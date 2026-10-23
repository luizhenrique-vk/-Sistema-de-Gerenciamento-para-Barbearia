package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findTop30ByUsuarioIdOrderByCriadaEmDesc(Long usuarioId);
    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
