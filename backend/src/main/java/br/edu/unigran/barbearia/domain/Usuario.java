package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, unique = true, length = 160) private String email;
    @Column(length = 20) private String telefone;
    @Column(nullable = false, length = 100) private String senha;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoUsuario tipo;
    @Column(nullable = false) private boolean ativo = true;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNome() { return nome; } public void setNome(String v) { this.nome = v; }
    public String getEmail() { return email; } public void setEmail(String v) { this.email = v; }
    public String getTelefone() { return telefone; } public void setTelefone(String v) { this.telefone = v; }
    public String getSenha() { return senha; } public void setSenha(String v) { this.senha = v; }
    public TipoUsuario getTipo() { return tipo; } public void setTipo(TipoUsuario v) { this.tipo = v; }
    public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { this.ativo = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
