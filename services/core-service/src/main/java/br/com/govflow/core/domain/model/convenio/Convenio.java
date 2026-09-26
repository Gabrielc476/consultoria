package br.com.govflow.core.domain.model.convenio;

import br.com.govflow.core.domain.exception.ClausulaSuspensivaNaoPodeSerSuperadaException;
import br.com.govflow.core.domain.exception.RegraNegocioClausulaSuspensivaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Entidade raiz agregada de Convênio / Contrato de Repasse no core-service.
 */
public class Convenio {

    private final UUID id;
    private final UUID tenantId;
    private final UUID prefeituraId;
    private String numeroSiconv;
    private String numeroProcesso;
    private String orgaoConcedente;
    private String objeto;
    private BigDecimal valorGlobal;
    private BigDecimal valorRepasse;
    private BigDecimal valorContrapartida;
    private String situacao;
    private boolean possuiClausulaSuspensiva;
    private LocalDate prazoClausulaSuspensiva;
    private LocalDate dataInicioVigencia;
    private LocalDate dataFimVigencia;
    private boolean prorrogacaoSolicitada;
    private LocalDate novoPrazoProrrogado;
    private String s3KeyTermoRetiradaSuspensiva;
    private final Instant createdAt;
    private Instant updatedAt;

    public Convenio(UUID id,
                    UUID tenantId,
                    UUID prefeituraId,
                    String numeroSiconv,
                    String numeroProcesso,
                    String orgaoConcedente,
                    String objeto,
                    BigDecimal valorGlobal,
                    BigDecimal valorRepasse,
                    BigDecimal valorContrapartida,
                    String situacao,
                    boolean possuiClausulaSuspensiva,
                    LocalDate prazoClausulaSuspensiva,
                    LocalDate dataInicioVigencia,
                    LocalDate dataFimVigencia,
                    boolean prorrogacaoSolicitada,
                    LocalDate novoPrazoProrrogado,
                    String s3KeyTermoRetiradaSuspensiva,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "O id do convênio não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "O tenantId não pode ser nulo");
        this.prefeituraId = Objects.requireNonNull(prefeituraId, "O prefeituraId não pode ser nulo");
        this.numeroSiconv = Objects.requireNonNull(numeroSiconv, "O número SICONV não pode ser nulo");
        this.numeroProcesso = numeroProcesso;
        this.orgaoConcedente = Objects.requireNonNull(orgaoConcedente, "O órgão concedente não pode ser nulo");
        this.objeto = Objects.requireNonNull(objeto, "O objeto do convênio não pode ser nulo");
        this.valorGlobal = valorGlobal != null ? valorGlobal : BigDecimal.ZERO;
        this.valorRepasse = valorRepasse != null ? valorRepasse : BigDecimal.ZERO;
        this.valorContrapartida = valorContrapartida != null ? valorContrapartida : BigDecimal.ZERO;
        this.situacao = situacao != null ? situacao : "EM_EXECUCAO";
        this.possuiClausulaSuspensiva = possuiClausulaSuspensiva;
        this.prazoClausulaSuspensiva = prazoClausulaSuspensiva;
        this.dataInicioVigencia = dataInicioVigencia;
        this.dataFimVigencia = dataFimVigencia;
        this.prorrogacaoSolicitada = prorrogacaoSolicitada;
        this.novoPrazoProrrogado = novoPrazoProrrogado;
        this.s3KeyTermoRetiradaSuspensiva = s3KeyTermoRetiradaSuspensiva;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    /**
     * Retorna a data fatal efetiva da cláusula suspensiva (considerando prorrogação concedida, se houver).
     */
    public LocalDate getPrazoFatalEfetivo() {
        return novoPrazoProrrogado != null ? novoPrazoProrrogado : prazoClausulaSuspensiva;
    }

    /**
     * Calcula o total de dias restantes até o prazo fatal efetivo da cláusula suspensiva.
     */
    public long calcularDiasRestantes(LocalDate dataReferencia) {
        LocalDate prazoEfetivo = getPrazoFatalEfetivo();
        if (prazoEfetivo == null) {
            return 0;
        }
        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();
        return ChronoUnit.DAYS.between(ref, prazoEfetivo);
    }

    /**
     * Calcula a criticidade visual do cronômetro de 180 dias.
     */
    public CriticidadePrazoSuspensiva calcularCriticidadePrazo(LocalDate dataReferencia) {
        if (!possuiClausulaSuspensiva || isClausulaSuspensivaSuperada()) {
            return CriticidadePrazoSuspensiva.REGULAR;
        }

        long dias = calcularDiasRestantes(dataReferencia);
        if (dias < 0) {
            return CriticidadePrazoSuspensiva.EXPIRADO;
        }
        if (dias <= 30) {
            return CriticidadePrazoSuspensiva.CRITICO;
        }
        if (dias <= 90) {
            return CriticidadePrazoSuspensiva.ATENCAO;
        }
        return CriticidadePrazoSuspensiva.REGULAR;
    }

    /**
     * Indica se a cláusula suspensiva foi definitivamente superada com termo de retirada emitido.
     */
    public boolean isClausulaSuspensivaSuperada() {
        return s3KeyTermoRetiradaSuspensiva != null && !s3KeyTermoRetiradaSuspensiva.trim().isEmpty();
    }

    /**
     * Registra o protocolo de solicitação formal de prorrogação excepcional de prazo da Cláusula Suspensiva.
     */
    public void solicitarProrrogacaoPrazo(LocalDate novoPrazo, LocalDate dataReferencia) {
        if (!possuiClausulaSuspensiva) {
            throw new RegraNegocioClausulaSuspensivaException("Este convênio não opera sob regime de cláusula suspensiva.");
        }
        if (isClausulaSuspensivaSuperada()) {
            throw new RegraNegocioClausulaSuspensivaException("A Cláusula Suspensiva já foi superada e o convênio é plenamente eficaz.");
        }
        if (novoPrazo == null) {
            throw new RegraNegocioClausulaSuspensivaException("A nova data proposta para prorrogação é obrigatória.");
        }
        LocalDate prazoAtual = getPrazoFatalEfetivo();
        if (prazoAtual != null && !novoPrazo.isAfter(prazoAtual)) {
            throw new RegraNegocioClausulaSuspensivaException("O novo prazo prorrogado deve ser posterior ao prazo fatal atual (" + prazoAtual + ").");
        }

        this.prorrogacaoSolicitada = true;
        this.novoPrazoProrrogado = novoPrazo;
        this.updatedAt = Instant.now();
    }

    /**
     * Conclui a Fase 2 e supera a Cláusula Suspensiva mediante validação dos três pilares aprovados.
     */
    public void superarClausulaSuspensiva(String s3KeyTermoRetirada, List<CondicionanteSuspensiva> condicionantes) {
        if (!possuiClausulaSuspensiva) {
            throw new RegraNegocioClausulaSuspensivaException("Este convênio não opera sob regime de cláusula suspensiva.");
        }
        if (s3KeyTermoRetirada == null || s3KeyTermoRetirada.trim().isEmpty()) {
            throw new RegraNegocioClausulaSuspensivaException("A chave S3 do Termo de Retirada da Cláusula Suspensiva é obrigatória para a superação.");
        }

        // Verifica os 3 pilares obrigatórios
        List<TipoCondicionanteSuspensiva> pendentes = new ArrayList<>();
        Set<TipoCondicionanteSuspensiva> presentesEAprovados = new HashSet<>();

        if (condicionantes != null) {
            for (CondicionanteSuspensiva c : condicionantes) {
                if (c.isAprovado()) {
                    presentesEAprovados.add(c.getTipoCondicionante());
                }
            }
        }

        for (TipoCondicionanteSuspensiva pilarObrigatorio : TipoCondicionanteSuspensiva.values()) {
            if (!presentesEAprovados.contains(pilarObrigatorio)) {
                pendentes.add(pilarObrigatorio);
            }
        }

        if (!pendentes.isEmpty()) {
            throw new ClausulaSuspensivaNaoPodeSerSuperadaException(this.id, pendentes);
        }

        this.s3KeyTermoRetiradaSuspensiva = s3KeyTermoRetirada.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public String getNumeroSiconv() {
        return numeroSiconv;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public String getOrgaoConcedente() {
        return orgaoConcedente;
    }

    public String getObjeto() {
        return objeto;
    }

    public BigDecimal getValorGlobal() {
        return valorGlobal;
    }

    public BigDecimal getValorRepasse() {
        return valorRepasse;
    }

    public BigDecimal getValorContrapartida() {
        return valorContrapartida;
    }

    public String getSituacao() {
        return situacao;
    }

    public boolean isPossuiClausulaSuspensiva() {
        return possuiClausulaSuspensiva;
    }

    public LocalDate getPrazoClausulaSuspensiva() {
        return prazoClausulaSuspensiva;
    }

    public LocalDate getDataInicioVigencia() {
        return dataInicioVigencia;
    }

    public LocalDate getDataFimVigencia() {
        return dataFimVigencia;
    }

    public boolean isProrrogacaoSolicitada() {
        return prorrogacaoSolicitada;
    }

    public LocalDate getNovoPrazoProrrogado() {
        return novoPrazoProrrogado;
    }

    public String getS3KeyTermoRetiradaSuspensiva() {
        return s3KeyTermoRetiradaSuspensiva;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
