package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import br.edu.unigran.barbearia.security.UsuarioPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacoes;
    private final AgendamentoRepository agendamentos;
    public AvaliacaoService(AvaliacaoRepository avaliacoes, AgendamentoRepository agendamentos) { this.avaliacoes = avaliacoes; this.agendamentos = agendamentos; }

    private static AvaliacaoResponse resp(Avaliacao a) { return new AvaliacaoResponse(a.getId(), a.getAgendamento().getId(), a.getNota(), a.getComentario(), a.getCriadoEm()); }

    @Transactional
    public AvaliacaoResponse avaliar(UsuarioPrincipal quem, AvaliacaoRequest r) {
        Agendamento ag = agendamentos.findById(r.agendamentoId())
                .filter(a -> a.getCliente().getId().equals(quem.id()))
                .orElseThrow(() -> NegocioException.naoEncontrado("Atendimento não encontrado."));
        if (ag.getStatus() != StatusAgendamento.FINALIZADO) throw NegocioException.invalido("Só é possível avaliar atendimentos já realizados.");
        if (avaliacoes.existsByAgendamentoId(ag.getId())) throw NegocioException.conflito("Este atendimento já foi avaliado.");
        Avaliacao a = new Avaliacao();
        a.setAgendamento(ag); a.setNota(r.nota()); a.setComentario(r.comentario() == null || r.comentario().isBlank() ? null : r.comentario().trim());
        return resp(avaliacoes.save(a));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> doCliente(Long clienteId) { return avaliacoes.doCliente(clienteId).stream().map(AvaliacaoService::resp).toList(); }
}
