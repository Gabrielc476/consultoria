import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { CadastrarPrefeituraModalComponent } from './cadastrar-prefeitura-modal.component';
import { PrefeituraService, PrefeituraResponse } from '../../services/prefeitura.service';
import { MunicipioContextService } from '../../context/municipio-context.service';
import { ToastService } from '../../ui/toast.service';

describe('CadastrarPrefeituraModalComponent', () => {
  let component: CadastrarPrefeituraModalComponent;
  let fixture: ComponentFixture<CadastrarPrefeituraModalComponent>;
  let prefeituraService: jasmine.SpyObj<PrefeituraService>;
  let municipioCtx: MunicipioContextService;
  let toastService: jasmine.SpyObj<ToastService>;

  const mockPrefeituraResponse: PrefeituraResponse = {
    id: 'pref-uuid-123',
    tenantId: 'tenant-123',
    cnpj: '13.519.354/0001-99',
    razaoSocial: 'Prefeitura Municipal de Monteiro',
    nomeMunicipio: 'Monteiro',
    uf: 'PB',
    codigoIbge: '2509701',
    porteMunicipio: 'PEQUENO_PORTE_1',
    statusCauc: 'REGULAR',
    ativo: true,
    createdAt: '2026-09-28T12:00:00Z',
    updatedAt: '2026-09-28T12:00:00Z'
  };

  beforeEach(async () => {
    localStorage.clear();
    const prefSpy = jasmine.createSpyObj('PrefeituraService', ['cadastrarPrefeitura', 'listarPrefeituras']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['sucesso', 'erro', 'alerta', 'info']);

    await TestBed.configureTestingModule({
      imports: [CadastrarPrefeituraModalComponent],
      providers: [
        { provide: PrefeituraService, useValue: prefSpy },
        { provide: ToastService, useValue: toastSpy },
        MunicipioContextService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CadastrarPrefeituraModalComponent);
    component = fixture.componentInstance;
    prefeituraService = TestBed.inject(PrefeituraService) as jasmine.SpyObj<PrefeituraService>;
    municipioCtx = TestBed.inject(MunicipioContextService);
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    fixture.detectChanges();
  });

  it('deve inicializar o modal com valores padrão', () => {
    expect(component).toBeTruthy();
    expect(component.uf).toBe('PB');
    expect(component.porteMunicipio).toBe('PEQUENO_PORTE_1');
    expect(component.salvando()).toBeFalse();
    expect(component.erro()).toBeNull();
  });

  it('deve aplicar máscara no CNPJ digitado', () => {
    const event = {
      target: { value: '13519354000199' }
    } as unknown as Event;

    component.aplicarMascaraCnpj(event);
    expect(component.cnpj).toBe('13.519.354/0001-99');
  });

  it('deve validar campos obrigatórios antes do envio', () => {
    component.cnpj = '';
    component.salvar();
    expect(component.erro()).toContain('Preencha todos os campos obrigatórios');
    expect(prefeituraService.cadastrarPrefeitura).not.toHaveBeenCalled();
  });

  it('deve validar que o Código IBGE possui 7 dígitos', () => {
    component.cnpj = '13.519.354/0001-99';
    component.razaoSocial = 'Prefeitura de Monteiro';
    component.nomeMunicipio = 'Monteiro';
    component.uf = 'PB';
    component.codigoIbge = '1234'; // Menos de 7 dígitos

    component.salvar();
    expect(component.erro()).toContain('O Código IBGE deve possuir exatamente 7 dígitos numéricos');
    expect(prefeituraService.cadastrarPrefeitura).not.toHaveBeenCalled();
  });

  it('deve cadastrar prefeitura com sucesso, atualizar contexto e fechar modal', () => {
    spyOn(component.fechar, 'emit');
    spyOn(component.cadastrado, 'emit');
    prefeituraService.cadastrarPrefeitura.and.returnValue(of(mockPrefeituraResponse));

    component.cnpj = '13.519.354/0001-99';
    component.razaoSocial = 'Prefeitura Municipal de Monteiro';
    component.nomeMunicipio = 'Monteiro';
    component.uf = 'PB';
    component.codigoIbge = '2509701';
    component.porteMunicipio = 'PEQUENO_PORTE_1';
    component.nomePrefeito = 'Maria Silva';

    component.salvar();

    expect(prefeituraService.cadastrarPrefeitura).toHaveBeenCalledWith({
      cnpj: '13.519.354/0001-99',
      razaoSocial: 'Prefeitura Municipal de Monteiro',
      nomeMunicipio: 'Monteiro',
      uf: 'PB',
      codigoIbge: '2509701',
      porteMunicipio: 'PEQUENO_PORTE_1',
      nomePrefeito: 'Maria Silva',
      cpfPrefeito: undefined
    });

    expect(toastService.sucesso).toHaveBeenCalled();
    expect(component.cadastrado.emit).toHaveBeenCalledWith(mockPrefeituraResponse);
    expect(component.fechar.emit).toHaveBeenCalled();

    const municipioAdicionado = municipioCtx.municipios().find(m => m.id === mockPrefeituraResponse.id);
    expect(municipioAdicionado).toBeDefined();
    expect(municipioAdicionado?.nome).toBe('Monteiro');
  });

  it('deve aplicar fallback local gracioso em caso de erro da API', () => {
    spyOn(component.fechar, 'emit');
    spyOn(component.cadastrado, 'emit');
    prefeituraService.cadastrarPrefeitura.and.returnValue(throwError(() => new Error('Falha de rede')));

    component.cnpj = '13.519.354/0001-99';
    component.razaoSocial = 'Prefeitura Municipal de Monteiro';
    component.nomeMunicipio = 'Monteiro';
    component.uf = 'PB';
    component.codigoIbge = '2509701';

    component.salvar();

    expect(toastService.sucesso).toHaveBeenCalled();
    expect(component.cadastrado.emit).toHaveBeenCalled();
    expect(component.fechar.emit).toHaveBeenCalled();
  });

  it('deve emitir evento de fechar ao invocar fecharModal', () => {
    spyOn(component.fechar, 'emit');
    component.fecharModal();
    expect(component.fechar.emit).toHaveBeenCalled();
  });
});
