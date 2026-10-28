import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { LicitacoesFase3ModalComponent } from './licitacoes-fase3-modal.component';
import { LicitacaoService } from '../../services/licitacao.service';
import { ConvenioCockpit } from '../../model/convenio-fase.model';
import { Licitacao } from '../../model/licitacao.model';

describe('LicitacoesFase3ModalComponent', () => {
  let component: LicitacoesFase3ModalComponent;
  let fixture: ComponentFixture<LicitacoesFase3ModalComponent>;
  let service: jasmine.SpyObj<LicitacaoService>;

  const mockConvenio: ConvenioCockpit = {
    id: 'conv-test-123',
    numeroSiconv: '914250/2023',
    ano: 2023,
    objeto: 'Construção de Creche',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'MEC / FNDE',
    valorTotal: 2500000,
    valorRepasse: 2250000,
    valorContrapartida: 250000,
    percentualExecucao: 0,
    saldoContaOp006: 0,
    diasParaVencimento: 180,
    dataFimVigencia: '2025-12-31',
    faseAtualNumero: 3,
    statusGeral: 'EM_DIA',
    fases: []
  };

  const mockLicitacao: Licitacao = {
    id: 'lic-1',
    tenantId: 't-1',
    convenioId: 'conv-test-123',
    prefeituraId: 'pref-1',
    numeroLicitacao: 'Concorrência 002/2026',
    modalidade: 'CONCORRENCIA_ELETRONICA',
    objeto: 'Obras civis',
    valorEstimado: 2000000,
    situacao: 'EDITAL_PUBLICADO',
    statusVrpl: 'NAO_ENVIADO',
    statusAio: 'NAO_EMITIDO',
    podeCriarMedicao: false
  };

  beforeEach(async () => {
    const serviceSpy = jasmine.createSpyObj('LicitacaoService', [
      'listar',
      'cadastrar',
      'homologar',
      'submeterVrpl',
      'registrarResultadoVrpl',
      'emitirAio'
    ]);
    serviceSpy.listar.and.returnValue(of([mockLicitacao]));

    await TestBed.configureTestingModule({
      imports: [LicitacoesFase3ModalComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LicitacaoService, useValue: serviceSpy }
      ]
    }).compileComponents();

    service = TestBed.inject(LicitacaoService) as jasmine.SpyObj<LicitacaoService>;
    fixture = TestBed.createComponent(LicitacoesFase3ModalComponent);
    component = fixture.componentInstance;
    component.convenio = mockConvenio;
    fixture.detectChanges();
  });

  it('deve carregar a lista 1:N ao abrir o modal da Fase 3', () => {
    expect(component.licitacoes().length).toBe(1);
    expect(component.licitacoes()[0].numeroLicitacao).toBe('Concorrência 002/2026');
    expect(service.listar).toHaveBeenCalledWith('conv-test-123');
  });

  it('deve exibir trava de medições enquanto AIO não estiver emitida', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('medições bloqueadas até a AIO emitida');
  });
});
