import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';

export type PorteMunicipio = 'PEQUENO_PORTE_1' | 'PEQUENO_PORTE_2' | 'MEDIO_PORTE' | 'GRANDE_PORTE';

export interface CadastrarPrefeituraPayload {
  cnpj: string;
  razaoSocial: string;
  nomeMunicipio: string;
  uf: string;
  codigoIbge: string;
  porteMunicipio: PorteMunicipio;
  nomePrefeito?: string;
  cpfPrefeito?: string;
  inicioMandato?: string;
  fimMandato?: string;
}

export interface PrefeituraResponse {
  id: string;
  tenantId: string;
  cnpj: string;
  razaoSocial: string;
  nomeMunicipio: string;
  uf: string;
  codigoIbge: string;
  porteMunicipio: PorteMunicipio;
  nomePrefeito?: string;
  cpfPrefeito?: string;
  inicioMandato?: string;
  fimMandato?: string;
  statusCauc: string;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PrefeituraService {
  private readonly http = inject(HttpClient);

  cadastrarPrefeitura(payload: CadastrarPrefeituraPayload): Observable<PrefeituraResponse> {
    return this.http.post<PrefeituraResponse>(API_ENDPOINTS.PREFEITURAS.BASE, payload);
  }

  listarPrefeituras(page = 0, size = 50, ativo?: boolean): Observable<any> {
    const params: any = { page, size };
    if (ativo !== undefined) {
      params.ativo = ativo;
    }
    return this.http.get<any>(API_ENDPOINTS.PREFEITURAS.BASE, { params });
  }

  buscarPorId(id: string): Observable<PrefeituraResponse> {
    return this.http.get<PrefeituraResponse>(`${API_ENDPOINTS.PREFEITURAS.BASE}/${id}`);
  }
}
