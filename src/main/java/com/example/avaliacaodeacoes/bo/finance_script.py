import yfinance as yf
import pandas as pd
import json
import sys

def obter_dados_acao(ticker):
    data_list = []

    try:
        stock = yf.Ticker(ticker)
        nome = stock.info.get('shortName', 'N/A')
        cashflow = stock.cashflow
        shares_outstanding = stock.info.get('sharesOutstanding', 'N/A')
        paper_type = 'Ação' if stock.info.get('quoteType', 'N/A') == 'EQUITY' else 'N/A'

        if isinstance(cashflow, pd.DataFrame) and 'Free Cash Flow' in cashflow.index:
            for year in cashflow.columns:
                fcf = cashflow.loc['Free Cash Flow', year]
                data_list.append({
                    'acao': ticker,
                    'nome': nome,
                    'ano': year.year,
                    'fluxo_caixa_livre': fcf,
                    'qtd_acaos': shares_outstanding,
                    'tipo': paper_type
                })
    except Exception as e:
        print(f"Erro ao buscar dados para {ticker}: {e}", file=sys.stderr)

    return data_list

def main(ticker):
    dados_acao = obter_dados_acao(ticker)

    # Converter os dados em DataFrame
    df = pd.DataFrame(dados_acao)

    # Salvar o DataFrame em um arquivo Excel
    output_file = f'{ticker}_stock_free_cash_flow.xlsx'
    df.to_excel(output_file, index=False, engine='openpyxl')

    # Converter os dados em formato JSON
    response_json = json.dumps(dados_acao, indent=4, ensure_ascii=False)

    print(response_json)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Uso: python finance_script.py <ticker>")
        sys.exit(1)

    ticker = sys.argv[1]
    main(ticker)
