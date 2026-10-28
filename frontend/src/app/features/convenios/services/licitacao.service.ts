import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import {
  CadastrarLicitacaoPayload,
  EmitirAioPayload,
  HomologarLicitacaoPayload,
  Licitacao,
  RegistrarResultadoVrplPayload,
  SubmeterVrplPayload
} from '../model/licitacao.model';

@Injectable({ providedIn: 'root' })
export class LicitacaoService {
  private readonly http = inject(HttpClient);

  listar(convenioId: string): Observable<Licitacao[]> {
    return this.http.get<Licitacao[]>(API_ENDPOINTS.CONVENIOS.LICITACOES(convenioId));
  }

  buscar(convenioId: string, licitacaoId: string): Observable<Licitacao> {
    return this.http.get<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACAO(convenioId, licitacaoId));
  }

  cadastrar(convenioId: string, payload: CadastrarLicitacaoPayload): Observable<Licitacao> {
    return this.http.post<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACOES(convenioId), payload);
  }

  homologar(convenioId: string, licitacaoId: string, payload: HomologarLicitacaoPayload): Observable<Licitacao> {
    return this.http.post<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACAO_HOMOLOGAR(convenioId, licitacaoId), payload);
  }

  submeterVrpl(convenioId: string, licitacaoId: string, payload: SubmeterVrplPayload): Observable<Licitacao> {
    return this.http.post<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACAO_VRPL_SUBMETER(convenioId, licitacaoId), payload);
  }

  registrarResultadoVrpl(
    convenioId: string,
    licitacaoId: string,
    payload: RegistrarResultadoVrplPayload
  ): Observable<Licitacao> {
    return this.http.post<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACAO_VRPL_RESULTADO(convenioId, licitacaoId), payload);
  }

  emitirAio(convenioId: string, licitacaoId: string, payload: EmitirAioPayload): Observable<Licitacao> {
    return this.http.post<Licitacao>(API_ENDPOINTS.CONVENIOS.LICITACAO_AIO(convenioId, licitacaoId), payload);
  }

  validarAioParaMedicao(convenioId: string, licitacaoId: string): Observable<void> {
    return this.http.post<void>(API_ENDPOINTS.CONVENIOS.LICITACAO_VALIDAR_AIO_MEDICAO(convenioId, licitacaoId), {});
  }
}
