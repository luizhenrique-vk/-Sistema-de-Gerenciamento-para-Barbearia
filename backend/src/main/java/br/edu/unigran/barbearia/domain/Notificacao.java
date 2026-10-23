package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Notificação interna exibida no sistema (RF: notificações/lembretes de agendamento). */
@Entity
@Table(name = "notificacao")
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Column(nullable = false, length = 300) private String mensagem;
    @Column(nullable = false) private boolean lida = false;
    @Column(name = "criada_em", nullable = false, updatable = false) private LocalDateTime criadaEm = LocalDateTime.now();

    public Notificacao() {}
    public Notificacao(Usuario usuario, String mensagem) { this.usuario = usuario; this.mensagem = mensagem; }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; } public void setLida(boolean v) { this.lida = v; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
