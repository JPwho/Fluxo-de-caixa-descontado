package com.example.avaliacaodeacoes.servico;

import com.example.avaliacaodeacoes.dto.ResponseDTO;
import com.example.avaliacaodeacoes.model.Acao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AcaoService {
    ResponseDTO buscarTodasAcoes();
    ResponseDTO<Acao> salvarAcao(Acao acao);
    ResponseDTO<Double> calcularValorJusto(Long id);
    ResponseDTO rasparESalvarAcao();
    ResponseDTO<List<Acao>> listar();
    ResponseDTO<Double> obterValorJustoPorAcao(String nome);

    ResponseDTO rasparAcaoPorPython();
}