package com.example.avaliacaodeacoes.bo;

import com.example.avaliacaodeacoes.model.Acao;
import com.example.avaliacaodeacoes.model.FluxoDeCaixaLivre;
import com.example.avaliacaodeacoes.repository.AcaoRepository;
import com.example.avaliacaodeacoes.repository.FluxoDeCaixaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AcaoBO {
    @Autowired
    private FluxoDeCaixaRepository fluxoDeCaixaRepository;

    @Autowired
    private AcaoRepository acaoRepository;

    private final HttpClient httpClient;

    public AcaoBO() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public Acao salvarAcao(Acao acao) {
        return acaoRepository.save(acao);
    }

    public double calcularValorJusto(Long acaoId) {
        Acao acao = acaoRepository.findById(acaoId).orElseThrow(() -> new RuntimeException("Ação não encontrada"));
        return calcularDCF(
                acao.getFluxoDeCaixaLivre(),
                acao.getTaxaCrescimentoEsperada(),
                acao.getTaxaDesconto(),
                acao.getAnosProjecao()
        );
    }

    public void rasparESalvarAcao() throws IOException {
        List<Acao> acaoList = acaoRepository.findByDataNaoAtualizada(LocalDate.now());
        if (!acaoList.isEmpty()) {
            for (Acao acao : acaoList) {
                if (Objects.nonNull(acao)) {
                    if (Objects.nonNull(acao.getNome())) {
                        String url = "https://br.financas.yahoo.com/quote/" + acao.getNome();
                        rasparDadosAcao(url, acao);
                    }
                }
            }
        }
    }

    //@Scheduled(cron = "0 0 6 1 */1 *") // Executa às 6 horas do primeiro dia do mês, a cada 30 dias
    public void buscarTodasAcoes() throws IOException {
        String apiUrl = "https://brapi.dev/api/available?search&token=5jWvREzveT63cYGHXV8wq4";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            //logger.error("Error updating the tickers", e.getMessage());
        }

        //salvar as ações
        parseStocks(response.body());

        return;
    }

    public List<Acao> parseStocks(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Acao> acaoList = new ArrayList<>();

        // Parse o JSON de resposta
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

        // Acesse a lista de ações (stocks) dentro do objeto raiz
        JsonNode stocksNode = jsonNode.get("stocks");

        // Verifique se a lista de ações (stocks) existe
        if (stocksNode != null && stocksNode.isArray()) {
            // Para cada ação na lista, crie uma entidade de ações (stocks) e armazene-a
            for (final JsonNode stockNode : stocksNode) {
                String nome = stockNode.asText();

                if (Objects.nonNull(nome)) {
                    Acao acao = acaoRepository.findByNome(nome + ".SA");

                    if (Objects.isNull(acao)) {
                        Acao novaAcao = new Acao();
                        novaAcao.setNome(nome + ".SA");
                        acaoList.add(novaAcao);
                    }
                }
            }
            acaoRepository.saveAll(acaoList);
        }
        return acaoList;
    }

    @Transactional
    public void rasparDadosAcao(String url, Acao acao) throws IOException {
        Document docResumo = Jsoup.connect(url).get();

        Double precoAtual = extrairDoubleDoTexto(docResumo, "/html/body/div[1]/div/div/div[1]/div/div[2]/div/div/div[6]/div/div/div/div[3]/div[1]/div/fin-streamer[1]");
        Double taxaCrescimento = 0.03D;
        Double taxaDesconto = 0.10D;
        int anosProjecao = acao.getFluxoDeCaixaLivre().size(); // Exemplo fixo, pode ser extraído também
        Double beta = extrairDoubleDoTexto(docResumo, "//*[@id='quote-summary']/div[2]/table/tbody/tr[2]/td[2]");

        if (Objects.nonNull(acao)) {
            if (Objects.isNull(acao.getTaxaCrescimentoEsperada())) {
                acao.setTaxaCrescimentoEsperada(taxaCrescimento);
            }

            if (Objects.isNull(acao.getTaxaDesconto())) {
                acao.setTaxaDesconto(taxaDesconto);
            }
        }

        Document docFinanca = Jsoup.connect(url + "/cash-flow").get();
        if (Objects.nonNull(docFinanca)) {
            List<FluxoDeCaixaLivre> fluxoDeCaixaLivre = new ArrayList<>();

            //31/12/2020
            String buscarPrimeiroAno = docFinanca.selectXpath("//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text();

            if (Objects.nonNull(buscarPrimeiroAno) && buscarPrimeiroAno.equalsIgnoreCase("")) {
                buscarPrimeiroAno = docFinanca.selectXpath("/html/body/div[1]/div/div/div[1]/div/div[3]/div[1]/div/div[2]/div/div/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text();
            }

            if (Objects.nonNull(buscarPrimeiroAno) && !buscarPrimeiroAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre primeiroAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarPrimeiroAno, acao);
                if (Objects.nonNull(primeiroAno)) {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[6]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[6]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[6]/span");
                    }

                    primeiroAno.setFluxoCaixaLivre(valorUm);
                    primeiroAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text());
                } else {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[6]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[6]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[6]/span");
                    }

                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text(),
                            valorUm, acao));
                }
            }

            //31/12/2021
            String buscarSegundoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text();

            if (Objects.nonNull(buscarSegundoAno) && buscarSegundoAno.equalsIgnoreCase("")) {
                buscarSegundoAno = docFinanca.selectXpath("/html/body/div[1]/div/div/div[1]/div/div[3]/div[1]/div/div[2]/div/div/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text();
            }

            if (Objects.nonNull(buscarSegundoAno) && !buscarSegundoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre segundoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarSegundoAno, acao);
                if (Objects.nonNull(segundoAno)) {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[5]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[5]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[5]/span");
                    }

                    segundoAno.setFluxoCaixaLivre(valorUm);
                    segundoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text());
                } else {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[5]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[5]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[5]/span");
                    }

                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text(),
                            valorUm, acao));
                }
            }

            //31/12/2022
            String buscarTerceiroAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text();

            if (Objects.nonNull(buscarTerceiroAno) && buscarTerceiroAno.equalsIgnoreCase("")) {
                buscarTerceiroAno = docFinanca.selectXpath("/html/body/div[1]/div/div/div[1]/div/div[3]/div[1]/div/div[2]/div/div/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text();
            }

            if (Objects.nonNull(buscarTerceiroAno) && !buscarTerceiroAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre terceiroAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarTerceiroAno, acao);
                if (Objects.nonNull(terceiroAno)) {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[4]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[4]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[4]/span");
                    }

                    terceiroAno.setFluxoCaixaLivre(valorUm);
                    terceiroAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text());
                } else {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[4]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[4]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[4]/span");
                    }

                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text(),
                            valorUm, acao));
                }
            }

            //31/12/2023
            String buscarQuartoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text();

            if (Objects.nonNull(buscarQuartoAno) && buscarQuartoAno.equalsIgnoreCase("")) {
                buscarQuartoAno = docFinanca.selectXpath("/html/body/div[1]/div/div/div[1]/div/div[3]/div[1]/div/div[2]/div/div/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text();
            }

            if (Objects.nonNull(buscarQuartoAno) && !buscarQuartoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre quartoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarQuartoAno, acao);
                if (Objects.nonNull(quartoAno)) {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[3]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[3]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[3]/span");
                    }

                    quartoAno.setFluxoCaixaLivre(valorUm);
                    quartoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text());
                } else {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[3]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[3]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[3]/span");
                    }

                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text(),
                            valorUm, acao));
                }
            }

            //TTM
            String buscarQuintoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text();

            if (Objects.nonNull(buscarQuintoAno) && buscarQuintoAno.equalsIgnoreCase("")) {
                buscarQuintoAno = docFinanca.selectXpath("/html/body/div[1]/div/div/div[1]/div/div[3]/div[1]/div/div[2]/div/div/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text();
            }

            if (Objects.nonNull(buscarQuintoAno) && !buscarQuintoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre quintoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarQuintoAno, acao);
                if (Objects.nonNull(quintoAno)) {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[2]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[2]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[2]/span");
                    }

                    quintoAno.setFluxoCaixaLivre(valorUm);
                    quintoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text());
                } else {
                    double valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[2]/span");

                    //Ações com mais de 5 anos de fluxo de caixa
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[2]/span");
                    }

                    //Ações internacionais
                    if (valorUm == 0D || Objects.isNull(valorUm)) {
                        valorUm = extrairDoubleDoTexto(docFinanca, "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[2]/span");
                    }

                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text(),
                            valorUm, acao));
                }
            }

            if (fluxoDeCaixaLivre.isEmpty()) {
                if (!acao.getFluxoDeCaixaLivre().isEmpty()) {
                    fluxoDeCaixaLivre.addAll(acao.getFluxoDeCaixaLivre());
                }
            }

            Document docBalanco = Jsoup.connect(url + "/balance-sheet").get();
            if (Objects.nonNull(docBalanco)) {
                String buscarQuantidadeDeAcoes = docBalanco.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div[1]/div[1]/div[2]/span").text();
                if (Objects.nonNull(buscarQuantidadeDeAcoes)) {
                    Double valor = extrairDoubleDoTexto(docBalanco, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div[1]/div[1]/div[2]/span");

                    if (!valor.isInfinite() && !valor.isNaN()) {
                        acao.setNumeroAcoes(valor);
                    }
                }
            }

            System.out.println("----------------------------");
            System.out.println(acao.getNome());
            System.out.println("----------------------------");

            if (Objects.nonNull(acao)) {
                Double valorJustoAcao = calcularValorJustoAcao(acao.getFluxoDeCaixaLivre(), acao.getTaxaCrescimentoEsperada(), acao.getTaxaDesconto(), acao.getAnosProjecao(), acao.getNumeroAcoes(), acao);
                if (!valorJustoAcao.isNaN() && !valorJustoAcao.isInfinite()) {
                    acao.setValorJusto(valorJustoAcao);
                }
                acao.setTaxaDesconto(taxaDesconto);
                acao.setTaxaCrescimentoEsperada(taxaCrescimento);
                acao.setDataAtualizacao(LocalDate.now());
                acaoRepository.save(acao);
            } else {
                Acao novaAcao = new Acao(acao.getId(), acao.getNome(), precoAtual, taxaCrescimento, taxaDesconto, beta, acao.getValorJusto(), acao.getClassificacao(), acao.getNumeroAcoes(), anosProjecao, acao.getValorDCF(), LocalDate.now(), fluxoDeCaixaLivre);
                acaoRepository.save(novaAcao);
            }
        }
    }

    private static double extrairDoubleDoTexto(Document doc, String seletorXpath) {
        Element elemento = doc.selectXpath(seletorXpath).first();
        if (elemento != null) {
            String texto = elemento.text().replace("%", "");
            texto = texto.replace(".", "").replace(",", ".");
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("Erro ao converter o valor para número: " + e.getMessage());
            }
        }
        return 0;
    }

    public static double calcularValorJustoAcao(List<FluxoDeCaixaLivre> fluxosDeCaixaLivre, double taxaCrescimento, double taxaDesconto, int anos, Double numeroAcoes, Acao acao) {
        Double valorDCF = calcularDCF(fluxosDeCaixaLivre, taxaCrescimento, taxaDesconto, anos);
        Double valor = valorDCF / numeroAcoes;
        acao.setValorDCF(valorDCF);
        return Objects.nonNull(valor) && !valor.isInfinite() && !valor.isNaN() ? valor : 0D;
    }

    public static double calcularDCF(List<FluxoDeCaixaLivre> fluxosDeCaixaLivre, double taxaCrescimento, double taxaDesconto, int anos) {
        double valorDCF = 0.0;
        //Transformando em porcentagem
        taxaCrescimento = (taxaCrescimento/100);
        taxaDesconto = (taxaDesconto/100);
        // Descontar os fluxos de caixa livres conhecidos
        for (int i = 0; i < fluxosDeCaixaLivre.size(); i++) {
            double fluxoCaixaLivre = fluxosDeCaixaLivre.get(i).getFluxoCaixaLivre();
            valorDCF += fluxoCaixaLivre / Math.pow((1 + taxaDesconto), i + 1);
        }

        // Obter o último fluxo de caixa conhecido
        double fluxoCaixaLivreAtual = fluxosDeCaixaLivre.get(fluxosDeCaixaLivre.size() - 1).getFluxoCaixaLivre();

        // Projetar e descontar fluxos de caixa futuros
        for (int i = fluxosDeCaixaLivre.size(); i < anos; i++) {
            fluxoCaixaLivreAtual *= (1 + taxaCrescimento);
            valorDCF += fluxoCaixaLivreAtual / Math.pow((1 + taxaDesconto), i + 1);
        }

        // Calcular valor terminal
        double valorTerminal = fluxoCaixaLivreAtual * (1 + taxaCrescimento) / (taxaDesconto - taxaCrescimento);
        valorDCF += valorTerminal / Math.pow((1 + taxaDesconto), anos);

        return valorDCF;
    }

