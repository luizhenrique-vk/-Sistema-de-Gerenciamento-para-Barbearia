package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "horario_funcionamento")
public class HorarioFuncionamento {
    @Id @Column(name = "dia_semana") private int diaSemana;
    @Column(nullable = false) private boolean aberto;
    @Column(nullable = false) private LocalTime abertura;
    @Column(nullable = false) private LocalTime fechamento;

    public int getDiaSemana() { return diaSemana; } public void setDiaSemana(int v) { this.diaSemana = v; }
    public boolean isAberto() { return aberto; } public void setAberto(boolean v) { this.aberto = v; }
    public LocalTime getAbertura() { return abertura; } public void setAbertura(LocalTime v) { this.abertura = v; }
    public LocalTime getFechamento() { return fechamento; } public void setFechamento(LocalTime v) { this.fechamento = v; }
}
