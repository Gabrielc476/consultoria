import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import {
  Documento,
  PageResponse,
  AprovarDocumentoPayload,
  RejeitarDocumentoPayload
} from '../model/documento.model';

@Injectable({
  providedIn: 'root'
})
export class RevisaoApiService {
  private readonly http = inject(HttpClient);

  public getDocumento(id: string): Observable<Documento> {
    return this.http.get<Documento>(API_ENDPOINTS.DOCUMENTOS.POR_ID(id));
  }

  public listarDocumentos(status?: string, page = 0, size = 20): Observable<PageResponse<Documento>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PageResponse<Documento>>(API_ENDPOINTS.DOCUMENTOS.BASE, { params });
  }

  public aprovarDocumento(id: string, payload: AprovarDocumentoPayload): Observable<Documento> {
    return this.http.put<Documento>(API_ENDPOINTS.DOCUMENTOS.APROVAR(id), payload);
  }

  public rejeitarDocumento(id: string, payload: RejeitarDocumentoPayload): Observable<Documento> {
    return this.http.put<Documento>(API_ENDPOINTS.DOCUMENTOS.REJEITAR(id), payload);
  }

  public getUrlArquivo(id: string): string {
    return API_ENDPOINTS.DOCUMENTOS.ARQUIVO(id);
  }
}
