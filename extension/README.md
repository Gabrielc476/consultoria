# Extensão GovFlow — Copiloto Transferegov

Content script Manifest V3. Detecta a tela **Incluir Documento Hábil**, busca documentos `PRONTO_PARA_TRANSFEREGOV` em `GET http://localhost:8080/api/v1/documentos` e injeta o painel em Shadow DOM fechado.

## Instalar no Chrome

```powershell
cd extension
npm install
npm run build
```

Em `chrome://extensions`, ative o modo desenvolvedor e carregue a pasta `extension/dist`.

Entre pelo popup da extensão (login do gateway). O token de modo demo do Angular não é aceito.

## Fixture local

```powershell
npx serve fixtures -p 4173
```

Abra `http://localhost:4173/incluir-documento-habil.html`. O painel só aparece nesse arquivo (ou no host `transferegov.sistema.gov.br` quando o heading é a tela de documento hábil).

## Testes

```powershell
npm test
```
