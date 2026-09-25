import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';

export interface UsuarioAutenticado {
  analistaId: string;
  nome: string;
  email: string;
  tenantId: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  analistaId: string;
  nome: string;
  email: string;
  tenantId: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'govflow_auth_token';
  private readonly USER_KEY = 'govflow_auth_user';

  private readonly _token = signal<string | null>(this.getStoredToken());
  private readonly _usuario = signal<UsuarioAutenticado | null>(this.getStoredUser());

  public readonly token = this._token.asReadonly();
  public readonly usuario = this._usuario.asReadonly();
  public readonly isAuthenticated = computed(() => !!this._token());

  public login(email: string, senha: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(API_ENDPOINTS.AUTH.LOGIN, { email, senha }).pipe(
      tap(response => {
        this.salvarSessao(response);
      })
    );
  }

  public entrarModoDemo(): void {
    const demoResponse: LoginResponse = {
      token: 'demo-token-govflow-jwt-expert',
      tokenType: 'Bearer',
      analistaId: 'analista-demo-01',
      nome: 'Gabriel Especialista',
      email: 'analista@govflow.com.br',
      tenantId: 'consultoria-alianca-pb'
    };
    this.salvarSessao(demoResponse);
    this.router.navigate(['/convenios']);
  }

  public logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._token.set(null);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  private salvarSessao(response: LoginResponse): void {
    const usuario: UsuarioAutenticado = {
      analistaId: response.analistaId,
      nome: response.nome,
      email: response.email,
      tenantId: response.tenantId
    };

    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(usuario));

    this._token.set(response.token);
    this._usuario.set(usuario);
  }

  private getStoredToken(): string | null {
    try {
      return localStorage.getItem(this.TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private getStoredUser(): UsuarioAutenticado | null {
    try {
      const data = localStorage.getItem(this.USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }
}
