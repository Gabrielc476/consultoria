import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/auth/pages/login/login-page.component';
import { RevisaoDetalhePageComponent } from './features/revisao-documento/pages/revisao-detalhe/revisao-detalhe-page.component';
import { DocumentosListPageComponent } from './features/revisao-documento/pages/documentos-list/documentos-list-page.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: 'documentos/:id/revisar',
    component: RevisaoDetalhePageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'documentos',
    component: DocumentosListPageComponent,
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'documentos',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'documentos'
  }
];
