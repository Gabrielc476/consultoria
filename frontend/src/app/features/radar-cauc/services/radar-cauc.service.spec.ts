import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RadarCaucService } from './radar-cauc.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';

describe('RadarCaucService', () => {
  let service: RadarCaucService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RadarCaucService]
    });
    service = TestBed.inject(RadarCaucService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve obter o resumo geral do CAUC via GET', () => {
    const mockResumo = {
      totalMunicipios: 5,
      totalRegulares: 75,
      totalAlerta: 3,
      totalVencidas: 2,
      municipios: []
    };

    service.obterResumo().subscribe(res => {
      expect(res.totalMunicipios).toBe(5);
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CAUC.RESUMO);
    expect(req.request.method).toBe('GET');
    req.flush(mockResumo);
  });

  it('deve reavaliar conformidade via POST', () => {
    service.reavaliarConformidade().subscribe(res => {
      expect(res.totalPrefeiturasAvaliadas).toBe(3);
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CAUC.AVALIAR);
    expect(req.request.method).toBe('POST');
    req.flush({ totalPrefeiturasAvaliadas: 3 });
  });
});
