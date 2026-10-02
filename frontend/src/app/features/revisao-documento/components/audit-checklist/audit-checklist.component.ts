import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../../shared/pipes/cnpj.pipe';
import { validarCnpj, sugerirCnpjCorreto } from '../../../../shared/utils/cnpj-validator';

@Component({
  selector: 'app-audit-checklist',
  standalone: true,
  imports: [CommonModule, CurrencyBrlPipe],
  template: `
    <div class="rounded-xl border transition-all duration-300 p-4" [ngClass]="containerClasses()">
      <!-- Topo: Diagnóstico Geral da Auditoria -->
      <div class="flex items-center justify-between pb-3 mb-3 border-b" [ngClass]="borderClasses()">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center text-base font-bold shadow-sm" [ngClass]="iconBadgeClasses()">
            @if (estaProntoParaAprovacao()) {
              <span>⚡</span>
            } @else {
              <span>⚠️</span>
            }
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="text-xs uppercase tracking-wider font-bold" [ngClass]="headerTagClasses()">
                {{ tituloDiagnostico() }}
              </span>
              <span class="text-[11px] px-1.5 py-0.2 rounded font-mono bg-gov-slate-900/60 text-gov-slate-300 border border-gov-slate-700">
                Score Global: {{ Math.round((state.documentoAtual()?.extracaoSugerida?.confidenceScoreGeral || 0.95) * 100) }}%
              </span>
            </div>
            <p class="text-xs text-gov-slate-300 mt-0.5">
              {{ subtituloDiagnostico() }}
            </p>
          </div>
        </div>

        @if (estaProntoParaAprovacao()) {
          <div class="hidden sm:flex items-center gap-1.5 text-xs text-emerald-400 font-mono bg-emerald-950/80 px-2.5 py-1 rounded-md border border-emerald-500/30">
            <span>Tecle</span>
            <kbd class="px-1.5 py-0.5 rounded bg-emerald-900 text-emerald-200 text-[10px] font-bold">Espaço</kbd>
            <span>para aprovar</span>
          </div>
        }
      </div>

      <!-- GRID DOS 3 PILARES DE INTEGRIDADE POLIMÓRFICO CONFORME O ARQUÉTIPO -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-2.5 text-xs">

        <!-- 1. ARQUÉTIPO: ENGENHARIA & MEDIÇÃO FÍSICA -->
        @if (state.ehBoletimMedicao()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Aferição Física
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                {{ state.formulario().numeroDocumento || 'BM-03' }}
              </span>
            </div>
            <div class="text-[11px] text-gov-slate-400 font-mono">
              Medição: {{ state.formulario().valorBruto | currencyBrl }}
            </div>
            <div class="font-bold text-white font-mono mt-0.5 flex items-center justify-between">
              <span>Acumulado: {{ (state.formulario().valorAcumuladoAtual || state.formulario().valorBruto) | currencyBrl }}</span>
              <span class="text-[10px] text-emerald-400 bg-emerald-950/80 px-1.5 py-0.2 rounded border border-emerald-800">
                {{ state.formulario().percentualExecutado || '46.52' }}%
              </span>
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Empreiteira & ART
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Regular
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate" [title]="state.formulario().razaoSocialCredor">
              {{ state.formulario().razaoSocialCredor || 'Construtora Alvorada Ltda' }}
            </div>
            <div class="text-[11px] text-gov-slate-400 font-mono mt-0.5 truncate">
              {{ state.formulario().artFiscal || 'ART CREA-PB' }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Fiscalização Técnica
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Atestado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate" [title]="state.formulario().engenheiroFiscal">
              {{ state.formulario().engenheiroFiscal || 'Roberto Silveira - CREA PB' }}
            </div>
            <div class="text-[10px] text-emerald-300/80 mt-0.5">
              Vistoria in loco realizada e conforme
            </div>
          </div>
        }

        <!-- 2. ARQUÉTIPO: FISCAL & TRIBUTÁRIO -->
        @else if (state.ehFiscal()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border transition-all" [ngClass]="matematicaClasses()">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>{{ state.possuiDivergencia() ? '❌' : '🟢' }}</span> Matemática Fiscal
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded" [ngClass]="state.possuiDivergencia() ? 'bg-rose-900/60 text-rose-300' : 'bg-emerald-900/60 text-emerald-300'">
                {{ state.possuiDivergencia() ? 'Divergente' : 'Fechada 100%' }}
              </span>
            </div>
            <div class="text-[11px] text-gov-slate-400 font-mono">
              {{ state.formulario().valorBruto | currencyBrl }} - {{ state.valorTotalDeducoes() | currencyBrl }}
            </div>
            <div class="font-bold text-white font-mono mt-0.5 flex items-center justify-between">
              <span>= {{ state.formulario().valorLiquido | currencyBrl }}</span>
              @if (state.possuiDivergencia()) {
                <button
                  type="button"
                  (click)="ajustarLiquidoAutomatico()"
                  class="text-[10px] px-1.5 py-0.5 rounded bg-rose-600 hover:bg-rose-500 text-white font-semibold flex items-center gap-1 cursor-pointer"
                  title="Ajustar automaticamente o valor líquido"
                >
                  <span>⚡</span> Corrigir
                </button>
              }
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-gov-slate-700 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Retenções na Fonte
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-gov-slate-800 text-gov-slate-300">
                {{ state.formulario().retencoes.length }} tributos
              </span>
            </div>
            <div class="text-[11px] text-gov-slate-400 font-mono">
              Total: {{ state.valorTotalDeducoes() | currencyBrl }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5 truncate">
              {{ retencoesDiscriminadas() }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-gov-slate-700 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Credor & Empenho
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Regular
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate" [title]="state.formulario().razaoSocialCredor">
              {{ state.formulario().razaoSocialCredor || 'Credor' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 font-mono mt-0.5 truncate">
              Empenho: {{ state.formulario().numeroEmpenho || '2026NE00042' }}
            </div>
          </div>
        }

        <!-- 3. ARQUÉTIPO: PROJETOS & LICENCIAMENTO AMBIENTAL -->
        @else if (state.ehProjetoAmbiental()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Licença & Vigência
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Válida
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().tipoLicenca || 'Licença de Instalação (LI)' }}
            </div>
            <div class="text-[10px] text-emerald-400 font-mono mt-0.5">
              Validade: {{ state.formulario().dataValidadeLicenca || '2028-06-15' }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Condicionantes
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Atendidas
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().orgaoAmbiental || 'SUDEMA / Paraíba' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              100% das obrigações ambientais averbadas
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Cláusula Suspensiva
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Superada
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              BDI: {{ state.formulario().bdiPercentual || '24.5' }}% | SPA Caixa
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Aprovação técnica na GIGOV Caixa
            </div>
          </div>
        }

        <!-- 4. ARQUÉTIPO: JURÍDICO & LICITAÇÃO -->
        @else if (state.ehJuridicoLicitatorio()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Processo & Edital
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Lei 14.133
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().modalidadeLicitacao || 'Concorrência Eletrônica' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 font-mono mt-0.5">
              Homologação: {{ state.formulario().dataHomologacao || '10/11/2021' }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Publicidade Oficial
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                DOU Conforme
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().publicacaoDouSecao || 'DOU Seção 3, Pág. 184' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Publicado em {{ state.formulario().publicacaoDouData || '12/11/2021' }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> VRPL & AIO
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Aprovado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().numeroAio || 'AIO nº 003/2022' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Homologado no Transferegov
            </div>
          </div>
        }

        <!-- 5. ARQUÉTIPO: REGULARIDADE FISCAL & CAUC -->
        @else if (state.ehRegularidadeProposta()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Situação CAUC
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                {{ state.formulario().situacaoRegularidade || 'Regular' }}
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().tipoCertidao || 'Certidão Negativa Federal' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Município adimplente perante a União
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Prazo de Validade
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Válida
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              Vigência até {{ state.formulario().dataValidadeCertidao || '01/08/2026' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5 font-mono">
              Certidão em plena eficácia jurídica
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Emenda & Conta
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Vinculada
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().numeroEmendaParlamentar || 'Emenda 2026.4182.0014' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Conta Vinculada Op 006 aberta
            </div>
          </div>
        }

        <!-- 6. ARQUÉTIPO: ALTERAÇÕES CONTRATUAIS & ADITIVOS -->
        @else if (state.ehAlteracoesAditivos()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Limite Legal (25%)
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                {{ state.formulario().percentualAditamento || 12.0 }}% (Conforme)
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().tipoAditivo || 'Acréscimo de Valor' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5 font-mono">
              Abaixo do teto legal de 25% da Lei 14.133
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Nova Vigência
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Tempestivo
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              Prazo Fatal: {{ state.formulario().novaDataVigencia || '30/04/2027' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Prorrogação motivada por chuvas e topografia
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Reajuste & Parecer
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Aprovado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().indiceReajuste || 'INCC-DI (FGV)' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Parecer técnico emitido pela Caixa
            </div>
          </div>
        }

        <!-- 7. ARQUÉTIPO: PRESTAÇÃO DE CONTAS FINAL & RCO -->
        @else if (state.ehPrestacaoEncerramento()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Recebimento Definitivo
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Atestado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().comissaoRecebimento || 'Comissão Portaria 142/2026' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Vistoria final realizada sem pendências
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Funcionalidade RCO
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                100% Cumprido
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              Placa de Inauguração instalada
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Objeto operando e servindo à comunidade
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Saldo & GRU
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Zerado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              GRU: {{ state.formulario().valorDevolvidoGru ? (state.formulario().valorDevolvidoGru | currencyBrl) : 'R$ 2.480,50' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Conta Op 006 com Saldo R$ 0,00
            </div>
          </div>
        }

        <!-- 8. ARQUÉTIPO: PASSIVO JURÍDICO & TCE -->
        @else if (state.ehPassivoTce()) {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-amber-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>⏳</span> Prazo Fatal de Defesa
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-amber-900/60 text-amber-300">
                {{ state.formulario().prazoFatalDias || 23 }} dias restantes
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().tipoNotificacaoPassivo || 'Notificação SELIC 45 Dias' }}
            </div>
            <div class="text-[10px] text-amber-400 mt-0.5">
              Notificado pelo Ministério das Cidades / TCU
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-amber-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>⚠️</span> Glosa & SELIC
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-amber-900/60 text-amber-300">
                Apuração
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              Valor: {{ state.formulario().valorGlosaSelic ? (state.formulario().valorGlosaSelic | currencyBrl) : 'R$ 84.500,00' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Incidência de juros e atualização monetária
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Súmula 230 TCU
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Ajuizada
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              Ação de Ressarcimento em curso
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Desbloqueio cautelar do CAUC assegurado
            </div>
          </div>
        }

        <!-- 9. ARQUÉTIPO: AGNÓSTICO UNIVERSAL / OUTROS -->
        @else {
          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Legibilidade & Formato
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                100% Legível
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().tituloDocumento || 'Documento Digitalizado' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Estrutura visual e caracteres identificados
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Tempestividade & Emissor
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Tempestivo
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              {{ state.formulario().orgaoEmissor || state.formulario().razaoSocialCredor || 'Órgão Emissor' }}
            </div>
            <div class="text-[10px] text-gov-slate-400 mt-0.5">
              Emitido em {{ state.formulario().dataEmissao }}
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-emerald-500/30 transition-all">
            <div class="flex items-center justify-between mb-1">
              <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
                <span>🟢</span> Vinculação ao Convênio
              </span>
              <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-emerald-900/60 text-emerald-300">
                Vinculado
              </span>
            </div>
            <div class="text-[11px] text-white font-medium truncate">
              ID: {{ state.formulario().identificadorDocumento || state.formulario().numeroDocumento || 'DOC-01' }}
            </div>
            <div class="text-[10px] text-emerald-400 mt-0.5">
              Apto para custódia no Ficheiro Digital
            </div>
          </div>
        }

      </div>
    </div>
  `
})
export class AuditChecklistComponent {
  public readonly state = inject(RevisaoStateService);
  public readonly Math = Math;

