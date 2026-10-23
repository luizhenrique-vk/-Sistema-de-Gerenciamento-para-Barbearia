package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

/** Janela de atendimento de um barbeiro em um dia da semana (1 = segunda ... 7 = domingo). */
@Entity
@Table(name = "disponibilidade")
public class Disponibilidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "barbeiro_id", nullable = false) private Barbeiro barbeiro;
    @Column(name = "dia_semana", nullable = false) private int diaSemana;
    @Column(name = "hora_inicio", nullable = false) private LocalTime horaInicio;
    @Column(name = "hora_fim", nullable = false) private LocalTime horaFim;

    public Long getId() { return id; }
    public Barbeiro getBarbeiro() { return barbeiro; } public void setBarbeiro(Barbeiro v) { this.barbeiro = v; }
    public int getDiaSemana() { return diaSemana; } public void setDiaSemana(int v) { this.diaSemana = v; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime v) { this.horaInicio = v; }
    public LocalTime getHoraFim() { return horaFim; } public void setHoraFim(LocalTime v) { this.horaFim = v; }
}
