package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ExportarFicheiroZipUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportarFicheiroZipService implements ExportarFicheiroZipUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExportarFicheiroZipService.class);
    private static final int BUFFER_SIZE = 8192;

    private final ConvenioRepositoryPort convenioRepository;
    private final DocumentoRepositoryPort documentoRepository;
    private final DocumentoStoragePort storagePort;
    private final UsuarioRepositoryPort usuarioRepository;

    public ExportarFicheiroZipService(ConvenioRepositoryPort convenioRepository,
                                     DocumentoRepositoryPort documentoRepository,
                                     DocumentoStoragePort storagePort,
                                     UsuarioRepositoryPort usuarioRepository) {
        this.convenioRepository = convenioRepository;
        this.documentoRepository = documentoRepository;
        this.storagePort = storagePort;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void exportarConvenioIntegral(UUID convenioId, OutputStream outputStream) {
        Convenio convenio = buscarEValidarConvenio(convenioId);
        List<Documento> documentos = documentoRepository.listarPorConvenioId(convenioId).stream()
                .filter(d -> d.getStatus() != StatusDocumento.EXCLUIDO)
                .toList();

        gerarZipStreaming(convenio, documentos, outputStream);
    }

    @Override
    public void exportarFaseEspecifica(UUID convenioId, FaseCicloVida fase, OutputStream outputStream) {
        Convenio convenio = buscarEValidarConvenio(convenioId);
        List<Documento> documentos = documentoRepository.listarPorConvenioIdEFase(convenioId, fase).stream()
                .filter(d -> d.getStatus() != StatusDocumento.EXCLUIDO)
                .toList();

        gerarZipStreaming(convenio, documentos, outputStream);
    }

    private Convenio buscarEValidarConvenio(UUID convenioId) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));

        validarAcessoPrefeitura(convenio.getPrefeituraId());
        return convenio;
    }

    private void validarAcessoPrefeitura(UUID prefeituraId) {
        if (prefeituraId == null) return;

        if (UserContext.isAgente()) {
            if (UserContext.temAcessoPrefeitura(prefeituraId)) {
                return;
            }
            UUID userId = UserContext.getUserId();
            if (userId != null) {
                Set<UUID> atribuidas = usuarioRepository.buscarPrefeiturasAtribuidas(userId);
                if (atribuidas.contains(prefeituraId)) {
                    return;
                }
            }
            throw new AcessoNegadoException("Você não possui autorização para acessar os documentos deste convênio.");
        }
    }

    private void gerarZipStreaming(Convenio convenio, List<Documento> documentos, OutputStream outputStream) {
        String prefixoRaiz = "SICONV_" + (convenio.getNumeroSiconv() != null ? convenio.getNumeroSiconv().replaceAll("[^a-zA-Z0-9_-]", "_") : "CONVENIO");

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            byte[] buffer = new byte[BUFFER_SIZE];

            for (Documento doc : documentos) {
                if (doc.getS3Bucket() == null || doc.getS3Key() == null) {
                    continue;
                }

                Optional<InputStream> isOpt = storagePort.carregarArquivo(doc.getS3Bucket(), doc.getS3Key());
                if (isOpt.isEmpty()) {
                    log.warn("Arquivo físico não encontrado no storage para documento {}: key={}", doc.getId(), doc.getS3Key());
                    continue;
                }

                String pastaFase = doc.getFaseCicloVida() != null ? doc.getFaseCicloVida().getNomePasta() : "05_Execucao_Financeira_e_Pagamentos";
                String nomeArquivo = doc.getNomeArquivoOriginal() != null ? doc.getNomeArquivoOriginal() : (doc.getId() + ".pdf");
                String zipEntryPath = String.format("%s/%s/%s", prefixoRaiz, pastaFase, nomeArquivo);

                ZipEntry entry = new ZipEntry(zipEntryPath);
                if (doc.getTamanhoBytes() != null && doc.getTamanhoBytes() > 0) {
                    entry.setSize(doc.getTamanhoBytes());
                }
                zos.putNextEntry(entry);

                try (InputStream is = isOpt.get()) {
                    int bytesLidos;
                    while ((bytesLidos = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesLidos);
                    }
                }
                zos.closeEntry();
            }
            zos.finish();
            zos.flush();
        } catch (Exception e) {
            log.error("Erro durante a geração do ZIP via streaming para convênio {}", convenio.getId(), e);
            throw new RuntimeException("Falha na geração do arquivo ZIP do convênio: " + e.getMessage(), e);
        }
    }
}
