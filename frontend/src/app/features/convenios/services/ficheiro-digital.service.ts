import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import {
  DocumentoFicheiro,
  ExcluirDocumentoPayload,
  FicheiroDigital,
  MoverDocumentoPayload,
  PreviewDocumento
} from '../model/ficheiro-digital.model';

@Injectable({
  providedIn: 'root'
})
export class FicheiroDigitalService {
  private readonly http = inject(HttpClient);

  obterFicheiro(convenioId: string): Observable<FicheiroDigital> {
    return this.http.get<FicheiroDigital>(API_ENDPOINTS.FICHEIRO.OBTER(convenioId));
  }

  listarDocumentosFase(convenioId: string, fase: string): Observable<DocumentoFicheiro[]> {
    return this.http.get<DocumentoFicheiro[]>(API_ENDPOINTS.FICHEIRO.FASES_DOCUMENTOS(convenioId, fase));
  }

  uploadDocumento(
    convenioId: string,
    arquivo: File,
    fase?: string,
    categoria?: string,
    pastaVirtual?: string,
    tags?: string[]
  ): Observable<DocumentoFicheiro> {
    const formData = new FormData();
    formData.append('arquivo', arquivo, arquivo.name);
    if (fase) formData.append('fase', fase);
    if (categoria) formData.append('categoria', categoria);
    if (pastaVirtual) formData.append('pastaVirtual', pastaVirtual);
    if (tags && tags.length > 0) {
      tags.forEach(t => formData.append('tags', t));
    }

    return this.http.post<DocumentoFicheiro>(API_ENDPOINTS.FICHEIRO.UPLOAD(convenioId), formData);
  }

  downloadZipConvenio(convenioId: string): Observable<Blob> {
    return this.http.get(API_ENDPOINTS.FICHEIRO.DOWNLOAD_ZIP_CONVENIO(convenioId), {
      responseType: 'blob'
    });
  }

  downloadZipFase(convenioId: string, fase: string): Observable<Blob> {
    return this.http.get(API_ENDPOINTS.FICHEIRO.DOWNLOAD_ZIP_FASE(convenioId, fase), {
      responseType: 'blob'
    });
  }

  obterPreview(documentoId: string): Observable<PreviewDocumento> {
    return this.http.get<PreviewDocumento>(API_ENDPOINTS.FICHEIRO.PREVIEW(documentoId));
  }

  moverDocumento(documentoId: string, payload: MoverDocumentoPayload): Observable<DocumentoFicheiro> {
    return this.http.patch<DocumentoFicheiro>(API_ENDPOINTS.FICHEIRO.MOVER(documentoId), payload);
  }

  excluirDocumento(documentoId: string, payload: ExcluirDocumentoPayload): Observable<void> {
    return this.http.delete<void>(API_ENDPOINTS.FICHEIRO.EXCLUIR(documentoId), {
      body: payload
    });
  }

  listarAuditoria(documentoId: string): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.FICHEIRO.HISTORICO_AUDITORIA(documentoId));
  }
}
