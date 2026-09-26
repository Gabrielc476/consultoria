package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AtualizarCertidaoCaucUseCase;
import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase;
import br.com.govflow.core.application.port.out.CaucEventPublisherPort;
import br.com.govflow.core.application.port.out.CertidaoCaucRepositoryPort;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.domain.event.AlertaCertidaoCaucEvent;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import br.com.govflow.core.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaucService implements ConsultarCaucUseCase, AtualizarCertidaoCaucUseCase, AvaliarConformidadeCaucUseCase {

    private static final Logger log = LoggerFactory.getLogger(CaucService.class);

    private final CertidaoCaucRepositoryPort certidaoRepository;
    private final PrefeituraRepositoryPort prefeituraRepository;
    private final CaucEventPublisherPort eventPublisher;

    public CaucService(CertidaoCaucRepositoryPort certidaoRepository,
                       PrefeituraRepositoryPort prefeituraRepository,
                       CaucEventPublisherPort eventPublisher) {
        this.certidaoRepository = certidaoRepository;
        this.prefeituraRepository = prefeituraRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DossieCaucDto obterDossiePrefeitura(UUID prefeituraId) {
        Prefeitura prefeitura = prefeituraRepository.buscarPorId(prefeituraId)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(prefeituraId));

        List<CertidaoCauc> certidoes = sincronizarDezesseisCertidoes(prefeitura);

        int regulares = 0;
        int alertas = 0;
        int vencidas = 0;

        List<ItemCertidaoDto> items = new ArrayList<>();
        for (CertidaoCauc cert : certidoes) {
            if (cert.getSituacao() == StatusCertidao.VENCIDA) {
                vencidas++;
            } else if (cert.getSituacao() == StatusCertidao.ALERTA) {
                alertas++;
            } else {
                regulares++;
            }
            items.add(toItemDto(cert));
        }

        return new DossieCaucDto(
                prefeitura.getId(),
                prefeitura.getNomeMunicipio(),
                prefeitura.getUf().name(),
                prefeitura.getCnpj().getFormatted(),
                prefeitura.getStatusCauc(),
                regulares,
                alertas,
                vencidas,
                items
        );
    }

    @Override
    public ResumoCaucDto obterResumoConsultoria(UUID tenantId) {
        List<Prefeitura> prefeituras = prefeituraRepository.listar(0, 1000, true);

        int totalRegulares = 0;
        int totalAlerta = 0;
        int totalVencidas = 0;
        List<MunicipioRiscoDto> municipios = new ArrayList<>();

        for (Prefeitura pref : prefeituras) {
            List<CertidaoCauc> certidoes = sincronizarDezesseisCertidoes(pref);

            int prefRegulares = 0;
            int prefAlerta = 0;
            int prefVencidas = 0;
            List<ItemCertidaoDto> itemDtos = new ArrayList<>();

            for (CertidaoCauc cert : certidoes) {
                if (cert.getSituacao() == StatusCertidao.VENCIDA) {
                    prefVencidas++;
                } else if (cert.getSituacao() == StatusCertidao.ALERTA) {
                    prefAlerta++;
                } else {
                    prefRegulares++;
                }
                itemDtos.add(toItemDto(cert));
            }

            totalRegulares += prefRegulares;
            totalAlerta += prefAlerta;
            totalVencidas += prefVencidas;

            // Determina próximo prazo fatal dentre as certidões
            PrazoFatalDto prazoFatal = calcularProximoPrazoFatal(certidoes);

            municipios.add(new MunicipioRiscoDto(
                    pref.getId(),
                    pref.getNomeMunicipio(),
                    pref.getUf().name(),
                    3, // Convênios monitorados
                    prefRegulares,
                    prefAlerta,
                    prefVencidas,
                    prazoFatal,
                    itemDtos
            ));
        }

        return new ResumoCaucDto(
                prefeituras.size(),
                totalRegulares,
                totalAlerta,
                totalVencidas,
                municipios
        );
    }

    @Override
    public CertidaoCauc cadastrarOuAtualizarCertidao(CadastrarCertidaoCommand command) {
        Prefeitura prefeitura = prefeituraRepository.buscarPorId(command.prefeituraId())
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(command.prefeituraId()));

        Optional<CertidaoCauc> existenteOpt = certidaoRepository.buscarPorPrefeituraIdETipoExigencia(
                command.prefeituraId(), command.tipoExigencia());

        CertidaoCauc certidao;
        if (existenteOpt.isPresent()) {
            certidao = existenteOpt.get();
            certidao.atualizarDados(
                    command.numeroCertidao(),
                    command.dataEmissao(),
                    command.dataValidade(),
                    command.s3KeyComprovante(),
                    command.situacaoForcada(),
                    LocalDate.now()
            );
        } else {
            certidao = new CertidaoCauc(
                    UUID.randomUUID(),
                    command.tenantId() != null ? command.tenantId() : prefeitura.getTenantId(),
                    prefeitura.getId(),
                    command.tipoExigencia(),
                    command.numeroCertidao(),
                    command.dataEmissao(),
                    command.dataValidade(),
                    command.situacaoForcada(),
                    null,
                    command.s3KeyComprovante(),
                    null,
                    null
            );
            if (command.situacaoForcada() == null) {
                certidao.reavaliarSituacao(LocalDate.now());
            }
        }

        CertidaoCauc salva = certidaoRepository.salvar(certidao);

        // Atualiza status geral da prefeitura
        atualizarStatusPrefeituraAposCertidao(prefeitura);

        // Emite alerta se crítico
        if (salva.isCriticaParaAlerta()) {
            dispararAlerta(prefeitura, salva);
        }

        return salva;
    }

    @Override
    public ResultadoAvaliacaoDto avaliarTodasPrefeituras(UUID tenantId) {
        List<Prefeitura> prefeituras = prefeituraRepository.listar(0, 1000, true);
        return executarAvaliacao(prefeituras);
    }

    @Override
    public ResultadoAvaliacaoDto avaliarTodasPrefeiturasGlobal() {
        List<Prefeitura> prefeituras = prefeituraRepository.listar(0, 1000, true);
        return executarAvaliacao(prefeituras);
    }

    @Override
    public ResultadoAvaliacaoDto avaliarPrefeitura(UUID prefeituraId) {
        Prefeitura prefeitura = prefeituraRepository.buscarPorId(prefeituraId)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(prefeituraId));
        return executarAvaliacao(List.of(prefeitura));
    }

    private ResultadoAvaliacaoDto executarAvaliacao(List<Prefeitura> prefeituras) {
        int totalCertidoesAvaliadas = 0;
        int totalCertidoesEmAlerta = 0;
        int totalCertidoesVencidas = 0;
        int totalAlertasDisparados = 0;
        int prefeiturasBloqueadas = 0;

        LocalDate hoje = LocalDate.now();

        for (Prefeitura pref : prefeituras) {
            List<CertidaoCauc> certidoes = sincronizarDezesseisCertidoes(pref);
            boolean possuiVencida = false;

            for (CertidaoCauc cert : certidoes) {
                cert.reavaliarSituacao(hoje);
                certidaoRepository.salvar(cert);
                totalCertidoesAvaliadas++;

                if (cert.getSituacao() == StatusCertidao.VENCIDA) {
                    possuiVencida = true;
                    totalCertidoesVencidas++;
                } else if (cert.getSituacao() == StatusCertidao.ALERTA) {
                    totalCertidoesEmAlerta++;
                }

                if (cert.isCriticaParaAlerta()) {
                    dispararAlerta(pref, cert);
                    totalAlertasDisparados++;
                }
            }

            if (possuiVencida) {
                if (pref.getStatusCauc() != StatusCauc.BLOQUEADO) {
                    pref.atualizarStatusCauc(StatusCauc.BLOQUEADO);
                    prefeituraRepository.salvar(pref);
                }
                prefeiturasBloqueadas++;
            } else {
                if (pref.getStatusCauc() != StatusCauc.ADIMPLENTE) {
                    pref.atualizarStatusCauc(StatusCauc.ADIMPLENTE);
                    prefeituraRepository.salvar(pref);
                }
            }
        }

        log.info("Avaliação de conformidade CAUC concluída: {} prefeituras, {} certidões, {} em alerta, {} vencidas, {} alertas enviados, {} bloqueadas.",
                prefeituras.size(), totalCertidoesAvaliadas, totalCertidoesEmAlerta, totalCertidoesVencidas, totalAlertasDisparados, prefeiturasBloqueadas);

        return new ResultadoAvaliacaoDto(
                prefeituras.size(),
                totalCertidoesAvaliadas,
                totalCertidoesEmAlerta,
                totalCertidoesVencidas,
                totalAlertasDisparados,
                prefeiturasBloqueadas
        );
    }

    /**
     * Garante a presença e integridade das 16 certidões oficiais para a prefeitura.
     * Caso alguma ainda não exista no banco, é semeada com validade inicial padrão de 90 dias.
     */
    private List<CertidaoCauc> sincronizarDezesseisCertidoes(Prefeitura prefeitura) {
        List<CertidaoCauc> existentes = certidaoRepository.listarPorPrefeituraId(prefeitura.getId());
        Set<TipoExigenciaCauc> tiposExistentes = existentes.stream()
                .map(CertidaoCauc::getTipoExigencia)
                .collect(Collectors.toSet());

        List<CertidaoCauc> atualizadas = new ArrayList<>(existentes);
        LocalDate hoje = LocalDate.now();

        for (TipoExigenciaCauc tipo : TipoExigenciaCauc.values()) {
            if (!tiposExistentes.contains(tipo)) {
                CertidaoCauc nova = CertidaoCauc.criarPadrao(prefeitura.getTenantId(), prefeitura.getId(), tipo, hoje);
                CertidaoCauc salva = certidaoRepository.salvar(nova);
                atualizadas.add(salva);
            }
        }

        atualizadas.sort(Comparator.comparing(c -> c.getTipoExigencia().getCodigo()));
        return atualizadas;
    }

    private void atualizarStatusPrefeituraAposCertidao(Prefeitura prefeitura) {
        List<CertidaoCauc> certidoes = certidaoRepository.listarPorPrefeituraId(prefeitura.getId());
        boolean possuiVencida = certidoes.stream().anyMatch(c -> c.getSituacao() == StatusCertidao.VENCIDA);

        if (possuiVencida && prefeitura.getStatusCauc() != StatusCauc.BLOQUEADO) {
            prefeitura.atualizarStatusCauc(StatusCauc.BLOQUEADO);
            prefeituraRepository.salvar(prefeitura);
        } else if (!possuiVencida && prefeitura.getStatusCauc() != StatusCauc.ADIMPLENTE) {
            prefeitura.atualizarStatusCauc(StatusCauc.ADIMPLENTE);
            prefeituraRepository.salvar(prefeitura);
        }
    }

    private void dispararAlerta(Prefeitura prefeitura, CertidaoCauc certidao) {
        AlertaCertidaoCaucEvent evento = AlertaCertidaoCaucEvent.criar(
                certidao.getTenantId(),
                prefeitura.getId(),
                prefeitura.getNomeMunicipio(),
                prefeitura.getUf().name(),
                certidao.getTipoExigencia().name(),
                certidao.getTipoExigencia().getCodigo(),
                certidao.getTipoExigencia().getNome(),
                certidao.getSituacao(),
                certidao.getDiasParaVencer(),
                certidao.getDataValidade()
        );
        eventPublisher.publicarAlerta(evento);
    }

    private PrazoFatalDto calcularProximoPrazoFatal(List<CertidaoCauc> certidoes) {
        if (certidoes == null || certidoes.isEmpty()) {
            return new PrazoFatalDto("Nenhum prazo cadastrado", 90, LocalDate.now().plusDays(90), "CAUC");
        }

        CertidaoCauc menorPrazo = certidoes.stream()
                .filter(c -> c.getDiasParaVencer() != null)
                .min(Comparator.comparingInt(CertidaoCauc::getDiasParaVencer))
                .orElse(certidoes.get(0));

        return new PrazoFatalDto(
                menorPrazo.getTipoExigencia().getNome(),
                menorPrazo.getDiasParaVencer() != null ? menorPrazo.getDiasParaVencer() : 0,
                menorPrazo.getDataValidade(),
                "CAUC"
        );
    }

    private ItemCertidaoDto toItemDto(CertidaoCauc cert) {
        return new ItemCertidaoDto(
                cert.getId(),
                cert.getTipoExigencia().getCodigo(),
                cert.getTipoExigencia().getGrupo(),
                cert.getTipoExigencia(),
                cert.getTipoExigencia().getNome(),
                cert.getTipoExigencia().getOrgaoEmissor(),
                cert.getNumeroCertidao(),
                cert.getDataEmissao(),
                cert.getDataValidade(),
                cert.getSituacao(),
                cert.getDiasParaVencer(),
                cert.getS3KeyComprovante(),
                cert.getUpdatedAt()
        );
    }
}
