import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/auth/pages/login/login-page.component';
import { AppShellComponent } from './core/layout/app-shell/app-shell.component';
import { ConvenioCockpitPageComponent } from './features/convenios/pages/convenio-cockpit/convenio-cockpit-page.component';
import { ConveniosListPageComponent } from './features/convenios/pages/convenios-list/convenios-list-page.component';
import { WhatsAppHubPageComponent } from './features/whatsapp/pages/whatsapp-hub/whatsapp-hub-page.component';
import { DocumentosListPageComponent } from './features/revisao-documento/pages/documentos-list/documentos-list-page.component';
import { RadarCaucPageComponent } from './features/radar-cauc/pages/radar-cauc-page.component';
import { RevisaoDetalhePageComponent } from './features/revisao-documento/pages/revisao-detalhe/revisao-detalhe-page.component';

export const routes: Routes = [
  // 1. Autenticação (fora do AppShell)
  {
    path: 'login',
    component: LoginPageComponent
  },

  // 2. Bancada de Revisão Lado a Lado (Modo Foco / Zen Mode 100% Viewport)
  {
    path: 'documentos/:id/revisar',
    component: RevisaoDetalhePageComponent,
    canActivate: [authGuard]
  },

  // 3. Área Logada Unificada sob o AppShell (Sidebar + Header Contextual)
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'convenios',
        component: ConvenioCockpitPageComponent
      },
      {
        path: 'convenios/lista',
        component: ConveniosListPageComponent
      },
      {
        path: 'convenios/:id',
        component: ConvenioCockpitPageComponent
      },
      {
        path: 'whatsapp',
        component: WhatsAppHubPageComponent
      },
      {
        path: 'documentos',
        component: DocumentosListPageComponent
      },
      {
        path: 'radar-cauc',
        component: RadarCaucPageComponent
      },
      {
        path: 'radar-prazos',
        redirectTo: 'radar-cauc',
        pathMatch: 'full'
      },
      {
        path: '',
        redirectTo: 'convenios',
        pathMatch: 'full'
      }
    ]
  },

  // Fallback
  {
    path: '**',
    redirectTo: 'convenios'
  }
];
