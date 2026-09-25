import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ConvenioContextService } from './convenio-context.service';
import { MunicipioContextService } from '../../../core/context/municipio-context.service';

describe('ConvenioContextService', () => {
  let service: ConvenioContextService;
  let municipioCtx: MunicipioContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ConvenioContextService,
        MunicipioContextService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ConvenioContextService);
    municipioCtx = TestBed.inject(MunicipioContextService);
  });

  it('deve ser instanciado e conter a lista padrão de convênios', () => {
    expect(service).toBeTruthy();
    expect(service.todosConvenios().length).toBeGreaterThan(0);
  });

  it('deve filtrar convênios pelo município ativo', () => {
    municipioCtx.selecionarMunicipio('mun-patos-01');
    const convs = service.conveniosDoMunicipio();
    expect(convs.every(c => c.municipioId === 'mun-patos-01')).toBeTrue();
  });

  it('deve alternar o convênio ativo e atualizar o município se pertencer a outro', () => {
    // Alterna para um convênio de Sousa
    service.selecionarConvenio('conv-921004');
    expect(service.convenioAtivo().id).toBe('conv-921004');
    expect(municipioCtx.municipioAtivoId()).toBe('mun-sousa-02');
  });
});