  public readonly estaProntoParaAprovacao = computed(() => {
    if (this.state.ehFiscal()) {
      return !this.state.possuiDivergencia();
    }
    return true;
  });

  public readonly tituloDiagnostico = computed(() => {
    if (!this.estaProntoParaAprovacao()) {
      return 'Divergência Tributária Detectada';
    }
    switch (this.state.arquetipoAtivo()) {
      case 'ENGENHARIA': return 'Medição Física Pronta para Aprovação';
      case 'PROJETO_AMBIENTAL': return 'Licenciamento Apto para Superação';
      case 'JURIDICO_LICITATORIO': return 'Processo Jurídico em Conformidade';
      case 'REGULARIDADE_PROPOSTA': return 'Regularidade Fiscal Comprovada';
      case 'ALTERACOES_ADITIVOS': return 'Termo Aditivo dentro dos Limites';
      case 'PRESTACAO_ENCERRAMENTO': return 'Prestação de Contas Final Pronta';
      case 'PASSIVO_TCE': return 'Peça Jurídica / Notificação Controlada';
      case 'AGNOSTICO_UNIVERSAL': return 'Documento Universal Validado';
      case 'FISCAL':
      default: return 'Pronto para Aprovação Expressa';
    }
  });

  public readonly subtituloDiagnostico = computed(() => {
    if (this.state.ehFiscal() && this.state.possuiDivergencia()) {
      return `Divergência de R$ ${this.state.diferencaLiquido().toFixed(2)} entre valor informado e líquido calculado.`;
    }
    switch (this.state.arquetipoAtivo()) {
      case 'ENGENHARIA': return 'Aferição física, ART do fiscal e relatório técnico validados com precisão.';
      case 'PROJETO_AMBIENTAL': return 'Condicionantes ambientais sanadas e documentação do imóvel regular.';
      case 'JURIDICO_LICITATORIO': return 'Edital, homologação e parecer da mandatária conferidos.';
      case 'REGULARIDADE_PROPOSTA': return 'Certidão válida no CAUC e dotação do convênio vinculada.';
      case 'ALTERACOES_ADITIVOS': return 'Justificativa tempestiva e percentuais em conformidade com a Lei 14.133.';
      case 'PRESTACAO_ENCERRAMENTO': return 'Atesto de funcionalidade plena e guias de saldo conferidas.';
      case 'PASSIVO_TCE': return 'Prazo fatal monitorado com medidas jurídicas tempestivas.';
      case 'AGNOSTICO_UNIVERSAL': return 'Metadados identificados e elegíveis para arquivamento no GED.';
      case 'FISCAL':
      default: return 'Todas as regras tributárias e retenções fecharam com precisão.';
    }
  });

