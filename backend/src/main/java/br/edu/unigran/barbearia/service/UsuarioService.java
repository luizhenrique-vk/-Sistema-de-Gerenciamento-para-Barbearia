package br.edu.unigran.barbearia.service;

import br.edu.unigran.barbearia.domain.*;
import br.edu.unigran.barbearia.dto.Dtos.*;
import br.edu.unigran.barbearia.exception.NegocioException;
import br.edu.unigran.barbearia.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, BarbeiroRepository barbeiros, AgendamentoRepository agendamentos, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.barbeiros = barbeiros; this.agendamentos = agendamentos; this.encoder = encoder;
    }

    private static String limpar(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    public UsuarioResponse resposta(Usuario u) {
        UsuarioResponse r = UsuarioResponse.de(u);
        if (u.getTipo() == TipoUsuario.BARBEIRO) return barbeiros.findByUsuarioId(u.getId()).map(b -> r.comBarbeiro(b.getId())).orElse(r);
        return r;
    }

    /** Cadastro público: sempre cria um CLIENTE, independentemente do que for enviado. */
    @Transactional
    public Usuario cadastrarCliente(CadastroRequest req) {
        if (usuarios.existsByEmailIgnoreCase(req.email().trim())) throw NegocioException.conflito("Já existe uma conta com este e-mail.");
        Usuario u = new Usuario();
        u.setNome(req.nome().trim()); u.setEmail(req.email().trim().toLowerCase()); u.setTelefone(limpar(req.telefone()));
        u.setSenha(encoder.encode(req.senha())); u.setTipo(TipoUsuario.CLIENTE);
        return usuarios.save(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long id) { return resposta(buscar(id)); }

    /** Atualiza apenas os dados do próprio usuário autenticado (id vem do token, nunca do corpo). */
    @Transactional
    public UsuarioResponse atualizarPerfil(Long id, PerfilRequest req) {
        Usuario u = buscar(id);
        String email = req.email().trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(id)).ifPresent(o -> { throw NegocioException.conflito("Este e-mail já está em uso."); });
        if (req.novaSenha() != null && !req.novaSenha().isBlank()) {
            if (req.senhaAtual() == null || !encoder.matches(req.senhaAtual(), u.getSenha())) throw NegocioException.invalido("A senha atual está incorreta.");
            u.setSenha(encoder.encode(req.novaSenha()));
        }
        u.setNome(req.nome().trim()); u.setEmail(email); u.setTelefone(limpar(req.telefone()));
        return resposta(u);
    }

    // ---------- Administração ----------
    @Transactional(readOnly = true)
    public Pagina<UsuarioResponse> listar(TipoUsuario tipo, String q, int pagina, int tamanho) {
        Pageable p = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 50), Sort.by("nome"));
        return Pagina.de(usuarios.buscar(tipo, limpar(q), p).map(this::resposta));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioAdminRequest req) {
        if (req.tipo() == TipoUsuario.BARBEIRO) throw NegocioException.invalido("Barbeiros devem ser cadastrados em Barbeiros (exigem especialidade).");
        if (req.senha() == null || req.senha().isBlank()) throw NegocioException.invalido("Informe a senha do novo usuário.");
        if (usuarios.existsByEmailIgnoreCase(req.email().trim())) throw NegocioException.conflito("Já existe uma conta com este e-mail.");
        Usuario u = new Usuario();
        u.setNome(req.nome().trim()); u.setEmail(req.email().trim().toLowerCase()); u.setTelefone(limpar(req.telefone()));
        u.setSenha(encoder.encode(req.senha())); u.setTipo(req.tipo()); u.setAtivo(req.ativo() == null || req.ativo());
        return resposta(usuarios.save(u));
    }

    @Transactional
    public UsuarioResponse atualizar(Long adminId, Long id, UsuarioAdminRequest req) {
        Usuario u = buscar(id);
        if (u.getTipo() == TipoUsuario.BARBEIRO || req.tipo() == TipoUsuario.BARBEIRO) {
            if (u.getTipo() != req.tipo()) throw NegocioException.invalido("O tipo de um barbeiro não pode ser alterado por aqui.");
        }
        String email = req.email().trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(id)).ifPresent(o -> { throw NegocioException.conflito("Este e-mail já está em uso."); });
        boolean ativo = req.ativo() == null || req.ativo();
        boolean perdeAdmin = u.getTipo() == TipoUsuario.ADMIN && (req.tipo() != TipoUsuario.ADMIN || !ativo);
        if (perdeAdmin) {
            if (id.equals(adminId)) throw NegocioException.invalido("Você não pode remover seu próprio acesso administrativo.");
            if (usuarios.countByTipoAndAtivoTrue(TipoUsuario.ADMIN) <= 1) throw NegocioException.invalido("É necessário manter ao menos um administrador ativo.");
        }
        u.setNome(req.nome().trim()); u.setEmail(email); u.setTelefone(limpar(req.telefone())); u.setTipo(req.tipo()); u.setAtivo(ativo);
        if (req.senha() != null && !req.senha().isBlank()) u.setSenha(encoder.encode(req.senha()));
        return resposta(u);
    }

    /** Exclusão preserva integridade: usuários com histórico são apenas desativados. */
    @Transactional
    public void excluir(Long adminId, Long id) {
        Usuario u = buscar(id);
        if (id.equals(adminId)) throw NegocioException.invalido("Você não pode excluir a própria conta.");
        if (u.getTipo() == TipoUsuario.ADMIN && usuarios.countByTipoAndAtivoTrue(TipoUsuario.ADMIN) <= 1) throw NegocioException.invalido("É necessário manter ao menos um administrador ativo.");
        boolean temHistorico = agendamentos.existsByClienteId(id) || barbeiros.findByUsuarioId(id).map(b -> agendamentos.existsByBarbeiroId(b.getId())).orElse(false) || u.getTipo() == TipoUsuario.BARBEIRO;
        if (temHistorico) throw NegocioException.conflito("Este usuário possui vínculos e histórico. Desative-o em vez de excluir.");
        usuarios.delete(u);
    }

    public Usuario buscar(Long id) { return usuarios.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado.")); }
}
