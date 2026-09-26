import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { DossieCauc, ResumoCauc, CertidaoCaucItem } from '../model/cauc.model';

@Injectable({
  providedIn: 'root'
})
export class RadarCaucService {
  private readonly http = inject(HttpClient);

  obterResumo(): Observable<ResumoCauc> {
    return this.http.get<ResumoCauc>(API_ENDPOINTS.CAUC.RESUMO);
  }

  obterDossie(prefeituraId: string): Observable<DossieCauc> {
    return this.http.get<DossieCauc>(API_ENDPOINTS.CAUC.POR_PREFEITURA(prefeituraId));
  }

  reavaliarConformidade(): Observable<any> {
    return this.http.post<any>(API_ENDPOINTS.CAUC.AVALIAR, {});
  }

  cadastrarCertidao(prefeituraId: string, certidao: any): Observable<CertidaoCaucItem> {
    return this.http.post<CertidaoCaucItem>(API_ENDPOINTS.CAUC.CERTIDOES(prefeituraId), certidao);
  }
}
