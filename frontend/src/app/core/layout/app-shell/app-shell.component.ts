import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, HeaderComponent],
  template: `
    <div class="h-screen w-screen overflow-hidden bg-[#0A0E17] text-gov-slate-100 flex font-sans">
      <!-- Barra Lateral Fixa -->
      <app-sidebar></app-sidebar>

      <!-- Painel Principal de Trabalho -->
      <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        <!-- Topo Global com Seletor de Município e Busca -->
        <app-header></app-header>

        <!-- Área de Conteúdo da Rota -->
        <main class="flex-1 overflow-y-auto bg-[#0A0E17]">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class AppShellComponent {}
