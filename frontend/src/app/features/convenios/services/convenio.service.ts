import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';

export interface CadastrarConvenioPayload {
  prefeituraId: string;
  numeroSiconv: string;
  numeroProcesso?: string;
  orgaoConcedente: string;
  objeto: string;
  valorGlobal: number;
  valorRepasse: number;
  valorContrapartida: number;
  possuiClausulaSuspensiva: boolean;
  prazoClausulaSuspensiva?: string;
  dataInicioVigencia?: string;
  dataFimVigencia: string;
}

export interface ConvenioResponseDto {
  id: string;
  tenantId: string;
  prefeituraId: string;
  numeroSiconv: string;
  numeroProcesso?: string;
  orgaoConcedente: string;
  objeto: string;
  valorGlobal: number;
  valorRepasse: number;
  valorContrapartida: number;
  situacao: string;
  possuiClausulaSuspensiva: boolean;
  prazoClausulaSuspensiva?: string;
  dataInicioVigencia?: string;
  dataFimVigencia: string;
  statusClausulaSuspensiva: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConvenioService {
  private readonly http = inject(HttpClient);

  cadastrarConvenio(payload: CadastrarConvenioPayload): Observable<ConvenioResponseDto> {
    return this.http.post<ConvenioResponseDto>(API_ENDPOINTS.CONVENIOS.BASE, payload);
  }

  listarConvenios(prefeituraId?: string): Observable<ConvenioResponseDto[]> {
    const params: any = {};
    if (prefeituraId) {
      params.prefeituraId = prefeituraId;
    }
    return this.http.get<ConvenioResponseDto[]>(API_ENDPOINTS.CONVENIOS.BASE, { params });
  }

  buscarPorId(id: string): Observable<ConvenioResponseDto> {
    return this.http.get<ConvenioResponseDto>(API_ENDPOINTS.CONVENIOS.POR_ID(id));
  }
}
