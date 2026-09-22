import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuditChecklistComponent } from './audit-checklist.component';
import { RevisaoStateService } from '../../services/revisao-state.service';

describe('AuditChecklistComponent', () => {
  let component: AuditChecklistComponent;
  let fixture: ComponentFixture<AuditChecklistComponent>;
  let state: RevisaoStateService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditChecklistComponent],
      providers: [
        RevisaoStateService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditChecklistComponent);
    component = fixture.componentInstance;
    state = TestBed.inject(RevisaoStateService);
    fixture.detectChanges();
  });

  it('deve instanciar com sucesso', () => {
    expect(component).toBeTruthy();
  });

  it('deve identificar documento pronto para aprovação quando consistente', () => {
    state.atualizarCampo('valorBruto', 10000);
    state.adicionarRetencao('INSS');
    state.atualizarRetencao(0, { tipoTributo: 'INSS', valorRetido: 1000 });
    state.atualizarCampo('valorLiquido', 9000);
    state.atualizarCampo('cnpjCredor', '12.345.678/0001-95');
    state.atualizarCampo('razaoSocialCredor', 'Construtora Teste LTDA');

    fixture.detectChanges();

    expect(component.estaProntoParaAprovacao()).toBeTrue();
    expect(component.credorValido()).toBeTrue();
  });

  it('deve sinalizar divergência quando o cálculo matemático não bater', () => {
    state.atualizarCampo('valorBruto', 10000);
    state.adicionarRetencao('INSS');
    state.atualizarRetencao(0, { tipoTributo: 'INSS', valorRetido: 1000 });
    state.atualizarCampo('valorLiquido', 8000); // Esperado 9000 -> divergência de 1000
    state.atualizarCampo('cnpjCredor', '12.345.678/0001-95');
    state.atualizarCampo('razaoSocialCredor', 'Construtora Teste LTDA');

    fixture.detectChanges();

    expect(component.estaProntoParaAprovacao()).toBeFalse();
    expect(state.possuiDivergencia()).toBeTrue();
  });

  it('deve sinalizar erro e permitir auto-correção quando o CNPJ do credor tiver dígitos verificadores inválidos', () => {
    state.atualizarCampo('valorBruto', 10000);
    state.atualizarCampo('valorLiquido', 10000);
    state.atualizarCampo('cnpjCredor', '12.345.678/0001-90'); // DV inválido (esperado: 95)
    state.atualizarCampo('razaoSocialCredor', 'Construtora Teste LTDA');

    fixture.detectChanges();

    expect(component.cnpjInvalido()).toBeTrue();
    expect(component.credorValido()).toBeFalse();
    expect(component.estaProntoParaAprovacao()).toBeFalse();
    expect(component.sugestaoCnpj()).toBe('12.345.678/0001-95');

    // Aplicar autocorreção de 1 clique
    component.aplicarSugestaoCnpj();
    fixture.detectChanges();

    expect(component.cnpjInvalido()).toBeFalse();
    expect(component.credorValido()).toBeTrue();
    expect(component.estaProntoParaAprovacao()).toBeTrue();
  });
});
