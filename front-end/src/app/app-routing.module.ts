import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListarAcaoComponent } from './acao/listar-acao/listar-acao.component';
import { HomeComponent } from './home/home.component';

const routes: Routes = [
  { path: 'acao/listar', component:ListarAcaoComponent },
  { path: '', component: HomeComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
