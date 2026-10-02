# Relatório de Sessão Técnica: Máscaras Monetárias BRL e Correção de Decimais

**Data**: 01/10/2026  
**Contexto**: Tela de Auditoria Side-by-Side (`/documentos/{id}/revisar`) do GovFlow  
**Objetivo**: Implementação de máscaras em Real Brasileiro (`R$ 0,00`) e correção de inputs numéricos com suporte a decimais com vírgula (padrão pt-BR).

---

## 1. Problema Identificado

1. **Bug dos Inputs Numéricos Nativos (`<input type="number">`)**:
   - Em navegadores com locale pt-BR ou configurações padrão, `<input type="number">` sem `step="any"` ou lidando com vírgula `,` bloqueava a digitação ou retornava `NaN`.
   - O uso de `Number(valor)` em strings contendo vírgula (como `"3,5"`) resultava em `NaN`, forçando o fallback `NaN || 0` e zerando alíquotas ou valores digitados.

2. **Ausência de Formatação Visual de Moeda**:
   - Valores monetários como `182400.00` eram exibidos de forma crua (`182400`), dificultando a conferência visual do auditor contra a nota fiscal física.

3. **Incompatibilidade no Contrato de Retenções**:
   - O payload JSON retornado pela IA utilizava os campos `aliquota` e `valor`, enquanto a interface e formulário esperavam `aliquotaPercentual` e `valorRetido`, resultando em valores `0,00` na carga inicial da tabela de impostos.

---

## 2. Solução Implementada

### 2.1. Utilitários Numéricos Robustos (`frontend/src/app/shared/utils/number-utils.ts`)
- `parseNumberBr(value)`: Trata valores nulos, números puros e strings com prefixo `R$`, pontos de milhar e vírgulas decimais, convertendo de forma segura para `number`.
- `formatCurrencyBr(value, includePrefix)`: Formata números para o padrão monetário brasileiro (`R$ 182.400,00` ou `182.400,00`).
- `formatDecimalBr(value, maxDecimals)`: Formata números decimais com vírgula (ex: `3,5`).

### 2.2. Diretivas Standalone
- **`CurrencyMaskDirective` (`[appCurrencyMask]`)**:
  - Implementa `ControlValueAccessor` para integração perfeita com `ngModel`.
  - Exibe visualmente `R$ 182.400,00` com seleção automática no foco.
  - Emite para o modelo Angular (`ngModel`) o valor puro como `number` float, garantindo que cálculos de deduções e validações matemáticas permaneçam precisos.
- **`DecimalMaskDirective` (`[appDecimalMask]`)**:
  - Permite digitação natural de vírgulas `,` e pontos `.`.
  - Converte e sincroniza o valor decimal para o modelo como `number`.

### 2.3. Adaptação dos Formulários Arquetípicos
As máscaras foram aplicadas em todos os arquétipos do auditor side-by-side:
- **Medição de Engenharia (Fase 04)**: `valorBruto`, `valorAcumuladoAnterior`, `valorAcumuladoAtual`, `saldoContratual`.
- **Nota Fiscal / Documento Hábil (Fase 05)**: `valorBruto`, `valorLiquido`, `valorRetido` e `aliquotaPercentual` (tabela de retenções).
- **Termo Aditivo (Fase 06)**: `valorBruto` e `percentualAditamento`.
- **Prestação de Contas (Fases 07/08)**: `valorDevolvidoGru`.
- **Passivo TCE (Fase 09)**: `valorGlosaSelic`.
- **Universal / Agnóstico**: `valorBruto`.

---

## 3. Validação e Compilação
- Compilação realizada com sucesso: `npm run build` (código de saída 0).
- Pacote estático atualizado no container `govflow-frontend` com recarga do Nginx.
