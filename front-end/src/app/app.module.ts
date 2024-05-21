import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ListarAcaoComponent } from './acao/listar-acao/listar-acao.component';
import { AcaoService } from './service/AcaoService';
import { AxiosAcaoResourceClient } from './service/service';
import { ResourceClientFactory } from './service/resource-client-factory';

// PrimeNG imports
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { DropdownModule } from 'primeng/dropdown';
import { SliderModule } from 'primeng/slider';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { CheckboxModule } from 'primeng/checkbox';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { HomeComponent } from './home/home.component';
import { ToastModule } from 'primeng/toast';
import { RippleModule } from 'primeng/ripple';
import { MessageService } from 'primeng/api';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SidebarModule } from 'primeng/sidebar'; // Importe o SidebarModule

@NgModule({
  declarations: [
    AppComponent,
    ListarAcaoComponent,
    HomeComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    // PrimeNG modules
    TableModule,
    ButtonModule,
    InputTextModule,
    MultiSelectModule,
    DropdownModule,
    SliderModule,
    ProgressBarModule,
    TagModule,
    CheckboxModule,
    CardModule,
    ToolbarModule,
    ToastModule,
    RippleModule,
    BrowserAnimationsModule,
    SidebarModule
  ],
  providers: [
    { provide: AxiosAcaoResourceClient, useValue: ResourceClientFactory.acaoResourceClient() },
    AcaoService,
    MessageService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
