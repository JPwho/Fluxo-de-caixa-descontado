/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2024-05-20 19:07:54.

export class Acao {
    id?: number;
    nome?: string;
    precoAtual?: number;
    taxaCrescimentoEsperada?: number;
    taxaDesconto?: number;
    beta?: number;
    valorJusto?: number;
    classificacao?: number;
    anosProjecao?: number;
    fluxoDeCaixaLivre?: FluxoDeCaixaLivre[];

    constructor(data: Acao) {
        this.id = data.id;
        this.nome = data.nome;
        this.precoAtual = data.precoAtual;
        this.taxaCrescimentoEsperada = data.taxaCrescimentoEsperada;
        this.taxaDesconto = data.taxaDesconto;
        this.beta = data.beta;
        this.valorJusto = data.valorJusto;
        this.classificacao = data.classificacao;
        this.anosProjecao = data.anosProjecao;
        this.fluxoDeCaixaLivre = data.fluxoDeCaixaLivre;
    }
}

export class FluxoDeCaixaLivre {
    id?: number;
    data?: string;
    fluxoCaixaLivre?: number;

    constructor(data: FluxoDeCaixaLivre) {
        this.id = data.id;
        this.data = data.data;
        this.fluxoCaixaLivre = data.fluxoCaixaLivre;
    }
}

export class ResponseDTO<T> {
    code?: number;
    status?: Status;
    messagens?: string;
    data?: T;

    constructor(data: ResponseDTO<T>) {
        this.code = data.code;
        this.status = data.status;
        this.messagens = data.messagens;
        this.data = data.data;
    }
}

export interface HttpClient<O> {

    request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: O; }): RestResponse<R>;
}

export class AcaoResourceClient<O> {

    constructor(protected httpClient: HttpClient<O>) {
    }

    /**
     * HTTP POST /api/acoes
     * Java method: com.example.avaliacaodeacoes.resource.AcaoResource.salvarAcao
     */
    salvarAcao(acao: Acao, options?: O): RestResponse<ResponseDTO<Acao>> {
        return this.httpClient.request({ method: "POST", url: uriEncoding`api/acoes`, data: acao, options: options });
    }

    /**
     * HTTP POST /api/acoes/buscarTodasAcoes
     * Java method: com.example.avaliacaodeacoes.resource.AcaoResource.buscarTodasAcoes
     */
    buscarTodasAcoes(options?: O): RestResponse<ResponseDTO<any>> {
        return this.httpClient.request({ method: "POST", url: uriEncoding`api/acoes/buscarTodasAcoes`, options: options });
    }

    /**
     * HTTP GET /api/acoes/listar
     * Java method: com.example.avaliacaodeacoes.resource.AcaoResource.listar
     */
    listar(options?: O): RestResponse<ResponseDTO<Acao[]>> {
        return this.httpClient.request({ method: "GET", url: uriEncoding`api/acoes/listar`, options: options });
    }

    /**
     * HTTP POST /api/acoes/raspar
     * Java method: com.example.avaliacaodeacoes.resource.AcaoResource.rasparAcao
     */
    rasparAcao(options?: O): RestResponse<ResponseDTO<any>> {
        return this.httpClient.request({ method: "POST", url: uriEncoding`api/acoes/raspar`, options: options });
    }

    /**
     * HTTP GET /api/acoes/{id}/valor-justo
     * Java method: com.example.avaliacaodeacoes.resource.AcaoResource.obterValorJusto
     */
    obterValorJusto(id: number, options?: O): RestResponse<ResponseDTO<number>> {
        return this.httpClient.request({ method: "GET", url: uriEncoding`api/acoes/${id}/valor-justo`, options: options });
    }
}

export type RestResponse<R> = Promise<Axios.GenericAxiosResponse<R>>;

export enum Status {
    SUCESSO = "SUCESSO",
    ERRO = "ERRO",
    ALERTA = "ALERTA",
}

function uriEncoding(template: TemplateStringsArray, ...substitutions: any[]): string {
    let result = "";
    for (let i = 0; i < substitutions.length; i++) {
        result += template[i];
        result += encodeURIComponent(substitutions[i]);
    }
    result += template[template.length - 1];
    return result;
}


// Added by 'AxiosClientExtension' extension

import axios from "axios";
import * as Axios from "axios";

declare module "axios" {
    export interface GenericAxiosResponse<R> extends Axios.AxiosResponse {
        data: R;
    }
}

class AxiosHttpClient implements HttpClient<Axios.AxiosRequestConfig> {

    constructor(private axios: Axios.AxiosInstance) {
    }

    request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: Axios.AxiosRequestConfig; }): RestResponse<R> {
        function assign(target: any, source?: any) {
            if (source != undefined) {
                for (const key in source) {
                    if (source.hasOwnProperty(key)) {
                        target[key] = source[key];
                    }
                }
            }
            return target;
        }

        const config: Axios.AxiosRequestConfig = {};
        config.method = requestConfig.method as typeof config.method;  // `string` in axios 0.16.0, `Method` in axios 0.19.0
        config.url = requestConfig.url;
        config.params = requestConfig.queryParams;
        config.data = requestConfig.data;
        assign(config, requestConfig.options);
        const copyFn = requestConfig.copyFn;

        const axiosResponse = this.axios.request(config);
        return axiosResponse.then(axiosResponse => {
            if (copyFn && axiosResponse.data) {
                (axiosResponse as any).originalData = axiosResponse.data;
                axiosResponse.data = copyFn(axiosResponse.data);
            }
            return axiosResponse;
        });
    }
}

export class AxiosAcaoResourceClient extends AcaoResourceClient<Axios.AxiosRequestConfig> {

    constructor(baseURL: string, axiosInstance: Axios.AxiosInstance = axios.create()) {
        axiosInstance.defaults.baseURL = baseURL;
        super(new AxiosHttpClient(axiosInstance));
    }
}
