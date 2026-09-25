import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RadarPrazosApiService } from './radar-prazos-api.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { RadarPrazosResponse } from '../model/radar-prazos.model';

describe('RadarPrazosApiService', () => {
  let service: RadarPrazosApiService;
  let httpMock: HttpTestingController;

  const mockResponse: RadarPrazosResponse = {
    resumo: {
      totalMonitorados: 5,
      totalCriticos: 2,
      totalAtencao: 1,
      totalRegulares: 2,
      dataReferencia: '2026-09-24'
    },
    agrupamentoPorMunicipio: [
      {
        municipio: 'Massaranduba',
        uf: 'PB',
        cnpjProponente: '08847784000144',
        nomeProponente: 'PREFEITURA DE MASSARANDUBA',
        totalConvenios: 3,
        totalCriticos: 2,
        totalAtencao: 1,
        totalRegulares: 0,
        maiorRisco: 'CRITICO'
      }
    ],
    alertas: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RadarPrazosApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(RadarPrazosApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeTruthy();
  });

  it('deve buscar dados do radar com parâmetros opcionais', () => {
    service.getRadar({ uf: 'PB', nivelRisco: 'CRITICO' }).subscribe((res) => {
      expect(res).toEqual(mockResponse);
      expect(res.resumo.totalCriticos).toBe(2);
    });

    const req = httpMock.expectOne((request) => {
      return (
        request.url === API_ENDPOINTS.TRANSFEREGOV.RADAR_PRAZOS &&
        request.params.get('uf') === 'PB' &&
        request.params.get('nivelRisco') === 'CRITICO'
      );
    });

    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('deve disparar avaliação de prazos via POST', () => {
    const mockPostResponse = { status: 'SUCESSO', totalAlertasCriticosEmitidos: 2 };

    service.dispararAvaliacao('2026-09-24').subscribe((res) => {
      expect(res.status).toBe('SUCESSO');
      expect(res.totalAlertasCriticosEmitidos).toBe(2);
    });

    const req = httpMock.expectOne((request) => {
      return (
        request.url === API_ENDPOINTS.TRANSFEREGOV.AVALIAR_PRAZOS &&
        request.params.get('dataReferencia') === '2026-09-24'
      );
    });

    expect(req.request.method).toBe('POST');
    req.flush(mockPostResponse);
  });
});
