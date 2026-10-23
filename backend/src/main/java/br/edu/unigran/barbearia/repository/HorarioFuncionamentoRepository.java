package br.edu.unigran.barbearia.repository;

import br.edu.unigran.barbearia.domain.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Integer> {
    List<HorarioFuncionamento> findAllByOrderByDiaSemanaAsc();
}
