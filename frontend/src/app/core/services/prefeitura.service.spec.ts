import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PrefeituraService, CadastrarPrefeituraPayload } from './prefeitura.service';
import { API_ENDPOINTS } from '../api/api-endpoints';

describe('PrefeituraService', () => {
  let service: PrefeituraService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PrefeituraService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PrefeituraService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve cadastrar uma prefeitura via POST', () => {
    const payload: CadastrarPrefeituraPayload = {
      cnpj: '09.288.665/0001-38',
      razaoSocial: 'Prefeitura Municipal de Esperança',
      nomeMunicipio: 'Esperança',
      uf: 'PB',
      codigoIbge: '2506004',
      porteMunicipio: 'PEQUENO_PORTE_1'
    };

    let resposta: any;
    service.cadastrarPrefeitura(payload).subscribe(res => {
      resposta = res;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.PREFEITURAS.BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({
      id: 'prefeitura-uuid-123',
      ...payload,
      statusCauc: 'REGULAR',
      ativo: true
    });

    expect(resposta).toBeTruthy();
    expect(resposta.id).toBe('prefeitura-uuid-123');
    expect(resposta.nomeMunicipio).toBe('Esperança');
  });

  it('deve listar prefeituras via GET com parâmetros de paginação', () => {
    service.listarPrefeituras(0, 10, true).subscribe();

    const req = httpMock.expectOne(req => req.url === API_ENDPOINTS.PREFEITURAS.BASE);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('ativo')).toBe('true');

    req.flush({ content: [], totalElements: 0 });
  });
});
