import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { FiltrosRadarRequest, RadarPrazosResponse } from '../model/radar-prazos.model';

@Injectable({
  providedIn: 'root'
})
export class RadarPrazosApiService {
  private readonly http = inject(HttpClient);

  getRadar(filtros?: FiltrosRadarRequest): Observable<RadarPrazosResponse> {
    let params = new HttpParams();

    if (filtros) {
      if (filtros.uf) {
        params = params.set('uf', filtros.uf);
      }
      if (filtros.cnpj) {
        params = params.set('cnpj', filtros.cnpj);
      }
      if (filtros.municipio) {
        params = params.set('municipio', filtros.municipio);
      }
      if (filtros.nivelRisco) {
        params = params.set('nivelRisco', filtros.nivelRisco);
      }
      if (filtros.dataReferencia) {
        params = params.set('dataReferencia', filtros.dataReferencia);
      }
    }

    return this.http.get<RadarPrazosResponse>(API_ENDPOINTS.TRANSFEREGOV.RADAR_PRAZOS, { params });
  }

  dispararAvaliacao(dataReferencia?: string): Observable<{ status: string; totalAlertasCriticosEmitidos: number }> {
    let params = new HttpParams();
    if (dataReferencia) {
      params = params.set('dataReferencia', dataReferencia);
    }

    return this.http.post<{ status: string; totalAlertasCriticosEmitidos: number }>(
      API_ENDPOINTS.TRANSFEREGOV.AVALIAR_PRAZOS,
      {},
      { params }
    );
  }
}
