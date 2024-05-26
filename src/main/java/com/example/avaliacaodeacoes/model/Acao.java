package com.example.avaliacaodeacoes.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Acao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @JsonProperty("nome")
    private String nome;
    private Double precoAtual;
    private Double taxaCrescimentoEsperada;
    private Double taxaDesconto;
    private Double beta;
    private Double valorJusto;
    private Double classificacao;
    @JsonProperty("numeroAcoes")
    private Double numeroAcoes;
    private int anosProjecao;
    private Double valorDCF;
    private LocalDate dataAtualizacao;
    @JsonProperty("tipo")
    private String tipo;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "acao")
    private List<FluxoDeCaixaLivre> fluxoDeCaixaLivre;
}
