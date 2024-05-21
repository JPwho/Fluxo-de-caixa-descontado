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
        List<Acao> acaoList = acaoRepository.findAll();
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
        Double taxaCrescimento = 5.3800;
        Double taxaDesconto = 10.0;
        Double beta = extrairDoubleDoTexto(docResumo, "//*[@id='quote-summary']/div[2]/table/tbody/tr[2]/td[2]");
        int anosProjecao = 10; // Exemplo fixo, pode ser extraído também

        Document docFinanca = Jsoup.connect(url + "/cash-flow").get();
        if (Objects.nonNull(docFinanca)) {
            List<FluxoDeCaixaLivre> fluxoDeCaixaLivre = new ArrayList<>();

            //31/12/2020
            String buscarPrimeiroAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text();
            if (Objects.nonNull(buscarPrimeiroAno) && !buscarPrimeiroAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre primeiroAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarPrimeiroAno, acao);
                if (Objects.nonNull(primeiroAno)) {
                    primeiroAno.setFluxoCaixaLivre(extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[6]/span"));
                    primeiroAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text());
                } else {
                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[6]/span").text(),
                            extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[6]/span"), acao));
                }
            }

            //31/12/2021
            String buscarSegundoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text();
            if (Objects.nonNull(buscarSegundoAno) && !buscarSegundoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre segundoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarSegundoAno, acao);
                if (Objects.nonNull(segundoAno)) {
                    segundoAno.setFluxoCaixaLivre(extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[5]/span"));
                    segundoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text());
                } else {
                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[5]/span").text(),
                            extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[5]/span"), acao));
                }
            }

            //31/12/2022
            String buscarTerceiroAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text();

            if (Objects.nonNull(buscarTerceiroAno) && !buscarTerceiroAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre terceiroAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarTerceiroAno, acao);
                if (Objects.nonNull(terceiroAno)) {
                    terceiroAno.setFluxoCaixaLivre(extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[4]/span"));
                    terceiroAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text());
                } else {
                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[4]/span").text(),
                            extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[4]/span"), acao));
                }
            }

            //31/12/2023
            String buscarQuartoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text();
            if (Objects.nonNull(buscarQuartoAno) && !buscarQuartoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre quartoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarQuartoAno, acao);
                if (Objects.nonNull(quartoAno)) {
                    quartoAno.setFluxoCaixaLivre(extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[3]/span"));
                    quartoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text());
                } else {
                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[3]/span").text(),
                            extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[3]/span"), acao));
                }
            }

            //TTM
            String buscarQuintoAno = docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text();

            if (Objects.nonNull(buscarQuintoAno) && !buscarQuintoAno.equalsIgnoreCase("")) {
                FluxoDeCaixaLivre quintoAno = fluxoDeCaixaRepository.findByDataAndAcao(buscarQuintoAno, acao);
                if (Objects.nonNull(quintoAno)) {
                    quintoAno.setFluxoCaixaLivre(extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[2]/span"));
                    quintoAno.setData(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text());
                } else {
                    fluxoDeCaixaLivre.add(new FluxoDeCaixaLivre(docFinanca.selectXpath("//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[1]/div/div[2]/span").text(),
                            extrairDoubleDoTexto(docFinanca, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[2]/span"), acao));
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
                acaoRepository.save(acao);
            } else {
                Acao novaAcao = new Acao(acao.getId(), acao.getNome(), precoAtual, taxaCrescimento, taxaDesconto, beta, acao.getValorJusto(), acao.getClassificacao(), acao.getNumeroAcoes(), anosProjecao, acao.getValorDCF(), fluxoDeCaixaLivre);
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

    public List<Acao> listar() {
        return acaoRepository.findAll();
    }
}
