const form = document.getElementById('login-form') as HTMLFormElement;
const emailInput = document.getElementById('email') as HTMLInputElement;
const senhaInput = document.getElementById('senha') as HTMLInputElement;
const sair = document.getElementById('sair') as HTMLButtonElement;
const statusEl = document.getElementById('status') as HTMLParagraphElement;

interface SessaoPopup {
  autenticado?: boolean;
  email?: string | null;
  nome?: string | null;
  ok?: boolean;
  erro?: string;
}

function mostrar(sessao: SessaoPopup): void {
  if (sessao.autenticado) {
    form.hidden = true;
    sair.hidden = false;
    statusEl.textContent = `Conectado${sessao.nome ? ` como ${sessao.nome}` : ''}${sessao.email ? ` (${sessao.email})` : ''}.`;
    return;
  }
  form.hidden = false;
  sair.hidden = true;
  statusEl.textContent = sessao.erro ?? 'Sem sessão. O modo demo do Angular não autentica aqui.';
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  statusEl.textContent = 'Entrando…';
  void chrome.runtime.sendMessage({
    type: 'LOGIN',
    email: emailInput.value.trim(),
    senha: senhaInput.value
  }).then((resposta: SessaoPopup) => mostrar(resposta));
});

sair.addEventListener('click', () => {
  void chrome.runtime.sendMessage({ type: 'LOGOUT' }).then(() => {
    senhaInput.value = '';
    mostrar({ autenticado: false });
  });
});

void chrome.runtime.sendMessage({ type: 'AUTH_STATUS' }).then((resposta: SessaoPopup) => mostrar(resposta ?? { autenticado: false }));
