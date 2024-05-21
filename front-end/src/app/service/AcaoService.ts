import { Injectable } from '@angular/core';
import { AxiosAcaoResourceClient, ResponseDTO, Acao } from './service';

@Injectable({
  providedIn: 'root'
})
export class AcaoService {

  constructor(private axiosClient: AxiosAcaoResourceClient) { }

  salvarAcao(acao: Acao) {
    return this.axiosClient.salvarAcao(acao)
      .then(response => response.data)
      .catch(error => {
        throw new Error(error.response.data);
      });
  }

  buscarTodasAcoes() {
    return this.axiosClient.buscarTodasAcoes()
      .then(response => response.data)
      .catch(error => {
        throw new Error(error.response.data);
      });
  }

  listar() {
    return this.axiosClient.listar().then(response => response.data)
    .catch(error => {
        throw new Error(error.response.data);
    });
  }

}