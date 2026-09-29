import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Municipio, SituacaoCauc } from './municipio.model';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthService } from '../auth/auth.service';

/**
 * =========================================================================
 * 📌 MAPEAMENTO DE PREFEITURAS - MUNICÍPIO CONTEXT SERVICE
 * -------------------------------------------------------------------------
 * - Modo Demo / Specs Unitários: Carrega MOCK_MUNICIPIOS.
 * - Sessão Real Autenticada: Carrega dados exclusivos do tenant via
 *   GET /api/v1/prefeituras. Se o tenant for novo, inicializa com lista vazia.
 * =========================================================================
 */
export const MOCK_MUNICIPIOS: Municipio[] = [
  {
    id: 'mun-patos-01',
    nome: 'Patos',
    uf: 'PB',
    codigoIbge: '2510808',
    cnpj: '09.288.665/0001-38',
    conveniosAtivos: 5,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-sousa-02',
    nome: 'Sousa',
    uf: 'PB',
    codigoIbge: '2516201',
    cnpj: '08.924.032/0001-92',
    conveniosAtivos: 3,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-pombal-03',
    nome: 'Pombal',
    uf: 'PB',
    codigoIbge: '2512101',
    cnpj: '08.924.032/0001-90',
    conveniosAtivos: 3,
    prazosCriticos: 0,
    situacaoCauc: 'ALERTA'
  },
  {
    id: 'mun-monteiro-04',
    nome: 'Monteiro',
    uf: 'PB',
    codigoIbge: '2509701',
    cnpj: '09.073.628/0001-91',
    conveniosAtivos: 3,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-cajazeiras-05',
    nome: 'Cajazeiras',
    uf: 'PB',
    codigoIbge: '2503704',
    cnpj: '08.923.976/0001-04',
    conveniosAtivos: 3,
    prazosCriticos: 0,
    situacaoCauc: 'REGULAR'
  }
];

const STORAGE_KEY_MUNICIPIO_ATIVO = 'govflow_municipio_ativo_id';

@Injectable({
  providedIn: 'root'
})
export class MunicipioContextService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  readonly municipios = signal<Municipio[]>(MOCK_MUNICIPIOS);
  readonly municipioAtivoId = signal<string | null>(this.obterIdSalvo() || 'mun-patos-01');

  readonly municipioAtivo = computed(() => {
    const lista = this.municipios();
    if (lista.length === 0) return null;
    const id = this.municipioAtivoId();
    if (!id) return lista[0] || null;
    return lista.find(m => m.id === id) || lista[0] || null;
  });

  readonly nomeMunicipioAtivoFormatado = computed(() => {
    const mun = this.municipioAtivo();
    return mun ? `Prefeitura de ${mun.nome} - ${mun.uf}` : 'Nenhuma Prefeitura Selecionada';
  });

  constructor() {
    this.carregarMunicipios();
    effect(() => {
      // Reage quando o token de autenticação mudar (login, logout, demo)
      this.auth.token();
      this.carregarMunicipios();
    });
  }

  carregarMunicipios(): void {
    if (this.auth.isModoDemo() || !this.auth.isAuthenticated()) {
      this.municipios.set(MOCK_MUNICIPIOS);
      const salvo = this.obterIdSalvo();
      const existe = MOCK_MUNICIPIOS.find(m => m.id === salvo);
      this.municipioAtivoId.set(existe ? salvo : 'mun-patos-01');
      return;
    }

    // Sessão Real de Tenant
    this.http.get<any>(API_ENDPOINTS.PREFEITURAS.BASE).subscribe({
      next: (dados) => {
        const lista = Array.isArray(dados) ? dados : (dados?.content && Array.isArray(dados.content) ? dados.content : []);
        const mapeados: Municipio[] = lista.map((item: any) => ({
          id: item.id || item.codigoIbge,
          nome: item.nomeMunicipio || item.nome || item.razaoSocial,
          uf: item.uf || 'PB',
          codigoIbge: item.codigoIbge || '',
          cnpj: item.cnpj || '',
          conveniosAtivos: item.conveniosAtivos ?? 0,
          prazosCriticos: item.prazosCriticos ?? 0,
          situacaoCauc: (item.statusCauc as SituacaoCauc) || (item.situacaoCauc as SituacaoCauc) || 'REGULAR'
        }));
        this.municipios.set(mapeados);
        if (mapeados.length > 0) {
          const salvo = this.obterIdSalvo();
          const aindaExiste = mapeados.find(m => m.id === salvo);
          this.selecionarMunicipio(aindaExiste ? salvo : mapeados[0].id);
        } else {
          this.selecionarMunicipio(null);
        }
      },
      error: () => {
        this.municipios.set([]);
        this.selecionarMunicipio(null);
      }
    });
  }

  recarregarMunicipios(): void {
    this.carregarMunicipios();
  }

  adicionarMunicipio(novo: Municipio): void {
    this.municipios.update(lista => {
      const semDuplicados = lista.filter(m => m.id !== novo.id && m.codigoIbge !== novo.codigoIbge);
      return [novo, ...semDuplicados];
    });
    this.selecionarMunicipio(novo.id);
  }

  selecionarMunicipio(id: string | null): void {
    this.municipioAtivoId.set(id);
    if (id) {
      localStorage.setItem(STORAGE_KEY_MUNICIPIO_ATIVO, id);
    } else {
      localStorage.removeItem(STORAGE_KEY_MUNICIPIO_ATIVO);
    }
  }

  private obterIdSalvo(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY_MUNICIPIO_ATIVO);
    } catch {
      return null;
    }
  }
}
