package com.example.avaliacaodeacoes.repository;

import com.example.avaliacaodeacoes.model.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AcaoRepository extends JpaRepository<Acao, Long> {
    Acao findByNome(String nome);

    @Query(value = "SELECT * FROM acao WHERE (data_atualizacao <> :dataAtualizacao OR data_atualizacao is null)", nativeQuery = true)
    List<Acao> findByDataNaoAtualizada(@Param("dataAtualizacao") LocalDate dataAtualizacao);
}
