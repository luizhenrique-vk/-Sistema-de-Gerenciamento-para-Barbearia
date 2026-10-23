package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamento")
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "cliente_id", nullable = false) private Usuario cliente;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "barbeiro_id", nullable = false) private Barbeiro barbeiro;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "servico_id", nullable = false) private Servico servico;
    @Column(name = "data_hora", nullable = false) private LocalDateTime dataHora;
    @Column(name = "data_hora_fim", nullable = false) private LocalDateTime dataHoraFim;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusAgendamento status = StatusAgendamento.AGENDADO;
    @Column(length = 400) private String observacoes;
    @Column(name = "preco_cobrado", nullable = false, precision = 10, scale = 2) private BigDecimal precoCobrado;
    @Column(nullable = false) private boolean pago = false;
    @Enumerated(EnumType.STRING) @Column(name = "forma_pagamento", length = 20) private FormaPagamento formaPagamento;
    @Column(name = "data_pagamento") private LocalDateTime dataPagamento;
    @Column(name = "lembrete_enviado", nullable = false) private boolean lembreteEnviado = false;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public Usuario getCliente() { return cliente; } public void setCliente(Usuario v) { this.cliente = v; }
    public Barbeiro getBarbeiro() { return barbeiro; } public void setBarbeiro(Barbeiro v) { this.barbeiro = v; }
    public Servico getServico() { return servico; } public void setServico(Servico v) { this.servico = v; }
    public LocalDateTime getDataHora() { return dataHora; } public void setDataHora(LocalDateTime v) { this.dataHora = v; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; } public void setDataHoraFim(LocalDateTime v) { this.dataHoraFim = v; }
    public StatusAgendamento getStatus() { return status; } public void setStatus(StatusAgendamento v) { this.status = v; }
    public String getObservacoes() { return observacoes; } public void setObservacoes(String v) { this.observacoes = v; }
    public BigDecimal getPrecoCobrado() { return precoCobrado; } public void setPrecoCobrado(BigDecimal v) { this.precoCobrado = v; }
    public boolean isPago() { return pago; } public void setPago(boolean v) { this.pago = v; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; } public void setFormaPagamento(FormaPagamento v) { this.formaPagamento = v; }
    public boolean isLembreteEnviado() { return lembreteEnviado; } public void setLembreteEnviado(boolean v) { this.lembreteEnviado = v; }
    public LocalDateTime getDataPagamento() { return dataPagamento; } public void setDataPagamento(LocalDateTime v) { this.dataPagamento = v; }
}