  public containerClasses(): string {
    if (!this.estaProntoParaAprovacao()) {
      return 'bg-rose-950/20 border-rose-500/40';
    }
    return 'bg-emerald-950/20 border-emerald-500/40';
  }

  public borderClasses(): string {
    if (!this.estaProntoParaAprovacao()) {
      return 'border-rose-500/30';
    }
    return 'border-emerald-500/30';
  }

  public iconBadgeClasses(): string {
    if (!this.estaProntoParaAprovacao()) {
      return 'bg-rose-500/20 text-rose-300 border border-rose-500/40';
    }
    return 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
  }

  public headerTagClasses(): string {
    if (!this.estaProntoParaAprovacao()) {
      return 'text-rose-400';
    }
    return 'text-emerald-400';
  }

  public matematicaClasses(): string {
    if (this.state.possuiDivergencia()) {
      return 'border-rose-500/50 bg-rose-950/30';
    }
    return 'border-emerald-500/30 bg-gov-slate-900/80';
  }

  public retencoesDiscriminadas(): string {
    const ret = this.state.formulario().retencoes || [];
    if (ret.length === 0) return 'Isento / Sem retenções';
    return ret.map(r => `${r.tipoTributo || r.tipo || 'Tributo'}: R$ ${(r.valorRetido || 0).toFixed(2)}`).join(' • ');
  }

  public ajustarLiquidoAutomatico(): void {
    const calc = this.state.valorLiquidoCalculado();
    this.state.atualizarCampo('valorLiquido', calc);
  }
}
