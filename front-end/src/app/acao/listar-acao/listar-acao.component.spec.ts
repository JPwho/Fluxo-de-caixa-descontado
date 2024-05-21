import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListarAcaoComponent } from './listar-acao.component';

describe('ListarAcaoComponent', () => {
  let component: ListarAcaoComponent;
  let fixture: ComponentFixture<ListarAcaoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ListarAcaoComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ListarAcaoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
