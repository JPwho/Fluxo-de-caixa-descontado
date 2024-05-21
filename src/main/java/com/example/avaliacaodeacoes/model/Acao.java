package com.example.avaliacaodeacoes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Acao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nome;
    private Double precoAtual;
    private Double taxaCrescimentoEsperada;
    private Double taxaDesconto;
//    private Double taxaLivreRisco;
    private Double beta;
    private Double valorJusto;
    private Double classificacao;
    private Double numeroAcoes;
    private int anosProjecao;
    private Double valorDCF;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "acao")
    private List<FluxoDeCaixaLivre> fluxoDeCaixaLivre;
}
