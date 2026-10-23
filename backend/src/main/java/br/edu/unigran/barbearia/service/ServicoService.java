package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.Servico;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ServicoService {
    private final ServicoRepository servicos;
    private final AgendamentoRepository agendamentos;
    public ServicoService(ServicoRepository servicos, AgendamentoRepository agendamentos) { this.servicos = servicos; this.agendamentos = agendamentos; }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(boolean somenteAtivos) {
        return (somenteAtivos ? servicos.findByAtivoTrueOrderByNomeAsc() : servicos.findAllByOrderByNomeAsc()).stream().map(ServicoResponse::de).toList();
    }

    @Transactional
    public ServicoResponse salvar(Long id, ServicoRequest r) {
        Servico s = id == null ? new Servico() : servicos.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Serviço não encontrado."));
        s.setNome(r.nome().trim()); s.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        s.setPreco(r.preco()); s.setDuracao(r.duracao()); s.setAtivo(r.ativo() == null || r.ativo());
        return ServicoResponse.de(servicos.save(s));
    }

    @Transactional
    public void excluir(Long id) {
        Servico s = servicos.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Serviço não encontrado."));
        if (agendamentos.existsByServicoId(id)) throw NegocioException.conflito("Este serviço já possui agendamentos. Desative-o em vez de excluir.");
        servicos.delete(s);
    }
}
