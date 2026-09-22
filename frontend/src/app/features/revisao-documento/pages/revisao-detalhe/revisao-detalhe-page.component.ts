import { Component, OnInit, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { RevisaoApiService } from '../../services/revisao-api.service';
import { ReviewHeaderComponent } from '../../components/review-header/review-header.component';
import { MediaWorkspaceComponent } from '../../components/media-workspace/media-workspace.component';
import { ExtractionFormComponent } from '../../components/extraction-form/extraction-form.component';
import { QueueDrawerComponent } from '../../components/queue-drawer/queue-drawer.component';
import { RejectionDialogComponent } from '../../components/rejection-dialog/rejection-dialog.component';
import { ToastContainerComponent } from '../../../../shared/ui/toast/toast-container.component';

@Component({
  selector: 'app-revisao-detalhe-page',
  standalone: true,
  imports: [
    CommonModule,
    ReviewHeaderComponent,
    MediaWorkspaceComponent,
    ExtractionFormComponent,
    QueueDrawerComponent,
    RejectionDialogComponent,
    ToastContainerComponent
  ],
  template: `
    <div class="h-screen w-screen flex flex-col bg-gov-slate-950 overflow-hidden font-sans">
      <!-- Barra Superior Operacional -->
      <app-review-header></app-review-header>

      <!-- Painel Principal Lado a Lado (Split 50% / 50%) -->
      <main class="flex-1 flex overflow-hidden relative">
        <!-- Lado Esquerdo: Visualizador de PDF / Imagem com Bounding Boxes (50%) -->
        <section class="w-1/2 h-full border-r border-gov-slate-800 flex flex-col relative overflow-hidden">
          <app-media-workspace
            class="flex flex-col flex-1 h-full min-h-0 w-full overflow-hidden"
            [url]="state.arquivoBlobUrl() || ''"
            [mimeType]="state.documentoAtual()?.contentType"
            [filename]="state.documentoAtual()?.nomeArquivoOriginal"
            [boxes]="state.boundingBoxes()"
            [fieldScores]="state.confidenceScores()"
            [activeField]="state.campoEmFoco()"
            (fieldSelected)="onCaixaSelecionadaNoPdf($event)"
          >
          </app-media-workspace>
        </section>

        <!-- Lado Direito: Formulário Estruturado e Retenções (50%) -->
        <section class="w-1/2 h-full flex flex-col relative overflow-hidden">
          <app-extraction-form class="flex flex-col flex-1 h-full min-h-0 w-full overflow-hidden"></app-extraction-form>
        </section>

        <!-- Loading Overlay -->
        @if (state.carregando()) {
          <div class="absolute inset-0 bg-gov-slate-950/80 backdrop-blur-sm z-50 flex flex-col items-center justify-center text-white">
            <div class="w-12 h-12 border-4 border-gov-cobalt-500 border-t-transparent rounded-full animate-spin mb-4"></div>
            <p class="text-sm font-semibold tracking-wide text-gov-slate-200">Carregando dados fiscais e bounding boxes da IA...</p>
          </div>
        }
      </main>

      <!-- Gaveta Retrátil de Pendências (Queue Drawer) -->
      <app-queue-drawer></app-queue-drawer>

      <!-- Diálogo Modal de Rejeição -->
      <app-rejection-dialog></app-rejection-dialog>

      <!-- Notificações Flutuantes (Toasts) -->
      <app-toast-container></app-toast-container>
    </div>
  `
})
export class RevisaoDetalhePageComponent implements OnInit {
  public readonly state = inject(RevisaoStateService);
  private readonly api = inject(RevisaoApiService);
  private readonly route = inject(ActivatedRoute);

  public ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.state.carregarDocumento(id);
      }
    });

    this.state.carregarFila();
  }

  public urlArquivo(): string {
    return this.state.arquivoBlobUrl() || '';
  }

  public onCaixaSelecionadaNoPdf(campo: string): void {
    this.state.focarCampo(campo);

    // Scroll suave e foco no elemento do formulário
    const elementId = `input-${this.normalizarIdInput(campo)}`;
    const element = document.getElementById(elementId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      element.focus();
    }
  }

  @HostListener('window:keydown', ['$event'])
  public handleKeyboardShortcuts(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const isEditingText = target && (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'SELECT' ||
      target.isContentEditable
    );

    // Barra de Espaço: Aprovação Expressa (apenas se não estiver digitando em campo de texto)
    if ((event.code === 'Space' || event.key === ' ') && !isEditingText) {
      if (!this.state.salvando() && !this.state.modalRejeicaoAberto() && !this.state.drawerFilaAberto()) {
        event.preventDefault();
        this.state.aprovar();
        return;
      }
    }

    // Ctrl + Enter: Aprovar
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      if (!this.state.salvando()) {
        this.state.aprovar();
      }
    }

    // Alt + R: Rejeitar
    if (event.altKey && (event.key === 'r' || event.key === 'R')) {
      event.preventDefault();
      this.state.abrirModalRejeicao();
    }

    // Alt + S: Sincronizar Líquido
    if (event.altKey && (event.key === 's' || event.key === 'S')) {
      event.preventDefault();
      this.state.sincronizarValorLiquido();
    }

    // Alt + Q: Toggle Queue Drawer
    if (event.altKey && (event.key === 'q' || event.key === 'Q')) {
      event.preventDefault();
      this.state.toggleDrawer();
    }

    // Esc: Fechar modais
    if (event.key === 'Escape') {
      if (this.state.modalRejeicaoAberto()) {
        this.state.fecharModalRejeicao();
      } else if (this.state.drawerFilaAberto()) {
        this.state.toggleDrawer();
      }
    }
  }

  private normalizarIdInput(campo: string): string {
    const map: Record<string, string> = {
      numeroDocumento: 'numero',
      dataEmissao: 'data',
      cnpjCredor: 'cnpj',
      razaoSocialCredor: 'razao',
      numeroEmpenho: 'empenho'
    };
    return map[campo] || campo;
  }
}
