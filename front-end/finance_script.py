import yfinance as yf
import pandas as pd

# Lista de tickers das ações da B3
tickers = ["CXSE3.SA"]

# Lista para armazenar dados
data_list = []

for ticker in tickers:
    stock = yf.Ticker(ticker)
    
    # Obter dados específicos
    try:
        nome = stock.info.get('shortName', 'N/A')
        print(nome)
        cashflow = stock.cashflow
        shares_outstanding = stock.info.get('sharesOutstanding', 'N/A')

        # Extrair fluxos de caixa livres (Free Cash Flow)
        free_cash_flow = cashflow.loc['Free Cash Flow'] if 'Free Cash Flow' in cashflow.index else 'N/A'

        # Verificar se é uma ação
        if stock.info.get('quoteType', 'N/A') == 'EQUITY':
            paper_type = 'Ação'
        else:
            paper_type = 'N/A'
        
        # Adicionar dados para cada ano disponível

        for year, fcf in free_cash_flow.items():
            data_list.append({
                'acao': ticker,
                # 'Nome': nome,
                'ano': year.year,
                'fluxo_caixa_livre': fcf,
                'qtd_acaos': shares_outstanding,
                'tipo': paper_type
            })
            
    except KeyError as e:
        print(f"Error fetching data for {ticker}: {e}")

# Converter a lista de dados em um DataFrame
df = pd.DataFrame(data_list)

# Salvar o DataFrame em um arquivo Excel
output_file = 'b3_stock_free_cash_flow.xlsx'
df.to_excel(output_file, index=False, engine='openpyxl')

print(f"Dados exportados para {output_file}")
