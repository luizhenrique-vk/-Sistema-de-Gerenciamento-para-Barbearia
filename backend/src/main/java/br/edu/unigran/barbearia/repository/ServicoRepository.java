package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByAtivoTrueOrderByNomeAsc();
    List<Servico> findAllByOrderByNomeAsc();
}
