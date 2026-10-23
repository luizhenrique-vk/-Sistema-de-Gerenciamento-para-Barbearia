package br.edu.unigran.barbearia.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servico")
public class Servico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String nome;
    @Column(length = 400) private String descricao;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal preco;
    /** Duração estimada em minutos. */
    @Column(nullable = false) private int duracao;
    @Column(nullable = false) private boolean ativo = true;

    public Long getId() { return id; }
    public String getNome() { return nome; } public void setNome(String v) { this.nome = v; }
    public String getDescricao() { return descricao; } public void setDescricao(String v) { this.descricao = v; }
    public BigDecimal getPreco() { return preco; } public void setPreco(BigDecimal v) { this.preco = v; }
    public int getDuracao() { return duracao; } public void setDuracao(int v) { this.duracao = v; }
    public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { this.ativo = v; }
}
