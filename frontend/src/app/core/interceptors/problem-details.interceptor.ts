import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../ui/toast.service';

export const problemDetailsInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        if (!req.url.includes('/auth/login')) {
          toastService.aviso('Sessão expirada', 'Por favor, realize login novamente.');
          authService.logout();
        }
      } else if (error.status === 422 || error.status === 400) {
        const problem = error.error;
        const msg = problem?.detail || problem?.message || 'Dados inválidos na requisição.';
        toastService.erro('Erro de Validação', msg);
      } else if (error.status >= 500) {
        toastService.erro('Falha no Servidor', 'Ocorreu um erro interno de processamento.');
      }

      return throwError(() => error);
    })
  );
};
