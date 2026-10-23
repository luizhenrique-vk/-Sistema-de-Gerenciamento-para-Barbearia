package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final UsuarioRepository usuarios;
    private final AgendamentoRepository agendamentos;
    private final AvaliacaoRepository avaliacoes;
    private final DisponibilidadeRepository disponibilidades;
    private final PasswordEncoder encoder;

    public BarbeiroService(BarbeiroRepository barbeiros, UsuarioRepository usuarios, AgendamentoRepository agendamentos,
                           AvaliacaoRepository avaliacoes, DisponibilidadeRepository disponibilidades, PasswordEncoder encoder) {
        this.barbeiros = barbeiros; this.usuarios = usuarios; this.agendamentos = agendamentos;
        this.avaliacoes = avaliacoes; this.disponibilidades = disponibilidades; this.encoder = encoder;
    }

    private BarbeiroResponse resp(Barbeiro b) {
        Usuario u = b.getUsuario();
        return new BarbeiroResponse(b.getId(), u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), b.getEspecialidade(), u.isAtivo(), avaliacoes.mediaDoBarbeiro(b.getId()));
    }

    @Transactional(readOnly = true)
    public List<BarbeiroResponse> listar(boolean somenteAtivos) {
        return (somenteAtivos ? barbeiros.findByUsuarioAtivoTrueOrderByUsuarioNomeAsc() : barbeiros.findAllByOrderByUsuarioNomeAsc()).stream().map(this::resp).toList();
    }

    @Transactional(readOnly = true)
    public BarbeiroResponse doUsuario(Long usuarioId) { return resp(porUsuario(usuarioId)); }

    public Barbeiro porUsuario(Long usuarioId) {
        return barbeiros.findByUsuarioId(usuarioId).orElseThrow(() -> NegocioException.naoEncontrado("Perfil de barbeiro não encontrado."));
    }

    @Transactional
    public BarbeiroResponse criar(BarbeiroRequest r) {
        if (r.senha() == null || r.senha().isBlank()) throw NegocioException.invalido("Informe a senha de acesso do barbeiro.");
        if (usuarios.existsByEmailIgnoreCase(r.email().trim())) throw NegocioException.conflito("Já existe uma conta com este e-mail.");
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(r.email().trim().toLowerCase());
        u.setTelefone(r.telefone() == null || r.telefone().isBlank() ? null : r.telefone().trim());
        u.setSenha(encoder.encode(r.senha())); u.setTipo(TipoUsuario.BARBEIRO); u.setAtivo(r.ativo() == null || r.ativo());
        usuarios.save(u);
        Barbeiro b = new Barbeiro(); b.setUsuario(u); b.setEspecialidade(r.especialidade().trim());
        return resp(barbeiros.save(b));
    }

    @Transactional
    public BarbeiroResponse atualizar(Long id, BarbeiroRequest r) {
        Barbeiro b = barbeiros.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Barbeiro não encontrado."));
        Usuario u = b.getUsuario();
        String email = r.email().trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(u.getId())).ifPresent(o -> { throw NegocioException.conflito("Este e-mail já está em uso."); });
        boolean ativo = r.ativo() == null || r.ativo();
        if (!ativo && agendamentos.existsByBarbeiroIdAndStatus(id, StatusAgendamento.AGENDADO))
            throw NegocioException.conflito("O barbeiro possui agendamentos futuros. Cancele ou conclua-os antes de desativá-lo.");
        u.setNome(r.nome().trim()); u.setEmail(email); u.setAtivo(ativo);
        u.setTelefone(r.telefone() == null || r.telefone().isBlank() ? null : r.telefone().trim());
        if (r.senha() != null && !r.senha().isBlank()) u.setSenha(encoder.encode(r.senha()));
        b.setEspecialidade(r.especialidade().trim());
        return resp(b);
    }

    @Transactional
    public void excluir(Long id) {
        Barbeiro b = barbeiros.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Barbeiro não encontrado."));
        if (agendamentos.existsByBarbeiroId(id)) throw NegocioException.conflito("Este barbeiro possui histórico de atendimentos. Desative-o em vez de excluir.");
        disponibilidades.apagarDoBarbeiro(id);
        barbeiros.delete(b);
        usuarios.delete(b.getUsuario());
    }

    /** O barbeiro só altera a própria especialidade (nome/telefone/senha pelo perfil comum). */
    @Transactional
    public BarbeiroResponse atualizarEspecialidade(Long usuarioId, PerfilBarbeiroRequest r) {
        Barbeiro b = porUsuario(usuarioId);
        b.setEspecialidade(r.especialidade().trim());
        return resp(b);
    }
}