//    @Transactional
//    public void rasparDadosAcao(String url, Acao acao) throws IOException {
//        Document docResumo = Jsoup.connect(url).get();
//        Double precoAtual = extrairDoubleDoTexto(docResumo, "/html/body/div[1]/div/div/div[1]/div/div[2]/div/div/div[6]/div/div/div/div[3]/div[1]/div/fin-streamer[1]");
//        Double taxaCrescimento = 0.03D;
//        Double taxaDesconto = 0.10D;
//        Double beta = extrairDoubleDoTexto(docResumo, "//*[@id='quote-summary']/div[2]/table/tbody/tr[2]/td[2]");
//
//        ajustarTaxas(acao, taxaCrescimento, taxaDesconto);
//
//        Document docFinanca = Jsoup.connect(url + "/cash-flow").get();
//        List<FluxoDeCaixaLivre> fluxoDeCaixaLivre = extrairFluxoDeCaixaLivre(docFinanca, acao);
//
//        Document docBalanco = Jsoup.connect(url + "/balance-sheet").get();
//        ajustarQuantidadeAcoes(docBalanco, acao);
//
//        if (Objects.nonNull(acao)) {
//            Double valorJustoAcao = calcularValorJustoAcao(acao.getFluxoDeCaixaLivre(), acao.getTaxaCrescimentoEsperada(), acao.getTaxaDesconto(), acao.getAnosProjecao(), acao.getNumeroAcoes(), acao);
//            if (!valorJustoAcao.isNaN() && !valorJustoAcao.isInfinite()) {
//                acao.setValorJusto(valorJustoAcao);
//            }
//            acao.setTaxaDesconto(taxaDesconto);
//            acao.setTaxaCrescimentoEsperada(taxaCrescimento);
//            acao.setDataAtualizacao(LocalDate.now());
//            acaoRepository.save(acao);
//        } else {
//            Acao novaAcao = new Acao(acao.getId(), acao.getNome(), precoAtual, taxaCrescimento, taxaDesconto, beta, acao.getValorJusto(), acao.getClassificacao(), acao.getNumeroAcoes(), acao.getAnosProjecao(), acao.getValorDCF(), LocalDate.now(), fluxoDeCaixaLivre);
//            acaoRepository.save(novaAcao);
//        }
//    }
//
//    private void ajustarTaxas(Acao acao, Double taxaCrescimento, Double taxaDesconto) {
//        if (Objects.nonNull(acao)) {
//            if (Objects.isNull(acao.getTaxaCrescimentoEsperada())) {
//                acao.setTaxaCrescimentoEsperada(taxaCrescimento);
//            }
//
//            if (Objects.isNull(acao.getTaxaDesconto())) {
//                acao.setTaxaDesconto(taxaDesconto);
//            }
//        }
//    }
//
//    private List<FluxoDeCaixaLivre> extrairFluxoDeCaixaLivre(Document docFinanca, Acao acao) {
//        List<FluxoDeCaixaLivre> fluxoDeCaixaLivre = new ArrayList<>();
//        String[] xpathsAno = {
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[%d]/span",
//                "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[1]/div/div[%d]/span"
//        };
//
//        for (int i = 0; i < 5; i++) {
//            String dataAno = null;
//            for (String xpath : xpathsAno) {
//                dataAno = docFinanca.selectXpath(String.format(xpath, 6 - i)).text();
//                if (Objects.nonNull(dataAno) && !dataAno.equalsIgnoreCase("")) {
//                    break;
//                }
//            }
//
//            if (Objects.nonNull(dataAno) && !dataAno.equalsIgnoreCase("")) {
//                FluxoDeCaixaLivre fluxo = fluxoDeCaixaRepository.findByDataAndAcao(dataAno, acao);
//                double valor = extrairValorFluxoCaixa(docFinanca, i);
//                if (Objects.isNull(fluxo)) {
//                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(dataAno, valor, acao));
//                } else {
//                    fluxo.setFluxoCaixaLivre(valor);
//                    fluxo.setData(dataAno);
//                }
//            }
//        }
//
////        if (fluxoDeCaixaLivre.isEmpty() && !acao.getFluxoDeCaixaLivre().isEmpty()) {
//        fluxoDeCaixaLivre.addAll(acao.getFluxoDeCaixaLivre());
//        fluxoDeCaixaRepository.saveAll(fluxoDeCaixaLivre);
////        }
//
//        return fluxoDeCaixaLivre;
//    }
//
//    private double extrairValorFluxoCaixa(Document docFinanca, int anoIndex) {
//        String[] xpaths = {
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[%d]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[%d]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[%d]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div/div[1]/div[%d]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[13]/div[1]/div[%d]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[%d]/span",
//        };
//        for (String xpath : xpaths) {
//            double valor = extrairDoubleDoTexto(docFinanca, String.format(xpath, 6 - anoIndex));
//            if (valor != 0D) {
//                return valor;
//            }
//        }
//        return 0D;
//    }
//
//    private void ajustarQuantidadeAcoes(Document docBalanco, Acao acao) {
//        String[] xpathsQuantidadeDeAcoes = {
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div[1]/div[1]/div[2]/span",
//                "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[2]/div[2]/div[3]/div[1]/div[2]/span"
//                // Adicione mais xpaths aqui conforme necessário
//        };
//
//        Double valor = null;
//        for (String xpath : xpathsQuantidadeDeAcoes) {
//            String buscarQuantidadeDeAcoes = docBalanco.selectXpath(xpath).text();
//            if (Objects.nonNull(buscarQuantidadeDeAcoes) && !buscarQuantidadeDeAcoes.equalsIgnoreCase("")) {
//                valor = extrairDoubleDoTexto(docBalanco, xpath);
//                if (valor != 0D) {
//                    break;
//                }
//            }
//        }
//
//        if (Objects.nonNull(valor) && !valor.isInfinite() && !valor.isNaN()) {
//            acao.setNumeroAcoes(valor);
//        }
//    }
//
//    private double extrairDoubleDoTexto(Document doc, String xpath) {
//        String texto = doc.selectXpath(xpath).text().replace(",", "");
//        try {
//            return Double.parseDouble(texto);
//        } catch (NumberFormatException e) {
//            return 0D;
//        }
//    }
//
//    public static double calcularValorJustoAcao(List<FluxoDeCaixaLivre> fluxosDeCaixaLivre,
//                                                double taxaCrescimento,
//                                                double taxaDesconto,
//                                                int anos,
//                                                double numeroAcoes,
//                                                Acao acao) {
//
//        // Calcula o valor presente dos fluxos de caixa descontados
//        double valorDCF = calcularDCF(fluxosDeCaixaLivre, taxaCrescimento, taxaDesconto, anos);
//
//        // Calcula o valor justo por ação
//        double valorJustoAcao = valorDCF / numeroAcoes;
//
//        // Define o valor presente dos fluxos de caixa na ação
//        if (acao != null) {
//            acao.setValorDCF(valorDCF);
//        }
//
//        // Retorna o valor justo por ação, evitando valores infinitos ou NaN
//        return Objects.nonNull(valorJustoAcao) && !Double.isInfinite(valorJustoAcao) && !Double.isNaN(valorJustoAcao) ? valorJustoAcao : 0.0;
//    }
//
//    public static double calcularDCF(List<FluxoDeCaixaLivre> fluxosDeCaixaLivre,
//                                     double taxaCrescimento,
//                                     double taxaDesconto,
//                                     int anos) {
//
//        double valorDCF = 0.0;
//        double fatorDesconto = 1.0 + taxaDesconto;
//
//        if (!fluxosDeCaixaLivre.isEmpty()) {
//            // Desconta os fluxos de caixa livres conhecidos
//            for (int i = 0; i < fluxosDeCaixaLivre.size(); i++) {
//                double fluxoCaixaLivre = fluxosDeCaixaLivre.get(i).getFluxoCaixaLivre();
//                double valorPresente = fluxoCaixaLivre / Math.pow(fatorDesconto, i + 1);
//                valorDCF += valorPresente;
//                System.out.printf("Ano %d: Fluxo Caixa Livre: %.2f, Valor Presente: %.2f\n", i + 1, fluxoCaixaLivre, valorPresente);
//            }
//
//            // Projeta e desconta fluxos de caixa futuros
//            double ultimoFluxoCaixaLivre = fluxosDeCaixaLivre.get(fluxosDeCaixaLivre.size() - 1).getFluxoCaixaLivre();
//            for (int i = fluxosDeCaixaLivre.size(); i < anos; i++) {
//                ultimoFluxoCaixaLivre *= (1 + taxaCrescimento);
//                double valorPresente = ultimoFluxoCaixaLivre / Math.pow(fatorDesconto, i + 1);
//                valorDCF += valorPresente;
//                System.out.printf("Ano %d: Fluxo Caixa Livre Projeção: %.2f, Valor Presente: %.2f\n", i + 1, ultimoFluxoCaixaLivre, valorPresente);
//            }
//
//            // Calcula o valor terminal usando o modelo de crescimento perpétuo
//            double valorTerminal = ultimoFluxoCaixaLivre * (1 + taxaCrescimento) / (taxaDesconto - taxaCrescimento);
//            double valorPresenteTerminal = valorTerminal / Math.pow(fatorDesconto, anos);
//            System.out.printf("Valor Presente Terminal : %.2f\n", valorPresenteTerminal);
//            System.out.printf("Valor Terminal: %.2f\n", valorTerminal);
//
//            // Adiciona o valor terminal ao valor presente dos fluxos de caixa
//            valorDCF += valorPresenteTerminal;
//        }
//
//        return valorDCF;
//    }

    public List<Acao> listar() {
        return acaoRepository.findAll();
    }

    public Double obterValorJustoPorAcao(String nome) {
        String url = "https://br.financas.yahoo.com/quote/" + nome;
        Acao acao = acaoRepository.findByNome(nome);
        try {
            rasparDadosAcao(url, acao);
        } catch (IOException e) {
            System.out.println("erro: " + e.getMessage());
        }

        return acaoRepository.findByNome(nome).getValorJusto();
    }
}
