import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ConvenioService, CadastrarConvenioPayload } from './convenio.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';

describe('ConvenioService', () => {
  let service: ConvenioService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ConvenioService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ConvenioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve cadastrar novo convênio via POST', () => {
    const payload: CadastrarConvenioPayload = {
      prefeituraId: 'mun-patos-01',
      numeroSiconv: '954120/2026',
      orgaoConcedente: 'Ministério das Cidades',
      objeto: 'Pavimentação Asfáltica',
      valorGlobal: 1000000,
      valorRepasse: 950000,
      valorContrapartida: 50000,
      possuiClausulaSuspensiva: true,
      prazoClausulaSuspensiva: '2026-10-31',
      dataFimVigencia: '2027-12-31'
    };

    let resposta: any;
    service.cadastrarConvenio(payload).subscribe(res => {
      resposta = res;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({
      id: 'conv-uuid-456',
      ...payload,
      statusClausulaSuspensiva: 'PENDENTE'
    });

    expect(resposta).toBeTruthy();
    expect(resposta.id).toBe('conv-uuid-456');
    expect(resposta.numeroSiconv).toBe('954120/2026');
  });

  it('deve listar convênios via GET com filtro por prefeituraId', () => {
    service.listarConvenios('mun-patos-01').subscribe();

    const req = httpMock.expectOne(req => req.url === API_ENDPOINTS.CONVENIOS.BASE);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('prefeituraId')).toBe('mun-patos-01');

    req.flush([]);
  });
});
