package com.example.avaliacaodeacoes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class FluxoDeCaixaLivre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("data")
    private String data;
    @JsonProperty("fluxoCaixaLivre")
    private Double fluxoCaixaLivre;

    @ManyToOne
    @JoinColumn(name = "id_acao")
    @JsonIgnore
    private Acao acao;

    public FluxoDeCaixaLivre(String data, Double fluxoCaixaLivre, Acao acao) {
        this.data = data;
        this.fluxoCaixaLivre = fluxoCaixaLivre;
        this.acao = acao;
    }
}
