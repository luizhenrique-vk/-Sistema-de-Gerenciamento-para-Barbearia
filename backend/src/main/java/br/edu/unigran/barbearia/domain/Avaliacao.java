package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacao")
public class Avaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "agendamento_id", nullable = false, unique = true) private Agendamento agendamento;
    @Column(nullable = false) private int nota;
    @Column(length = 500) private String comentario;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public Agendamento getAgendamento() { return agendamento; } public void setAgendamento(Agendamento v) { this.agendamento = v; }
    public int getNota() { return nota; } public void setNota(int v) { this.nota = v; }
    public String getComentario() { return comentario; } public void setComentario(String v) { this.comentario = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
