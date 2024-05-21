package com.example.avaliacaodeacoes.repository;

import com.example.avaliacaodeacoes.model.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcaoRepository extends JpaRepository<Acao, Long> {
    Acao findByNome(String nome);
}
