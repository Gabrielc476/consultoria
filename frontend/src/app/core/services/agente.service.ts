import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthService } from '../auth/auth.service';

export interface Agente {
  id: string;
  tenantId: string;
  nome: string;
  email: string;
  telefoneCelular?: string;
  role: 'ADMIN' | 'AGENTE';
  ativo: boolean;
  prefeiturasAtribuidasIds: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CriarAgentePayload {
  nome: string;
  email: string;
  senha: string;
  telefoneCelular?: string;
  prefeiturasIds?: string[];
}

export interface AtualizarAgentePayload {
  nome?: string;
  telefoneCelular?: string;
  ativo?: boolean;
  prefeiturasIds?: string[];
}

export const MOCK_AGENTES: Agente[] = [
  {
    id: 'agente-demo-01',
    tenantId: 'consultoria-alianca-pb',
    nome: 'Carlos Gestor',
    email: 'carlos@planejabrasil.com.br',
    telefoneCelular: '+5583999998888',
    role: 'ADMIN',
    ativo: true,
    prefeiturasAtribuidasIds: ['mun-patos-01', 'mun-sousa-02', 'mun-pombal-03', 'mun-monteiro-04', 'mun-cajazeiras-05']
  },
  {
    id: 'agente-demo-02',
    tenantId: 'consultoria-alianca-pb',
    nome: 'João Analista',
    email: 'joao@planejabrasil.com.br',
    telefoneCelular: '+5583988881111',
    role: 'AGENTE',
    ativo: true,
    prefeiturasAtribuidasIds: ['mun-patos-01']
  },
  {
    id: 'agente-demo-03',
    tenantId: 'consultoria-alianca-pb',
    nome: 'Mariana Engenharia',
    email: 'mariana@planejabrasil.com.br',
    telefoneCelular: '+5583977772222',
    role: 'AGENTE',
    ativo: true,
    prefeiturasAtribuidasIds: ['mun-sousa-02', 'mun-pombal-03']
  }
];

@Injectable({
  providedIn: 'root'
})
export class AgenteService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private agentesLocaisDemo: Agente[] = [...MOCK_AGENTES];

  listarAgentes(): Observable<Agente[]> {
    if (this.auth.isModoDemo()) {
      return of([...this.agentesLocaisDemo]);
    }
    return this.http.get<Agente[]>(API_ENDPOINTS.AGENTES.BASE);
  }

  buscarPorId(id: string): Observable<Agente> {
    if (this.auth.isModoDemo()) {
      const agente = this.agentesLocaisDemo.find(a => a.id === id);
      return of(agente || this.agentesLocaisDemo[0]);
    }
    return this.http.get<Agente>(API_ENDPOINTS.AGENTES.POR_ID(id));
  }

  cadastrarAgente(payload: CriarAgentePayload): Observable<Agente> {
    if (this.auth.isModoDemo()) {
      const novo: Agente = {
        id: 'agente-demo-' + (this.agentesLocaisDemo.length + 1),
        tenantId: 'consultoria-alianca-pb',
        nome: payload.nome,
        email: payload.email,
        telefoneCelular: payload.telefoneCelular || '+5583999990000',
        role: 'AGENTE',
        ativo: true,
        prefeiturasAtribuidasIds: payload.prefeiturasIds || []
      };
      this.agentesLocaisDemo.push(novo);
      return of(novo);
    }
    return this.http.post<Agente>(API_ENDPOINTS.AGENTES.BASE, payload);
  }

  atualizarAgente(id: string, payload: AtualizarAgentePayload): Observable<Agente> {
    if (this.auth.isModoDemo()) {
      const idx = this.agentesLocaisDemo.findIndex(a => a.id === id);
      if (idx !== -1) {
        this.agentesLocaisDemo[idx] = {
          ...this.agentesLocaisDemo[idx],
          ...payload,
          prefeiturasAtribuidasIds: payload.prefeiturasIds || this.agentesLocaisDemo[idx].prefeiturasAtribuidasIds
        };
        return of(this.agentesLocaisDemo[idx]);
      }
    }
    return this.http.put<Agente>(API_ENDPOINTS.AGENTES.POR_ID(id), payload);
  }

  inativarAgente(id: string): Observable<void> {
    if (this.auth.isModoDemo()) {
      const agente = this.agentesLocaisDemo.find(a => a.id === id);
      if (agente) {
        agente.ativo = false;
      }
      return of(undefined);
    }
    return this.http.delete<void>(API_ENDPOINTS.AGENTES.POR_ID(id));
  }
}
