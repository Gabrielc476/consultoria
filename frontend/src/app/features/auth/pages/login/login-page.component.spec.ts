import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginPageComponent } from './login-page.component';
import { AuthService } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';

describe('LoginPageComponent', () => {
  let component: LoginPageComponent;
  let fixture: ComponentFixture<LoginPageComponent>;
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
      imports: [LoginPageComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPageComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('deve inicializar na aba de LOGIN e conter link para página de cadastro', () => {
    expect(component).toBeTruthy();
    expect(component.aba()).toBe('LOGIN');
    expect(component.carregando()).toBeFalse();
    expect(component.erro()).toBeNull();

    const compiled = fixture.nativeElement as HTMLElement;
    const linkCadastro = compiled.querySelector('a[routerLink="/cadastro"]');
    expect(linkCadastro).toBeTruthy();
  });

  it('deve alternar para a aba REGISTER', () => {
    component.aba.set('REGISTER');
    expect(component.aba()).toBe('REGISTER');
  });

  it('deve formatar CNPJ com máscara ao digitar', () => {
    const event = {
      target: { value: '13519354000199' }
    } as unknown as Event;

    component.aplicarMascaraCnpj(event);
    expect(component.regCnpj).toBe('13.519.354/0001-99');
  });

  it('deve autenticar analista com sucesso e navegar para convenios', () => {
    authService.login.and.returnValue(of({
      token: 'jwt-token',
      tokenType: 'Bearer',
      analistaId: 'u1',
      nome: 'Analista',
      email: 'analista@govflow.com.br',
      tenantId: 'tenant-123'
    }));

    component.email = 'analista@govflow.com.br';
    component.senha = 'govflow123';
    component.onSubmitLogin();

    expect(authService.login).toHaveBeenCalledWith('analista@govflow.com.br', 'govflow123');
    expect(toastService.sucesso).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/convenios']);
  });

  it('deve cadastrar consultoria (onboarding) com sucesso e navegar', () => {
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

  it('deve ativar o modo demo ao acionar entrarDemo()', () => {
    component.entrarDemo();
    expect(authService.entrarModoDemo).toHaveBeenCalled();
    expect(toastService.sucesso).toHaveBeenCalled();
  });
});
