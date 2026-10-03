import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { CadastrarContatoETriarPayload, CadastrarContatoPayload, ContatoDto, TriagemItem } from '../model/triagem.model';
import { ToastService } from '../../../core/ui/toast.service';

@Injectable({
  providedIn: 'root'
})
export class TriagemService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  readonly itensPendentes = signal<TriagemItem[]>([]);
  readonly carregando = signal<boolean>(false);
  readonly erro = signal<string | null>(null);

  readonly totalPendentes = computed(() => this.itensPendentes().length);

  carregarPendentes(): Observable<TriagemItem[]> {
    this.carregando.set(true);
    this.erro.set(null);

    return this.http.get<TriagemItem[]>(API_ENDPOINTS.TRIAGEM.PENDENTES).pipe(
      tap((itens) => {
        this.itensPendentes.set(itens);
        this.carregando.set(false);
      }),
      catchError((err) => {
        const msg = err.error?.detail || err.error?.message || 'Falha ao carregar itens da triagem';
        this.erro.set(msg);
        this.carregando.set(false);
        this.toast.erro('Erro na Triagem', msg);
        return throwError(() => err);
      })
    );
  }

  confirmarArquivamento(inboxId: string, convenioId?: string, faseCicloVida?: string): Observable<TriagemItem> {
    this.carregando.set(true);
    let params: any = {};
    if (convenioId) params.convenioId = convenioId;
    if (faseCicloVida) params.faseCicloVida = faseCicloVida;

    return this.http.post<TriagemItem>(API_ENDPOINTS.TRIAGEM.CONFIRMAR_ARQUIVAMENTO(inboxId), null, { params }).pipe(
      tap((itemAtualizado) => {
        this.itensPendentes.update((lista) => lista.filter((i) => i.id !== inboxId));
        this.carregando.set(false);
        this.toast.sucesso('Documento Arquivado', 'Documento arquivado com sucesso no Ficheiro Digital!');
      }),
      catchError((err) => {
        this.carregando.set(false);
        const msg = err.error?.detail || err.error?.message || 'Erro ao confirmar arquivamento';
        this.toast.erro('Falha no Arquivamento', msg);
        return throwError(() => err);
      })
    );
  }

  ignorarItem(inboxId: string): Observable<TriagemItem> {
    this.carregando.set(true);
    return this.http.post<TriagemItem>(API_ENDPOINTS.TRIAGEM.IGNORAR(inboxId), null).pipe(
      tap(() => {
        this.itensPendentes.update((lista) => lista.filter((i) => i.id !== inboxId));
        this.carregando.set(false);
        this.toast.aviso('Item Ignorado', 'Item de triagem ignorado com sucesso.');
      }),
      catchError((err) => {
        this.carregando.set(false);
        const msg = err.error?.detail || err.error?.message || 'Erro ao ignorar item';
        this.toast.erro('Falha ao Ignorar', msg);
        return throwError(() => err);
      })
    );
  }

  cadastrarContatoETriar(inboxId: string, payload: CadastrarContatoETriarPayload): Observable<TriagemItem> {
    this.carregando.set(true);
    return this.http.post<TriagemItem>(API_ENDPOINTS.TRIAGEM.CADASTRAR_CONTATO_E_ARQUIVAR(inboxId), payload).pipe(
      tap((itemResolvido) => {
        this.itensPendentes.update((lista) => lista.filter((i) => i.id !== inboxId));
        this.carregando.set(false);
        this.toast.sucesso('Contato e Triagem', 'Contato cadastrado e documento arquivado com sucesso!');
      }),
      catchError((err) => {
        this.carregando.set(false);
        const msg = err.error?.detail || err.error?.message || 'Erro ao cadastrar contato e triar';
        this.toast.erro('Falha no Cadastro', msg);
        return throwError(() => err);
      })
    );
  }

  listarContatos(): Observable<ContatoDto[]> {
    return this.http.get<ContatoDto[]>(API_ENDPOINTS.CONTATOS.BASE);
  }

  cadastrarContato(payload: CadastrarContatoPayload): Observable<ContatoDto> {
    return this.http.post<ContatoDto>(API_ENDPOINTS.CONTATOS.BASE, payload).pipe(
      tap(() => this.toast.sucesso('Contato Cadastrado', 'Contato cadastrado com sucesso!'))
    );
  }

  baixarArquivo(documentoId: string): Observable<Blob> {
    return this.http.get(API_ENDPOINTS.DOCUMENTOS.ARQUIVO(documentoId), {
      responseType: 'blob'
    });
  }
}
