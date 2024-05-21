import { Component, OnInit } from '@angular/core';
import { AcaoService } from '../../service/AcaoService';
import { Acao } from '../../service/service';
import { MessageService } from 'primeng/api';
import { LazyLoadEvent } from 'primeng/api';

@Component({
  selector: 'app-listar-acao',
  templateUrl: './listar-acao.component.html',
  styleUrls: ['./listar-acao.component.css']
})
export class ListarAcaoComponent implements OnInit {
  acao: Acao[] = [];
  selectedAcoes: Acao[] = [];
  loading: boolean = true;
  searchValue: string = '';

  constructor(
    private acaoService: AcaoService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.acaoService.listar()
      .then(data => {
        this.acao = data.data!;
        this.loading = false;
      })
      .catch(error => {
        console.error(error);
        this.loading = false;
      });
  }

  clear(dt: any) {
    if (dt) {
      dt.filterGlobal('', 'contains');
    }
  }

  show() {
    this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Message Content' });
  }

}
