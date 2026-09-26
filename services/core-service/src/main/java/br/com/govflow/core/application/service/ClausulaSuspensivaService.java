package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.*;
import br.com.govflow.core.application.port.out.ClausulaSuspensivaEventPublisherPort;
import br.com.govflow.core.application.port.out.ClausulaSuspensivaStoragePort;
import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.event.AlertaPrazoSuspensivaEvent;
import br.com.govflow.core.domain.event.ClausulaSuspensivaSuperadaEvent;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.convenio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClausulaSuspensivaService implements
        ConsultarClausulaSuspensivaUseCase,
        GerenciarCondicionanteUseCase,
        ProrrogarPrazoClausulaSuspensivaUseCase,
        SuperarClausulaSuspensivaUseCase,
        UploadDocumentoCondicionanteUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClausulaSuspensivaService.class);

    private final ConvenioRepositoryPort convenioRepository;
    private final CondicionanteSuspensivaRepositoryPort condicionanteRepository;
    private final ClausulaSuspensivaStoragePort storagePort;
    private final ClausulaSuspensivaEventPublisherPort eventPublisher;

    @Value("${govflow.storage.bucket-documentos:govflow-documentos}")
    private String bucketDocumentos;

    public ClausulaSuspensivaService(ConvenioRepositoryPort convenioRepository,
                                    CondicionanteSuspensivaRepositoryPort condicionanteRepository,
                                    ClausulaSuspensivaStoragePort storagePort,
                                    ClausulaSuspensivaEventPublisherPort eventPublisher) {
        this.convenioRepository = convenioRepository;
        this.condicionanteRepository = condicionanteRepository;
        this.storagePort = storagePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DossieClausulaSuspensivaDto obterDossiePorConvenioId(UUID convenioId) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));
        return montarDossie(convenio);
    }

    @Override
    public DossieClausulaSuspensivaDto obterDossiePorNumeroSiconv(String numeroSiconv) {
        Convenio convenio = convenioRepository.buscarPorNumeroSiconv(numeroSiconv)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(numeroSiconv));
        return montarDossie(convenio);
    }

    @Override
    public CondicionanteSuspensiva submeterParaAnaliseCaixa(UUID convenioId, TipoCondicionanteSuspensiva tipo) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, tipo);
        cond.submeterParaAnaliseCaixa();
        log.info("Condicionante {} do convênio {} submetida para análise técnica da Caixa GIGOV", tipo, convenioId);
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public CondicionanteSuspensiva registrarDiligenciaCaixa(RegistrarDiligenciaCommand command) {
        Convenio convenio = buscarConvenioOuFalhar(command.convenioId());
        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, command.tipo());
        cond.registrarDiligencia(
                command.observacoes(),
                command.s3KeyLaudoPendencias(),
                command.dataLimiteSaneamento(),
                LocalDate.now()
        );
        log.info("Diligência registrada para condicionante {} do convênio {}. Prazo limite saneamento: {}",
                command.tipo(), command.convenioId(), command.dataLimiteSaneamento());
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public CondicionanteSuspensiva aprovarCondicionante(AprovarCondicionanteCommand command) {
        Convenio convenio = buscarConvenioOuFalhar(command.convenioId());
        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, command.tipo());
        cond.aprovar(
                command.numeroDocumentoComprobatorio(),
                command.dataAprovacao(),
                command.dataValidade(),
                command.valorOrcamentoAprovado(),
                command.percentualBdiAprovado(),
                command.numeroArtRrt(),
                command.orgaoEmissor(),
                command.s3KeyDocumento()
        );
        log.info("Condicionante {} do convênio {} APROVADA pela Caixa. Doc: {}",
                command.tipo(), command.convenioId(), command.numeroDocumentoComprobatorio());
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public CondicionanteSuspensiva atualizarParametrosTecnicos(AtualizarCondicionanteCommand command) {
        Convenio convenio = buscarConvenioOuFalhar(command.convenioId());
        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, command.tipo());
        cond.atualizarParametrosTecnicos(
                command.numeroDocumentoComprobatorio(),
                command.dataValidade(),
                command.orgaoEmissor(),
                command.valorOrcamentoAprovado(),
                command.percentualBdiAprovado(),
                command.numeroArtRrt(),
                command.observacoes()
        );
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public Convenio solicitarProrrogacao(UUID convenioId, LocalDate novoPrazo) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        convenio.solicitarProrrogacaoPrazo(novoPrazo, LocalDate.now());
        log.info("Solicitação de prorrogação protocolada para convênio {}. Novo prazo fatal: {}", convenioId, novoPrazo);
        return convenioRepository.salvar(convenio);
    }

    @Override
    public Convenio superarClausulaSuspensiva(UUID convenioId, String s3KeyTermoRetirada) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        List<CondicionanteSuspensiva> pilares = sincronizarTresPilares(convenio);

        convenio.superarClausulaSuspensiva(s3KeyTermoRetirada, pilares);
        Convenio salvo = convenioRepository.salvar(convenio);

        log.info("CLÁUSULA SUSPENSIVA SUPERADA COM SUCESSO para o convênio {} ({})! Termo: {}",
                salvo.getId(), salvo.getNumeroSiconv(), s3KeyTermoRetirada);

        // Dispara evento para liberar Fase 3 (Licitações)
        eventPublisher.publicarSuperacao(new ClausulaSuspensivaSuperadaEvent(
                salvo.getTenantId(),
                salvo.getId(),
                salvo.getNumeroSiconv(),
                LocalDate.now(),
                s3KeyTermoRetirada
        ));

        return salvo;
    }

    @Override
    public CondicionanteSuspensiva uploadDocumentoComprobatorio(UUID convenioId,
                                                               TipoCondicionanteSuspensiva tipo,
                                                               String nomeArquivo,
                                                               String contentType,
                                                               byte[] conteudo) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        String s3Key = String.format("clausula-suspensiva/%s/%s/%s/%s",
                convenio.getTenantId(), convenioId, tipo.name(), sanitizarNomeArquivo(nomeArquivo));

        storagePort.salvarArquivo(bucketDocumentos, s3Key, conteudo, contentType);

        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, tipo);
        cond.vincularDocumento(s3Key);
        log.info("Documento comprobatório armazenado no MinIO para condicionante {}: {}", tipo, s3Key);
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public CondicionanteSuspensiva uploadLaudoPendencias(UUID convenioId,
                                                        TipoCondicionanteSuspensiva tipo,
                                                        String nomeArquivo,
                                                        String contentType,
                                                        byte[] conteudo) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        String s3Key = String.format("clausula-suspensiva/%s/%s/%s/diligencias/%s",
                convenio.getTenantId(), convenioId, tipo.name(), sanitizarNomeArquivo(nomeArquivo));

        storagePort.salvarArquivo(bucketDocumentos, s3Key, conteudo, contentType);

        CondicionanteSuspensiva cond = obterOuCriarCondicionante(convenio, tipo);
        cond.registrarDiligencia(
                cond.getObservacoesAnaliseCaixa() != null ? cond.getObservacoesAnaliseCaixa() : "Laudo de Pendências anexado",
                s3Key,
                cond.getDataLimiteSaneamento(),
                LocalDate.now()
        );
        return condicionanteRepository.salvar(cond);
    }

    @Override
    public Convenio uploadTermoRetirada(UUID convenioId,
                                        String nomeArquivo,
                                        String contentType,
                                        byte[] conteudo) {
        Convenio convenio = buscarConvenioOuFalhar(convenioId);
        String s3Key = String.format("clausula-suspensiva/%s/%s/termo-retirada/%s",
                convenio.getTenantId(), convenioId, sanitizarNomeArquivo(nomeArquivo));

        storagePort.salvarArquivo(bucketDocumentos, s3Key, conteudo, contentType);
        return superarClausulaSuspensiva(convenioId, s3Key);
    }

    private DossieClausulaSuspensivaDto montarDossie(Convenio convenio) {
        List<CondicionanteSuspensiva> pilares = sincronizarTresPilares(convenio);

        LocalDate hoje = LocalDate.now();
        long diasRestantes = convenio.calcularDiasRestantes(hoje);
        CriticidadePrazoSuspensiva criticidade = convenio.calcularCriticidadePrazo(hoje);

        // Se estiver em risco crítico e ainda não superado, publica alerta
        if (criticidade == CriticidadePrazoSuspensiva.CRITICO && !convenio.isClausulaSuspensivaSuperada()) {
            eventPublisher.publicarAlertaPrazo(new AlertaPrazoSuspensivaEvent(
                    convenio.getTenantId(),
                    convenio.getId(),
                    convenio.getNumeroSiconv(),
                    diasRestantes,
                    criticidade
            ));
        }

        List<ItemCondicionanteDto> itensDto = pilares.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return new DossieClausulaSuspensivaDto(
                convenio.getId(),
                convenio.getPrefeituraId(),
                convenio.getNumeroSiconv(),
                convenio.getNumeroProcesso(),
                convenio.getOrgaoConcedente(),
                convenio.getObjeto(),
                convenio.getValorGlobal(),
                convenio.getValorRepasse(),
                convenio.getValorContrapartida(),
                convenio.isPossuiClausulaSuspensiva(),
                convenio.getPrazoClausulaSuspensiva(),
                convenio.isProrrogacaoSolicitada(),
                convenio.getNovoPrazoProrrogado(),
                convenio.getPrazoFatalEfetivo(),
                diasRestantes,
                criticidade,
                convenio.isClausulaSuspensivaSuperada(),
                convenio.getS3KeyTermoRetiradaSuspensiva(),
                itensDto
        );
    }

    private ItemCondicionanteDto toItemDto(CondicionanteSuspensiva c) {
        return new ItemCondicionanteDto(
                c.getId(),
                c.getTipoCondicionante(),
                c.getTipoCondicionante().getDescricao(),
                c.getStatus(),
                c.getStatus().getDescricao(),
                c.getNumeroDocumentoComprobatorio(),
                c.getDataAprovacao(),
                c.getDataValidade(),
                c.getObservacoesAnaliseCaixa(),
                c.getS3KeyDocumento(),
                c.getDataLimiteSaneamento(),
                c.getS3KeyLaudoPendencias(),
                c.getValorOrcamentoAprovadoCaixa(),
                c.getPercentualBdiAprovado(),
                c.getNumeroArtRrt(),
                c.getOrgaoEmissor()
        );
    }

    private List<CondicionanteSuspensiva> sincronizarTresPilares(Convenio convenio) {
        List<CondicionanteSuspensiva> existentes = condicionanteRepository.buscarPorConvenioId(convenio.getId());
        Map<TipoCondicionanteSuspensiva, CondicionanteSuspensiva> mapa = existentes.stream()
                .collect(Collectors.toMap(CondicionanteSuspensiva::getTipoCondicionante, c -> c));

        List<CondicionanteSuspensiva> novas = new ArrayList<>();
        for (TipoCondicionanteSuspensiva tipo : TipoCondicionanteSuspensiva.values()) {
            if (!mapa.containsKey(tipo)) {
                CondicionanteSuspensiva nova = CondicionanteSuspensiva.nova(convenio.getTenantId(), convenio.getId(), tipo);
                novas.add(nova);
                mapa.put(tipo, nova);
            }
        }

        if (!novas.isEmpty()) {
            condicionanteRepository.salvarTodas(novas);
            log.info("Auto-seeding de {} pilares condicionantes da Caixa para o convênio {}", novas.size(), convenio.getId());
        }

        // Ordenação canônica dos 3 pilares
        return Arrays.stream(TipoCondicionanteSuspensiva.values())
                .map(mapa::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CondicionanteSuspensiva obterOuCriarCondicionante(Convenio convenio, TipoCondicionanteSuspensiva tipo) {
        return condicionanteRepository.buscarPorConvenioETipo(convenio.getId(), tipo)
                .orElseGet(() -> {
                    CondicionanteSuspensiva nova = CondicionanteSuspensiva.nova(convenio.getTenantId(), convenio.getId(), tipo);
                    return condicionanteRepository.salvar(nova);
                });
    }

    private Convenio buscarConvenioOuFalhar(UUID convenioId) {
        return convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));
    }

    private String sanitizarNomeArquivo(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "documento_" + System.currentTimeMillis() + ".pdf";
        }
        return nomeOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
