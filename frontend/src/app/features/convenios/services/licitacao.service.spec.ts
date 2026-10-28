import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { LicitacaoService } from './licitacao.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { Licitacao } from '../model/licitacao.model';

describe('LicitacaoService', () => {
  let service: LicitacaoService;
  let httpMock: HttpTestingController;

  const convenioId = 'conv-123';
  const mock: Licitacao = {
    id: 'lic-1',
    tenantId: 't-1',
    convenioId,
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

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LicitacaoService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(LicitacaoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve listar licitações do convênio sem fallback silencioso', () => {
    let resultado: Licitacao[] | undefined;
    service.listar(convenioId).subscribe((data) => (resultado = data));

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.LICITACOES(convenioId));
    expect(req.request.method).toBe('GET');
    req.flush([mock]);

    expect(resultado?.length).toBe(1);
    expect(resultado?.[0].numeroLicitacao).toBe('Concorrência 002/2026');
  });

  it('deve cadastrar certame via POST', () => {
    service.cadastrar(convenioId, {
      numeroLicitacao: 'Pregão 015/2026',
      modalidade: 'PREGAO_ELETRONICO',
      objeto: 'Supervisão',
      valorEstimado: 150000
    }).subscribe();

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.LICITACOES(convenioId));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.numeroLicitacao).toBe('Pregão 015/2026');
    req.flush(mock);
  });

  it('deve submeter VRPL e emitir AIO nos endpoints corretos', () => {
    service.submeterVrpl(convenioId, 'lic-1', {
      numeroVrplTransferegov: 'VRPL-01',
      dataEnvioVrpl: '2026-04-15'
    }).subscribe();
    httpMock.expectOne(API_ENDPOINTS.CONVENIOS.LICITACAO_VRPL_SUBMETER(convenioId, 'lic-1')).flush(mock);

    service.emitirAio(convenioId, 'lic-1', {
      numeroAio: 'AIO-003/2026',
      dataEmissaoAio: '2026-05-20'
    }).subscribe();
    httpMock.expectOne(API_ENDPOINTS.CONVENIOS.LICITACAO_AIO(convenioId, 'lic-1')).flush({
      ...mock,
      statusAio: 'EMITIDO',
      podeCriarMedicao: true
    });
  });
});
