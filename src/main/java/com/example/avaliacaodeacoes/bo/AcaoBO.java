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
import java.util.*;

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
//        List<Acao> acaoList = acaoRepository.findByDataNaoAtualizada(LocalDate.now());
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

        Double taxaCrescimento = 0.03D;
        Double taxaDesconto = 0.10D;
        int anosProjecao = 5; // Exemplo fixo, pode ser extraído também
        Double beta = extrairDoubleDoTexto(docResumo, "//*[@id='quote-summary']/div[2]/table/tbody/tr[2]/td[2]");
        Double precoAtual = extrairDoubleDoTexto(docResumo, "/html/body/div[1]/div/div/div[1]/div/div[2]/div/div/div[6]/div/div/div/div[3]/div[1]/div/fin-streamer[1]");

        Document docBalanco = Jsoup.connect(url + "/balance-sheet").get();
        Double numeroAcoes = extrairDoubleDoTexto(docBalanco, "//*[@id='Col1-1-Financials-Proxy']/section/div[3]/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div[1]/div[1]/div[2]/span");

        System.out.println("----------------------------");
        System.out.println(acao.getNome());
        System.out.println("----------------------------");

        Document docFinanca = Jsoup.connect(url + "/cash-flow").get();
        if (Objects.nonNull(docFinanca)) {
            acao.setBeta(beta);
            acao.setAnosProjecao(anosProjecao);
            acao.setPrecoAtual(precoAtual);
            acao.setTaxaCrescimentoEsperada(taxaCrescimento);
            acao.setTaxaDesconto(taxaDesconto);
            acao.setNumeroAcoes(numeroAcoes);

            //Cria uma nova ação que não existe no banco de dados
            if (Objects.isNull(acao.getId())) {
                acao = acaoRepository.save(acao);
            }

            // Mapeamento de anos para possíveis xPaths para os valores de fluxo de caixa
            Map<String, String[]> xPathMapAnos = new HashMap<>();
            xPathMapAnos.put("31/03/2020", new String[]{
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[6]/span", //5 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div[3]/div[1]/div[6]/span", //5 anos com fluxo de caixa reduzido
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[6]/span", //6 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[6]/span", //5 anos internacional
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[6]/span"  //6 anos com fluxo de caixa reduzido
            });
            xPathMapAnos.put("31/12/2021", new String[]{
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[5]/span", //5 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div[3]/div[1]/div[5]/span", //5 anos com fluxo de caixa reduzido
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[5]/span", //6 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[5]/span", //5 anos internacional
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[5]/span"  //6 anos com fluxo de caixa reduzido
            });

            xPathMapAnos.put("31/12/2022", new String[]{
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[4]/span", //5 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div[3]/div[1]/div[4]/span", //5 anos com fluxo de caixa reduzido
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[4]/span", //6 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[4]/span", //5 anos internacional
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[4]/span"  //6 anos com fluxo de caixa reduzido
            });

            xPathMapAnos.put("31/03/2023", new String[]{
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[3]/span", //5 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div[3]/div[1]/div[3]/span", //5 anos com fluxo de caixa reduzido
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[3]/span", //6 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[3]/span", //5 anos internacional
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[3]/span"  //6 anos com fluxo de caixa reduzido
            });

            xPathMapAnos.put("TTM", new String[]{
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[3]/div[1]/div[2]/span", //5 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[5]/div[2]/div[3]/div[1]/div[2]/span", //5 anos com fluxo de caixa reduzido
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[3]/div[1]/div[2]/span", //6 anos
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[7]/div[2]/div[2]/div[1]/div[2]/span", //5 anos internacional
                    "//*[@id=\"Col1-1-Financials-Proxy\"]/section/div[3]/div[1]/div/div[2]/div[6]/div[2]/div[2]/div[1]/div[2]/span"  //6 anos com fluxo de caixa reduzido
            });

            // Iterar sobre os anos disponíveis no mapeamento
            for (Map.Entry<String, String[]> entry : xPathMapAnos.entrySet()) {
                String ano = entry.getKey();
                String[] xPaths = entry.getValue();

                // Tenta encontrar o valor de fluxo de caixa para o ano atual
                Double valorFluxoCaixa = 0D;
                for (String xPath : xPaths) {

                    Double valor = extrairDoubleDoTexto(docFinanca, xPath);
                    if (Objects.nonNull(valor) && valor != 0D) {
                        valorFluxoCaixa = valor;
                        break;  // Sai do loop se encontrar um valor
                    }
                }

                // Busca ou cria um novo FluxoDeCaixaLivre para o ano corrente
                FluxoDeCaixaLivre fluxoDeCaixaLivre = fluxoDeCaixaRepository.findByDataAndAcao(ano, acao);
                if (fluxoDeCaixaLivre == null) {
                    fluxoDeCaixaLivre = new FluxoDeCaixaLivre();
                    fluxoDeCaixaLivre.setData(ano);
                    fluxoDeCaixaLivre.setAcao(acao);
                }

                // Converte o valor de fluxo de caixa para o tipo adequado e configura no objeto
                fluxoDeCaixaLivre.setFluxoCaixaLivre(valorFluxoCaixa);

                if (Objects.isNull(acao.getFluxoDeCaixaLivre())) {
                    acao.setFluxoDeCaixaLivre(new ArrayList<>());
                    acao.getFluxoDeCaixaLivre().add(fluxoDeCaixaLivre);
                }

                // Salva o fluxo de caixa no repositório
                fluxoDeCaixaRepository.save(fluxoDeCaixaLivre);
            }

            // Calcular valor do preço justo
            acao.setValorJusto(calcularValorJustoAcao(acao.getFluxoDeCaixaLivre(), acao.getTaxaCrescimentoEsperada(), acao.getTaxaDesconto(), acao.getAnosProjecao(), acao.getNumeroAcoes(), acao));

            // Salva a ação atualizada no repositório
            acaoRepository.save(acao);
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

    @Transactional
    public Double obterValorJustoPorAcao(String nome) {
        String url = "https://br.financas.yahoo.com/quote/" + nome;
        Acao acao = acaoRepository.findByNome(nome);

        try {
            if (Objects.isNull(acao)) {
                acao = new Acao();
                acao.setNome(nome);
            }
            rasparDadosAcao(url, acao);
        } catch (IOException e) {
            System.out.println("erro: " + e.getMessage());
        }

        return acaoRepository.findByNome(nome).getValorJusto();
    }
}
