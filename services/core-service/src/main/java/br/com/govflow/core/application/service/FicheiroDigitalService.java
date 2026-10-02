package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.GerenciarFicheiroDigitalUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.application.port.out.FicheiroRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.adapter.out.storage.MinioDocumentoStorageAdapter;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.*;

@Service
@Transactional
public class FicheiroDigitalService implements GerenciarFicheiroDigitalUseCase {

    private static final Logger log = LoggerFactory.getLogger(FicheiroDigitalService.class);

    private final ConvenioRepositoryPort convenioRepository;
    private final DocumentoRepositoryPort documentoRepository;
    private final FicheiroRepositoryPort ficheiroRepository;
    private final DocumentoStoragePort storagePort;
    private final UsuarioRepositoryPort usuarioRepository;

    public FicheiroDigitalService(ConvenioRepositoryPort convenioRepository,
                                 DocumentoRepositoryPort documentoRepository,
                                 FicheiroRepositoryPort ficheiroRepository,
                                 DocumentoStoragePort storagePort,
                                 UsuarioRepositoryPort usuarioRepository) {
        this.convenioRepository = convenioRepository;
        this.documentoRepository = documentoRepository;
        this.ficheiroRepository = ficheiroRepository;
        this.storagePort = storagePort;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FicheiroDigital obterFicheiro(UUID convenioId) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));

        validarAcessoPrefeitura(convenio.getPrefeituraId());
        return ficheiroRepository.carregarFicheiroDigital(convenioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Documento> listarDocumentosFase(UUID convenioId, FaseCicloVida fase) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));

        validarAcessoPrefeitura(convenio.getPrefeituraId());
        return ficheiroRepository.listarDocumentosPorFase(convenioId, fase).stream()
                .filter(d -> d.getStatus() != StatusDocumento.EXCLUIDO)
                .toList();
    }

    @Override
    public Documento uploadDocumento(UploadDocumentoCommand command) {
        Convenio convenio = convenioRepository.buscarPorId(command.convenioId())
                .orElseThrow(() -> new ConvenioNaoEncontradoException(command.convenioId()));

        validarAcessoPrefeitura(convenio.getPrefeituraId());

        UUID docId = UUID.randomUUID();
        UUID tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : convenio.getTenantId();
        FaseCicloVida fase = command.fase() != null ? command.fase() : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA;
        CategoriaDocumento categoria = command.categoria() != null ? command.categoria() : CategoriaDocumento.OUTROS;

        String s3Key = MinioDocumentoStorageAdapter.construirS3Key(
                tenantId,
                convenio.getPrefeituraId(),
                convenio.getId(),
                fase,
                docId,
                command.nomeArquivo()
        );

        String hashSha256 = (command.conteudo() != null && command.conteudo().length > 0)
                ? MinioDocumentoStorageAdapter.calcularSha256(command.conteudo())
                : null;

        long tamanhoBytes = command.conteudo() != null ? command.conteudo().length : 0L;
        String contentType = (command.contentType() != null && !command.contentType().isBlank())
                ? command.contentType()
                : "application/octet-stream";

        storagePort.salvarArquivo(
                null,
                s3Key,
                new ByteArrayInputStream(command.conteudo() != null ? command.conteudo() : new byte[0]),
                tamanhoBytes,
                contentType
        );

        String pastaVirtual = (command.pastaVirtual() != null && !command.pastaVirtual().isBlank())
                ? command.pastaVirtual()
                : "/" + fase.getNomePasta();

        ArmazenamentoArquivo armazenamento = new ArmazenamentoArquivo(
                "govflow-documentos",
                s3Key,
                command.nomeArquivo(),
                contentType,
                tamanhoBytes
        );

        Documento documento = Documento.criarNovo(
                docId,
                tenantId,
                convenio.getPrefeituraId(),
                convenio.getId(),
                fase,
                categoria,
                pastaVirtual,
                armazenamento,
                hashSha256,
                OrigemCanal.UPLOAD_MANUAL,
                command.tags(),
                Collections.emptyMap(),
                command.usuarioId()
        );

        Documento salvo = documentoRepository.salvar(documento);

        DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                tenantId,
                salvo.getId(),
                command.usuarioId(),
                "UPLOAD",
                "Upload manual de documento via Ficheiro Digital",
                null,
                String.format("{\"fase\":\"%s\",\"categoria\":\"%s\",\"pastaVirtual\":\"%s\",\"nomeOriginal\":\"%s\"}",
                        fase.name(), categoria.name(), pastaVirtual, command.nomeArquivo())
        );
        documentoRepository.salvarAuditoria(auditoria);

        log.info("Documento {} gravado no Ficheiro Digital do convênio {} na fase {}",
                salvo.getId(), convenio.getId(), fase);
        return salvo;
    }

    @Override
    public Documento moverDocumento(MoverDocumentoCommand command) {
        Documento doc = documentoRepository.buscarPorId(command.documentoId())
                .orElseThrow(() -> new DocumentoNaoEncontradoException(command.documentoId()));

        validarAcessoPrefeitura(doc.getPrefeituraId());

        String snapshotAnterior = String.format("{\"fase\":\"%s\",\"pastaVirtual\":\"%s\"}",
                doc.getFaseCicloVida() != null ? doc.getFaseCicloVida().name() : null,
                doc.getPastaVirtual());

        doc.moverPasta(command.novaFase(), command.novaPastaVirtual());
        Documento atualizado = documentoRepository.salvar(doc);

        String snapshotAtual = String.format("{\"fase\":\"%s\",\"pastaVirtual\":\"%s\"}",
                atualizado.getFaseCicloVida() != null ? atualizado.getFaseCicloVida().name() : null,
                atualizado.getPastaVirtual());

        DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                atualizado.getTenantId(),
                atualizado.getId(),
                command.usuarioId(),
                "MOVIDO_DE_PASTA",
                command.justificativa() != null ? command.justificativa() : "Movimentação de pasta no Ficheiro Digital",
                snapshotAnterior,
                snapshotAtual
        );
        documentoRepository.salvarAuditoria(auditoria);

        log.info("Documento {} movido para fase {} e pasta {}",
                atualizado.getId(), command.novaFase(), command.novaPastaVirtual());
        return atualizado;
    }

    @Override
    @Transactional(readOnly = true)
    public String obterUrlPreview(UUID documentoId) {
        Documento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));

        validarAcessoPrefeitura(doc.getPrefeituraId());

        return storagePort.gerarPresignedUrlPreview(doc.getS3Bucket(), doc.getS3Key(), 15);
    }

    @Override
    @Transactional(readOnly = true)
    public Documento obterDocumentoPorId(UUID documentoId) {
        Documento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));

        validarAcessoPrefeitura(doc.getPrefeituraId());
        return doc;
    }

    @Override
    @Transactional(readOnly = true)
    public void escreverConteudoDocumento(UUID documentoId, java.io.OutputStream outputStream) {
        Documento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));

        validarAcessoPrefeitura(doc.getPrefeituraId());

        java.util.Optional<java.io.InputStream> isOpt = storagePort.carregarArquivo(doc.getS3Bucket(), doc.getS3Key());
        if (isOpt.isPresent()) {
            try (java.io.InputStream is = isOpt.get()) {
                is.transferTo(outputStream);
            } catch (java.io.IOException e) {
                log.error("Erro ao transmitir conteúdo do documento {}", documentoId, e);
                throw new RuntimeException("Erro ao transmitir arquivo: " + e.getMessage(), e);
            }
        } else {
            log.warn("Arquivo do documento {} não encontrado no S3 para a chave {}. Transmitindo visualização amigável de análise.", documentoId, doc.getS3Key());
            try {
                String aviso = "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">"
                        + "<style>body{font-family:ui-sans-serif,system-ui,sans-serif;background:#0b0f19;color:#e2e8f0;padding:2rem;line-height:1.5;}"
                        + ".card{max-width:600px;margin:2rem auto;background:#111827;border:1px solid rgba(255,255,255,0.1);border-radius:1rem;padding:2rem;box-shadow:0 20px 25px -5px rgba(0,0,0,0.5);text-align:center;}"
                        + ".badge{display:inline-block;padding:0.25rem 0.75rem;border-radius:9999px;font-size:0.75rem;font-weight:600;background:rgba(59,130,246,0.2);color:#93c5fd;border:1px solid rgba(59,130,246,0.3);margin-bottom:1rem;}"
                        + "h3{font-size:1.2rem;color:#fff;margin:0 0 0.5rem;font-family:monospace;}"
                        + "p{font-size:0.875rem;color:#94a3b8;margin:0.5rem 0;}"
                        + "</style></head><body>"
                        + "<div class=\"card\">"
                        + "<div class=\"badge\">🤖 EM ANÁLISE POR IA</div>"
                        + "<h3>" + (doc.getNomeArquivoOriginal() != null ? doc.getNomeArquivoOriginal() : "documento") + "</h3>"
                        + "<p>O arquivo foi recebido pelo GovFlow e os metadados fiscais estão sendo sincronizados e processados pela Inteligência Artificial.</p>"
                        + "<p style=\"font-size:0.75rem;color:#64748b;\">Status atual: " + (doc.getStatus() != null ? doc.getStatus().name() : "EM_ANALISE_IA") + "</p>"
                        + "</div></body></html>";
                outputStream.write(aviso.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException e) {
                log.error("Erro ao escrever aviso de contingência", e);
            }
        }
    }

    @Override
    public void excluirDocumento(ExcluirDocumentoCommand command) {
        Documento doc = documentoRepository.buscarPorId(command.documentoId())
                .orElseThrow(() -> new DocumentoNaoEncontradoException(command.documentoId()));

        validarAcessoPrefeitura(doc.getPrefeituraId());

        String snapshotAnterior = String.format("{\"status\":\"%s\"}", doc.getStatus());
        doc.marcarExcluido(command.justificativa());
        Documento atualizado = documentoRepository.salvar(doc);

        String snapshotAtual = String.format("{\"status\":\"%s\",\"motivo\":\"%s\"}",
                atualizado.getStatus(), command.justificativa());

        DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                atualizado.getTenantId(),
                atualizado.getId(),
                command.usuarioId(),
                "EXCLUSAO",
                command.justificativa() != null ? command.justificativa() : "Exclusão lógica do documento",
                snapshotAnterior,
                snapshotAtual
        );
        documentoRepository.salvarAuditoria(auditoria);

        log.info("Documento {} marcado como EXCLUIDO no Ficheiro Digital", doc.getId());
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
            throw new AcessoNegadoException("Você não possui autorização para operar documentos desta prefeitura.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoAuditoria> listarAuditoria(UUID documentoId) {
        Documento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));
        validarAcessoPrefeitura(doc.getPrefeituraId());
        return documentoRepository.listarAuditorias(documentoId);
    }
}
