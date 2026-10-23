package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.NotificacaoResponse;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificacaoService {
    private final NotificacaoRepository repo;
    public NotificacaoService(NotificacaoRepository repo) { this.repo = repo; }

    @Transactional
    public void notificar(Usuario destino, String mensagem) { repo.save(new Notificacao(destino, mensagem)); }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(Long usuarioId) {
        return repo.findTop30ByUsuarioIdOrderByCriadaEmDesc(usuarioId).stream()
                .map(n -> new NotificacaoResponse(n.getId(), n.getMensagem(), n.isLida(), n.getCriadaEm())).toList();
    }

    @Transactional(readOnly = true)
    public long naoLidas(Long usuarioId) { return repo.countByUsuarioIdAndLidaFalse(usuarioId); }

    @Transactional
    public void marcarLida(Long usuarioId, Long id) {
        Notificacao n = repo.findById(id).filter(x -> x.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> NegocioException.naoEncontrado("Notificação não encontrada."));
        n.setLida(true);
    }

    @Transactional
    public void marcarTodasLidas(Long usuarioId) {
        repo.findTop30ByUsuarioIdOrderByCriadaEmDesc(usuarioId).forEach(n -> n.setLida(true));
    }
}
