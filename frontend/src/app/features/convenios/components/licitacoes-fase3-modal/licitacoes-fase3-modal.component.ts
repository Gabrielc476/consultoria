import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConvenioCockpit } from '../../model/convenio-fase.model';
import {
  CriterioJulgamento,
  Licitacao,
  ModalidadeLicitacao,
  RegimeExecucao,
  StatusVrpl
} from '../../model/licitacao.model';
import { LicitacaoService } from '../../services/licitacao.service';

@Component({
  selector: 'app-licitacoes-fase3-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
      <div class="bg-[#0e1726] border border-white/10 rounded-2xl max-w-5xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
        <div class="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-gradient-to-r from-gov-cobalt-950/60 via-slate-900 to-[#0e1726] shrink-0">
          <div>
            <span class="px-2.5 py-1 rounded bg-gov-cobalt-500/20 text-gov-cobalt-300 border border-gov-cobalt-500/30 text-xs font-mono font-bold">FASE 03</span>
            <h2 class="text-base sm:text-lg font-bold text-white mt-2">Licitações, VRPL e AIO</h2>
            <p class="text-xs text-gov-slate-400 mt-0.5">
              Convênio: <strong class="text-gov-slate-200">{{ convenio.numeroSiconv }}</strong> • relação 1:N de certames
            </p>
          </div>
          <button type="button" (click)="fechar.emit()" class="text-gov-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 text-lg font-bold">✕</button>
        </div>

        @if (mensagemSucesso()) {
          <div class="px-6 py-2.5 bg-emerald-950/80 border-b border-emerald-500/30 text-emerald-200 text-xs">{{ mensagemSucesso() }}</div>
        }
        @if (mensagemErro()) {
          <div class="px-6 py-2.5 bg-rose-950/80 border-b border-rose-500/30 text-rose-200 text-xs">{{ mensagemErro() }}</div>
        }

        <div class="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          <div class="p-4 rounded-xl bg-white/[0.03] border border-white/10 space-y-3">
            <h3 class="text-sm font-semibold text-white">Novo certame</h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
              <label class="space-y-1">
                <span class="text-gov-slate-400">Número</span>
                <input class="w-full bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" [(ngModel)]="formCadastro.numeroLicitacao" />
              </label>
              <label class="space-y-1">
                <span class="text-gov-slate-400">Modalidade</span>
                <select class="w-full bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" [(ngModel)]="formCadastro.modalidade">
                  <option value="CONCORRENCIA_ELETRONICA">Concorrência eletrônica</option>
                  <option value="PREGAO_ELETRONICO">Pregão eletrônico</option>
                  <option value="DISPENSA">Dispensa</option>
                  <option value="INEXIGIBILIDADE">Inexigibilidade</option>
                </select>
              </label>
              <label class="space-y-1">
                <span class="text-gov-slate-400">Valor estimado (SPA)</span>
                <input type="number" class="w-full bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" [(ngModel)]="formCadastro.valorEstimado" />
              </label>
            </div>
            <label class="space-y-1 block">
              <span class="text-gov-slate-400">Objeto</span>
              <input class="w-full bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" [(ngModel)]="formCadastro.objeto" />
            </label>
            <button type="button" (click)="cadastrar()" class="px-4 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold" [disabled]="carregando()">
              Cadastrar licitação
            </button>
          </div>

          @if (licitacoes().length === 0) {
            <p class="text-gov-slate-400">Nenhum certame vinculado a este convênio.</p>
          }

          @for (item of licitacoes(); track item.id) {
            <article class="p-4 rounded-xl border border-white/10 bg-white/[0.02] space-y-3">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 class="text-sm font-bold text-white">{{ item.numeroLicitacao }}</h3>
                  <p class="text-gov-slate-400">{{ item.objeto }}</p>
                </div>
                <div class="flex flex-wrap gap-2">
                  <span class="px-2 py-0.5 rounded-full border text-[10px] font-bold" [class]="classeVrpl(item.statusVrpl)">VRPL {{ rotuloVrpl(item.statusVrpl) }}</span>
                  <span class="px-2 py-0.5 rounded-full border text-[10px] font-bold" [class]="classeAio(item.statusAio)">AIO {{ item.statusAio === 'EMITIDO' ? 'EMITIDA' : item.statusAio }}</span>
                </div>
              </div>

              <div class="grid grid-cols-2 md:grid-cols-4 gap-2 text-gov-slate-300">
                <div>Estimado: <strong class="text-white">{{ item.valorEstimado | number:'1.2-2' }}</strong></div>
                <div>Homologado: <strong class="text-white">{{ item.valorHomologado ? (item.valorHomologado | number:'1.2-2') : '—' }}</strong></div>
                <div>Economia: <strong class="text-white">{{ item.percentualDesconto != null ? item.percentualDesconto + '%' : '—' }}</strong></div>
                <div>Protocolo VRPL: <strong class="text-white">{{ item.numeroVrplTransferegov || '—' }}</strong></div>
              </div>

              @if (!item.podeCriarMedicao) {
                <p class="text-amber-300 bg-amber-950/40 border border-amber-500/30 rounded-lg px-3 py-2">
                  TravaAntigrosa ativa: medições bloqueadas até a AIO emitida.
                </p>
              } @else {
                <p class="text-emerald-300 bg-emerald-950/40 border border-emerald-500/30 rounded-lg px-3 py-2">
                  AIO {{ item.numeroAio }} em {{ item.dataEmissaoAio }} — medições liberadas.
                </p>
              }

              @if (item.situacao !== 'HOMOLOGADA') {
                <div class="grid grid-cols-1 md:grid-cols-4 gap-2">
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" placeholder="Valor homologado" type="number" [(ngModel)]="formsHomologacao[item.id].valorHomologado" />
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" type="date" [(ngModel)]="formsHomologacao[item.id].dataHomologacao" />
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" placeholder="CNPJ vencedor" [(ngModel)]="formsHomologacao[item.id].cnpjVencedor" />
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" placeholder="Razão social" [(ngModel)]="formsHomologacao[item.id].razaoSocialVencedor" />
                </div>
                <button type="button" class="px-3 py-1.5 rounded bg-white/10 text-white" (click)="homologar(item)">Homologar</button>
              }

              @if (item.situacao === 'HOMOLOGADA' && item.statusVrpl === 'NAO_ENVIADO') {
                <div class="grid grid-cols-1 md:grid-cols-3 gap-2">
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" placeholder="Protocolo VRPL" [(ngModel)]="formsVrpl[item.id].numeroVrplTransferegov" />
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" type="date" [(ngModel)]="formsVrpl[item.id].dataEnvioVrpl" />
                  <button type="button" class="px-3 py-1.5 rounded bg-gov-cobalt-600 text-white" (click)="submeterVrpl(item)">Submeter VRPL</button>
                </div>
              }

              @if (item.statusVrpl === 'EM_ANALISE_CAIXA' || item.statusVrpl === 'DILIGENCIA') {
                <textarea class="w-full bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" rows="2" placeholder="Parecer técnico da Caixa" [(ngModel)]="formsResultado[item.id].parecerTecnico"></textarea>
                <div class="flex flex-wrap gap-2">
                  <button type="button" class="px-3 py-1.5 rounded bg-emerald-700 text-white" (click)="registrarResultado(item, 'ACEITO_HOMOLOGADO')">Aceitar VRPL</button>
                  <button type="button" class="px-3 py-1.5 rounded bg-amber-700 text-white" (click)="registrarResultado(item, 'DILIGENCIA')">Diligência</button>
                  <button type="button" class="px-3 py-1.5 rounded bg-rose-700 text-white" (click)="registrarResultado(item, 'REJEITADO')">Rejeitar</button>
                </div>
              }

              @if (item.statusVrpl === 'ACEITO_HOMOLOGADO' && item.statusAio !== 'EMITIDO') {
                <div class="grid grid-cols-1 md:grid-cols-3 gap-2">
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" placeholder="Número da AIO" [(ngModel)]="formsAio[item.id].numeroAio" />
                  <input class="bg-[#111827] border border-white/10 rounded-lg px-3 py-2 text-white" type="date" [(ngModel)]="formsAio[item.id].dataEmissaoAio" />
                  <button type="button" class="px-3 py-1.5 rounded bg-emerald-600 text-white" (click)="emitirAio(item)">Emitir AIO</button>
                </div>
              }
            </article>
          }
        </div>
      </div>
    </div>
  `
})
export class LicitacoesFase3ModalComponent implements OnInit {
  @Input({ required: true }) convenio!: ConvenioCockpit;
  @Output() fechar = new EventEmitter<void>();
  @Output() atualizado = new EventEmitter<Licitacao[]>();

  private readonly service = inject(LicitacaoService);

  readonly licitacoes = signal<Licitacao[]>([]);
  readonly carregando = signal(false);
  readonly mensagemSucesso = signal<string | null>(null);
  readonly mensagemErro = signal<string | null>(null);

  formCadastro = {
    numeroLicitacao: '',
    modalidade: 'CONCORRENCIA_ELETRONICA' as ModalidadeLicitacao,
    criterioJulgamento: 'MENOR_PRECO' as CriterioJulgamento,
    regimeExecucao: 'EMPREITADA_PRECO_UNITARIO' as RegimeExecucao,
    objeto: '',
    valorEstimado: 0
  };

  formsHomologacao: Record<string, { valorHomologado: number; dataHomologacao: string; cnpjVencedor: string; razaoSocialVencedor: string }> = {};
  formsVrpl: Record<string, { numeroVrplTransferegov: string; dataEnvioVrpl: string }> = {};
  formsResultado: Record<string, { parecerTecnico: string }> = {};
  formsAio: Record<string, { numeroAio: string; dataEmissaoAio: string }> = {};

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.service.listar(this.convenio.id).subscribe({
      next: (itens) => {
        this.licitacoes.set(itens);
        itens.forEach((item) => this.garantirForms(item.id));
        this.carregando.set(false);
      },
      error: (err) => this.falhar(err)
    });
  }

  cadastrar(): void {
    this.limparMensagens();
    this.carregando.set(true);
    this.service.cadastrar(this.convenio.id, { ...this.formCadastro }).subscribe({
      next: () => {
        this.mensagemSucesso.set('Licitação cadastrada.');
        this.formCadastro.numeroLicitacao = '';
        this.formCadastro.objeto = '';
        this.formCadastro.valorEstimado = 0;
        this.carregar();
      },
      error: (err) => this.falhar(err)
    });
  }

  homologar(item: Licitacao): void {
    this.limparMensagens();
    const form = this.formsHomologacao[item.id];
    this.service.homologar(this.convenio.id, item.id, form).subscribe({
      next: () => {
        this.mensagemSucesso.set('Certame homologado. Economia calculada.');
        this.carregar();
      },
      error: (err) => this.falhar(err)
    });
  }

  submeterVrpl(item: Licitacao): void {
    this.limparMensagens();
    this.service.submeterVrpl(this.convenio.id, item.id, this.formsVrpl[item.id]).subscribe({
      next: () => {
        this.mensagemSucesso.set('VRPL submetido à Caixa.');
        this.carregar();
      },
      error: (err) => this.falhar(err)
    });
  }

  registrarResultado(item: Licitacao, resultado: StatusVrpl): void {
    this.limparMensagens();
    const parecer = this.formsResultado[item.id].parecerTecnico;
    this.service.registrarResultadoVrpl(this.convenio.id, item.id, {
      resultado,
      parecerTecnico: parecer,
      observacoesDiligencia: resultado === 'DILIGENCIA' ? parecer : undefined,
      dataAceiteVrpl: resultado === 'ACEITO_HOMOLOGADO' ? new Date().toISOString().slice(0, 10) : undefined
    }).subscribe({
      next: () => {
        this.mensagemSucesso.set('Resultado de VRPL registrado.');
        this.carregar();
      },
      error: (err) => this.falhar(err)
    });
  }

  emitirAio(item: Licitacao): void {
    this.limparMensagens();
    this.service.emitirAio(this.convenio.id, item.id, this.formsAio[item.id]).subscribe({
      next: (atualizada) => {
        this.mensagemSucesso.set('AIO emitida. TravaAntigrosa liberada para medições.');
        this.carregar();
        this.atualizado.emit([atualizada]);
      },
      error: (err) => this.falhar(err)
    });
  }

  classeVrpl(status: StatusVrpl): string {
    if (status === 'ACEITO_HOMOLOGADO') return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
    if (status === 'DILIGENCIA' || status === 'EM_ANALISE_CAIXA') return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
    if (status === 'REJEITADO') return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
    return 'bg-white/5 text-gov-slate-300 border-white/10';
  }

  classeAio(status: string): string {
    return status === 'EMITIDO'
      ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
      : 'bg-rose-500/20 text-rose-300 border-rose-500/30';
  }

  rotuloVrpl(status: StatusVrpl): string {
    return status.replaceAll('_', ' ');
  }

  private garantirForms(id: string): void {
    this.formsHomologacao[id] ??= { valorHomologado: 0, dataHomologacao: '', cnpjVencedor: '', razaoSocialVencedor: '' };
    this.formsVrpl[id] ??= { numeroVrplTransferegov: '', dataEnvioVrpl: '' };
    this.formsResultado[id] ??= { parecerTecnico: '' };
    this.formsAio[id] ??= { numeroAio: '', dataEmissaoAio: '' };
  }

  private falhar(err: { error?: { detail?: string; title?: string } }): void {
    this.carregando.set(false);
    this.mensagemErro.set(err?.error?.detail || err?.error?.title || 'Falha ao comunicar com o core-service.');
  }

  private limparMensagens(): void {
    this.mensagemSucesso.set(null);
    this.mensagemErro.set(null);
  }
}
