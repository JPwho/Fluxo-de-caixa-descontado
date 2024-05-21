package com.example.avaliacaodeacoes.repository;

import com.example.avaliacaodeacoes.model.Acao;
import com.example.avaliacaodeacoes.model.FluxoDeCaixaLivre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FluxoDeCaixaRepository extends JpaRepository<FluxoDeCaixaLivre, Long> {
    FluxoDeCaixaLivre findByData(String data);

    FluxoDeCaixaLivre findByDataAndAcao(String ano, Acao acao);
}
