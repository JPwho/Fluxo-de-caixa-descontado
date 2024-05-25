package com.example.avaliacaodeacoes.servico.servicoImpl;

import com.example.avaliacaodeacoes.bo.AcaoBO;
import com.example.avaliacaodeacoes.dto.ResponseDTO;
import com.example.avaliacaodeacoes.model.Acao;
import com.example.avaliacaodeacoes.servico.AcaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class acaoServiceImpl implements AcaoService {

    @Autowired
    private AcaoBO acaoBO;

    @Override
    public ResponseDTO<Acao> salvarAcao(Acao acao) {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
            responseDTO.setData(acaoBO.salvarAcao(acao));
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public ResponseDTO<Double> calcularValorJusto(Long id) {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
            responseDTO.setData(acaoBO.calcularValorJusto(id));
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public ResponseDTO rasparESalvarAcao() {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            acaoBO.rasparESalvarAcao();
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public ResponseDTO<List<Acao>> listar() {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            responseDTO.setData(acaoBO.listar());
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public ResponseDTO<Double> obterValorJustoPorAcao(String nome) {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            responseDTO.setData(acaoBO.obterValorJustoPorAcao(nome));
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public ResponseDTO buscarTodasAcoes() {
        ResponseDTO responseDTO = new ResponseDTO<>();
        try {
            acaoBO.buscarTodasAcoes();
            responseDTO.setCode(200);
            responseDTO.setStatus(responseDTO.getStatus().SUCESSO);
            responseDTO.setMessagens("Sucesso!");
        } catch (Exception e) {
            responseDTO.setCode(400);
            responseDTO.setStatus(responseDTO.getStatus().ERRO);
            responseDTO.setMessagens(e.getMessage());
        }
        return responseDTO;
    }
}
