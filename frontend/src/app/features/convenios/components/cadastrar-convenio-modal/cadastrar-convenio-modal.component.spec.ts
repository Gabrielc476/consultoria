import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CadastrarConvenioModalComponent } from './cadastrar-convenio-modal.component';
import { ConvenioService, ConvenioResponseDto } from '../../services/convenio.service';
import { ConvenioContextService } from '../../services/convenio-context.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { ToastService } from '../../../../core/ui/toast.service';

describe('CadastrarConvenioModalComponent', () => {
  let component: CadastrarConvenioModalComponent;
  let fixture: ComponentFixture<CadastrarConvenioModalComponent>;
  let convenioService: jasmine.SpyObj<ConvenioService>;
  let convenioCtx: ConvenioContextService;
  let municipioCtx: MunicipioContextService;
  let toastService: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  const mockConvenioResponse: ConvenioResponseDto = {
    id: 'conv-uuid-456',
    tenantId: 'tenant-123',
    prefeituraId: 'mun-patos-01',
    numeroSiconv: '954120/2026',
    numeroProcesso: '00124/2026',
    orgaoConcedente: 'Ministério das Cidades (MCID) / Caixa GIGOV',
    objeto: 'Pavimentação em Paralelepípedo e Drenagem no Bairro Monte Castelo',
    valorGlobal: 1000000.0,
    valorRepasse: 950000.0,
    valorContrapartida: 50000.0,
    situacao: 'EM_EXECUCAO',
    possuiClausulaSuspensiva: true,
    prazoClausulaSuspensiva: '2026-09-28',
    dataInicioVigencia: '2026-03-01',
    dataFimVigencia: '2028-03-01',
    statusClausulaSuspensiva: 'PENDENTE',
    createdAt: '2026-09-28T12:00:00Z',
    updatedAt: '2026-09-28T12:00:00Z'
  };

  beforeEach(async () => {
    localStorage.clear();
    const convSpy = jasmine.createSpyObj('ConvenioService', ['cadastrarConvenio', 'listarConvenios']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['sucesso', 'erro', 'alerta', 'info']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CadastrarConvenioModalComponent],
      providers: [
        { provide: ConvenioService, useValue: convSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        ConvenioContextService,
        MunicipioContextService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CadastrarConvenioModalComponent);
    component = fixture.componentInstance;
    convenioService = TestBed.inject(ConvenioService) as jasmine.SpyObj<ConvenioService>;
    convenioCtx = TestBed.inject(ConvenioContextService);
    municipioCtx = TestBed.inject(MunicipioContextService);
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    municipioCtx.selecionarMunicipio('mun-patos-01');
    fixture.detectChanges();
  });

  it('deve inicializar com cláusula suspensiva ativa e prazos calculados', () => {
    expect(component).toBeTruthy();
    expect(component.possuiClausulaSuspensiva).toBeTrue();
    expect(component.prazoClausulaSuspensiva).toBeTruthy();
    expect(component.dataFimVigencia).toBeTruthy();
    expect(component.salvando()).toBeFalse();
    expect(component.erro()).toBeNull();
  });

  it('deve calcular a contrapartida automaticamente ao alterar global e repasse', () => {
    component.valorGlobal = 1000000;
    component.valorRepasse = 920000;
    component.onValoresChange();

    expect(component.valorContrapartida).toBe(80000);
    expect(component.validacaoFinanceiraOk()).toBeTrue();
  });

  it('deve acusar inconsistência se repasse + contrapartida != valor global', () => {
    component.valorGlobal = 1000000;
    component.valorRepasse = 900000;
    component.valorContrapartida = 50000; // Soma 950.000 != 1.000.000

    expect(component.validacaoFinanceiraOk()).toBeFalse();
  });

  it('deve validar campos obrigatórios antes do salvamento', () => {
    component.numeroSiconv = '';
    component.salvar();

    expect(component.erro()).toContain('Preencha todos os campos obrigatórios');
    expect(convenioService.cadastrarConvenio).not.toHaveBeenCalled();
  });

  it('deve impedir salvamento se a validação financeira falhar', () => {
    component.numeroSiconv = '954120/2026';
    component.orgaoConcedente = 'MCID';
    component.objeto = 'Pavimentação';
    component.valorGlobal = 1000000;
    component.valorRepasse = 800000;
    component.valorContrapartida = 100000; // Inconsistente

    component.salvar();

    expect(component.erro()).toContain('A soma do Repasse com a Contrapartida deve ser exatamente igual');
    expect(convenioService.cadastrarConvenio).not.toHaveBeenCalled();
  });

  it('deve cadastrar convênio com sucesso, adicionar ao cockpit e navegar', () => {
    spyOn(component.fechar, 'emit');
    convenioService.cadastrarConvenio.and.returnValue(of(mockConvenioResponse));

    component.numeroSiconv = '954120/2026';
    component.orgaoConcedente = 'Ministério das Cidades (MCID) / Caixa GIGOV';
    component.objeto = 'Pavimentação em Paralelepípedo e Drenagem no Bairro Monte Castelo';
    component.valorGlobal = 1000000;
    component.valorRepasse = 950000;
    component.valorContrapartida = 50000;
    component.possuiClausulaSuspensiva = true;

    component.salvar();

    expect(convenioService.cadastrarConvenio).toHaveBeenCalled();
    expect(toastService.sucesso).toHaveBeenCalled();
    expect(component.fechar.emit).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/convenios', mockConvenioResponse.id]);

    const convAdicionado = convenioCtx.obterConvenioPorId(mockConvenioResponse.id);
    expect(convAdicionado).toBeDefined();
    expect(convAdicionado?.numeroSiconv).toBe('954120/2026');
    expect(convAdicionado?.faseAtualNumero).toBe(2);
  });

  it('deve tratar fallback localmente e navegar para o novo ID em caso de erro da API', () => {
    spyOn(component.fechar, 'emit');
    convenioService.cadastrarConvenio.and.returnValue(throwError(() => new Error('Offline')));

    component.numeroSiconv = '954120/2026';
    component.orgaoConcedente = 'MCID';
    component.objeto = 'Pavimentação';
    component.valorGlobal = 1000000;
    component.valorRepasse = 950000;
    component.valorContrapartida = 50000;

    component.salvar();

    expect(toastService.sucesso).toHaveBeenCalled();
    expect(component.fechar.emit).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalled();
  });

  it('deve emitir evento de fechar ao invocar fecharModal', () => {
    spyOn(component.fechar, 'emit');
    component.fecharModal();
    expect(component.fechar.emit).toHaveBeenCalled();
  });
});
