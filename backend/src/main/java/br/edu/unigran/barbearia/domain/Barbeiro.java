package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "barbeiro")
public class Barbeiro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(nullable = false, length = 120) private String especialidade;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { this.usuario = v; }
    public String getEspecialidade() { return especialidade; } public void setEspecialidade(String v) { this.especialidade = v; }
}
