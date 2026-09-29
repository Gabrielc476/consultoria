import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { RegisterPageComponent } from './register-page.component';
import { AuthService } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';

describe('RegisterPageComponent', () => {
  let component: RegisterPageComponent;
  let fixture: ComponentFixture<RegisterPageComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    const authSpy = jasmine.createSpyObj('AuthService', [
      'login',
      'cadastrarConsultoria',
      'entrarModoDemo',
      'logout'
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['sucesso', 'erro', 'alerta', 'info']);

    await TestBed.configureTestingModule({
      imports: [RegisterPageComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPageComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('deve inicializar a página de cadastro', () => {
    expect(component).toBeTruthy();
    expect(component.carregando()).toBeFalse();
    expect(component.erro()).toBeNull();
  });

  it('deve aplicar máscara ao CNPJ digitado', () => {
    const event = {
      target: { value: '13519354000199' }
    } as unknown as Event;

    component.aplicarMascaraCnpj(event);
    expect(component.regCnpj).toBe('13.519.354/0001-99');
  });

  it('deve validar campos obrigatórios antes do envio', () => {
    component.regRazaoSocial = '';
    component.onSubmitRegister();

    expect(component.erro()).toContain('Preencha todos os campos obrigatórios');
    expect(authService.cadastrarConsultoria).not.toHaveBeenCalled();
  });

  it('deve validar tamanho mínimo da senha', () => {
    component.regRazaoSocial = 'Planeja Brasil';
    component.regCnpj = '13.519.354/0001-99';
    component.regNomeAnalista = 'Carlos Lima';
    component.regEmailAnalista = 'carlos@planeja.com';
    component.regSenha = '123';

    component.onSubmitRegister();

    expect(component.erro()).toContain('A senha deve ter no mínimo 6 caracteres');
    expect(authService.cadastrarConsultoria).not.toHaveBeenCalled();
  });

  it('deve cadastrar consultoria com sucesso e navegar para /convenios', () => {
    authService.cadastrarConsultoria.and.returnValue(of({
      token: 'jwt-token-onboarding',
      tokenType: 'Bearer',
      analistaId: 'analista-id',
      nome: 'Carlos Lima',
      email: 'carlos@planeja.com',
      tenantId: 'tenant-123'
    }));

    component.regRazaoSocial = 'Planeja Brasil';
    component.regCnpj = '13.519.354/0001-99';
    component.regNomeAnalista = 'Carlos Lima';
    component.regEmailAnalista = 'carlos@planeja.com';
    component.regSenha = 'senhaSegura123';

    component.onSubmitRegister();

    expect(authService.cadastrarConsultoria).toHaveBeenCalledWith(jasmine.objectContaining({
      razaoSocial: 'Planeja Brasil',
      cnpj: '13.519.354/0001-99',
      nomeAdministrador: 'Carlos Lima',
      emailAdministrador: 'carlos@planeja.com',
      senha: 'senhaSegura123',
      plano: 'PRO'
    }));
    expect(toastService.sucesso).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/convenios']);
  });

  it('deve permitir acesso via modo demo', () => {
    component.entrarDemo();
    expect(authService.entrarModoDemo).toHaveBeenCalled();
    expect(toastService.sucesso).toHaveBeenCalled();
  });
});
