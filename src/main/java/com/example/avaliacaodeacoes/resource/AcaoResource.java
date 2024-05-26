package com.example.avaliacaodeacoes.resource;

import com.example.avaliacaodeacoes.dto.ResponseDTO;
import com.example.avaliacaodeacoes.model.Acao;
import com.example.avaliacaodeacoes.servico.AcaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acoes")
public class AcaoResource {

    @Autowired
    private AcaoService acaoService;

    @PostMapping
    public ResponseEntity<ResponseDTO<Acao>> salvarAcao(@RequestBody Acao acao) {
        return (ResponseEntity<ResponseDTO<Acao>>) ResponseEntity.status(HttpStatus.CREATED).body(acaoService.salvarAcao(acao));
    }

    @GetMapping("/{id}/valor-justo")
    public ResponseEntity<ResponseDTO<Double>> obterValorJusto(@PathVariable Long id) {
        return (ResponseEntity<ResponseDTO<Double>>) ResponseEntity.status(HttpStatus.CREATED).body(acaoService.calcularValorJusto(id));
    }

    @GetMapping("/valor-justo")
    public ResponseEntity<ResponseDTO<Double>> obterValorJustoPorAcao(@RequestParam (name = "nome") String nome) {
        return (ResponseEntity<ResponseDTO<Double>>) ResponseEntity.status(HttpStatus.ACCEPTED).body(acaoService.obterValorJustoPorAcao(nome));
    }

    @PostMapping("/raspar")
    public ResponseEntity<ResponseDTO> rasparAcao() {
        return (ResponseEntity<ResponseDTO>) ResponseEntity.status(HttpStatus.CREATED).body(acaoService.rasparESalvarAcao());
    }

    @PostMapping("/buscarTodasAcoes")
    public ResponseEntity<ResponseDTO> buscarTodasAcoes() {
        return (ResponseEntity<ResponseDTO>) ResponseEntity.status(HttpStatus.CREATED).body(acaoService.buscarTodasAcoes());
    }

    @GetMapping("listar")
    public ResponseEntity<ResponseDTO<List<Acao>>> listar () {
        return (ResponseEntity<ResponseDTO<List<Acao>>>) ResponseEntity.status(HttpStatus.ACCEPTED).body(acaoService.listar());
    }

    @PostMapping("/rasparAcaoPorPython")
    public ResponseEntity<ResponseDTO> rasparAcaoPorPython() {
        return (ResponseEntity<ResponseDTO>) ResponseEntity.status(HttpStatus.CREATED).body(acaoService.rasparAcaoPorPython());
    }
}