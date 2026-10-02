package br.com.govflow.core.domain.model.documento;

import br.com.govflow.core.domain.model.Documento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado / Visão Estrutural do Ficheiro Digital do Convênio, organizado nas 10 Fases.
 */
public class FicheiroDigital {

    public record PastaFase(
            FaseCicloVida fase,
            String codigoFase,
            String nomePasta,
            String descricao,
            int quantidadeArquivos,
            long tamanhoTotalBytes,
            List<Documento> documentos
    ) {
        public PastaFase {
            documentos = documentos != null ? Collections.unmodifiableList(documentos) : Collections.emptyList();
        }
    }

    private final UUID convenioId;
    private final UUID prefeituraId;
    private final UUID tenantId;
    private final String numeroSiconv;
    private final String objeto;
    private final List<PastaFase> fases;
    private final int totalArquivos;
    private final long tamanhoTotalBytes;

    public FicheiroDigital(UUID convenioId,
                           UUID prefeituraId,
                           UUID tenantId,
                           String numeroSiconv,
                           String objeto,
                           List<PastaFase> fases) {
        this.convenioId = convenioId;
        this.prefeituraId = prefeituraId;
        this.tenantId = tenantId;
        this.numeroSiconv = numeroSiconv;
        this.objeto = objeto;
        this.fases = fases != null ? Collections.unmodifiableList(new ArrayList<>(fases)) : Collections.emptyList();

        int contagem = 0;
        long bytes = 0;
        for (PastaFase pf : this.fases) {
            contagem += pf.quantidadeArquivos();
            bytes += pf.tamanhoTotalBytes();
        }
        this.totalArquivos = contagem;
        this.tamanhoTotalBytes = bytes;
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getNumeroSiconv() {
        return numeroSiconv;
    }

    public String getObjeto() {
        return objeto;
    }

    public List<PastaFase> getFases() {
        return fases;
    }

    public int getTotalArquivos() {
        return totalArquivos;
    }

    public long getTamanhoTotalBytes() {
        return tamanhoTotalBytes;
    }

    public PastaFase obterPasta(FaseCicloVida fase) {
        return fases.stream()
                .filter(p -> p.fase() == fase)
                .findFirst()
                .orElse(null);
    }
}
