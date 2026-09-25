package br.com.govflow.transferegov.query.service;

import br.com.govflow.transferegov.domain.compliance.SeveridadeInconformidade;
import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialInconformidadeEntity;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoAcaoEntity;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialInconformidadeRepository;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialPlanoAcaoRepository;
import br.com.govflow.transferegov.query.dto.AuditoriaAdpf854ResponseDTO;
import br.com.govflow.transferegov.query.dto.AuditoriaAdpf854ResponseDTO.AlertaInconformidadeResumoDTO;
import br.com.govflow.transferegov.query.dto.AuditoriaAdpf854ResponseDTO.MunicipioAuditoriaResumoDTO;
import br.com.govflow.transferegov.query.dto.EmendaEspecialDetalheDTO;
import br.com.govflow.transferegov.query.dto.EmendaEspecialResumoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EmendaEspecialQueryService {

    private final EmendaEspecialPlanoAcaoRepository planoAcaoRepository;
    private final EmendaEspecialInconformidadeRepository inconformidadeRepository;

    public EmendaEspecialQueryService(
            EmendaEspecialPlanoAcaoRepository planoAcaoRepository,
            EmendaEspecialInconformidadeRepository inconformidadeRepository
    ) {
        this.planoAcaoRepository = planoAcaoRepository;
        this.inconformidadeRepository = inconformidadeRepository;
    }

    public Page<EmendaEspecialResumoDTO> listarEmendas(
            String uf,
            String cnpj,
            String municipio,
            Integer ano,
            String parlamentar,
            StatusAdpf854 status,
            Pageable pageable
    ) {
        return planoAcaoRepository.findByFiltros(uf, cnpj, municipio, ano, parlamentar, status, pageable)
                .map(EmendaEspecialResumoDTO::fromEntity);
    }

    public Optional<EmendaEspecialDetalheDTO> buscarPorIdOuCodigo(String idOuCodigo) {
        if (idOuCodigo == null || idOuCodigo.isBlank()) {
            return Optional.empty();
        }

        try {
            UUID uuid = UUID.fromString(idOuCodigo);
            return planoAcaoRepository.findById(uuid).map(EmendaEspecialDetalheDTO::fromEntity);
        } catch (IllegalArgumentException ignored) {}

        try {
            Long idPlano = Long.parseLong(idOuCodigo);
            Optional<EmendaEspecialPlanoAcaoEntity> byIdPlano = planoAcaoRepository.findByIdPlanoAcao(idPlano);
            if (byIdPlano.isPresent()) {
                return byIdPlano.map(EmendaEspecialDetalheDTO::fromEntity);
            }
        } catch (NumberFormatException ignored) {}

        return planoAcaoRepository.findByCodigoPlanoAcao(idOuCodigo).map(EmendaEspecialDetalheDTO::fromEntity);
    }

    public AuditoriaAdpf854ResponseDTO obterPainelAuditoria(String uf) {
        long totalEmendas = planoAcaoRepository.count();
        BigDecimal valorTotal = planoAcaoRepository.sumValorTotal();
        BigDecimal valorRisco = planoAcaoRepository.sumValorTotalNaoConforme();

        long totalConforme = planoAcaoRepository.countByStatusAdpf854(StatusAdpf854.CONFORME);
        long totalAlerta = planoAcaoRepository.countByStatusAdpf854(StatusAdpf854.ALERTA);
        long totalNaoConforme = planoAcaoRepository.countByStatusAdpf854(StatusAdpf854.NAO_CONFORME);

        double taxaConformidade = 100.0;
        if (totalEmendas > 0) {
            taxaConformidade = ((double) totalConforme / totalEmendas) * 100.0;
        }

        List<EmendaEspecialPlanoAcaoEntity> todas = (uf != null && !uf.isBlank())
                ? planoAcaoRepository.findByUfBeneficiario(uf)
                : planoAcaoRepository.findAll();

        Map<String, List<EmendaEspecialPlanoAcaoEntity>> porMunicipio = todas.stream()
                .collect(Collectors.groupingBy(EmendaEspecialPlanoAcaoEntity::getNomeBeneficiario));

        List<MunicipioAuditoriaResumoDTO> resumoMunicipios = new ArrayList<>();
        porMunicipio.forEach((mun, list) -> {
            long munTotal = list.size();
            BigDecimal munValor = list.stream()
                    .map(EmendaEspecialPlanoAcaoEntity::getValorTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long munConforme = list.stream().filter(e -> e.getStatusAdpf854() == StatusAdpf854.CONFORME).count();
            long munAlerta = list.stream().filter(e -> e.getStatusAdpf854() == StatusAdpf854.ALERTA).count();
            long munNaoConforme = list.stream().filter(e -> e.getStatusAdpf854() == StatusAdpf854.NAO_CONFORME).count();

            double munTaxa = munTotal > 0 ? ((double) munConforme / munTotal) * 100.0 : 100.0;

            String munUf = list.get(0).getUfBeneficiario();
            String munCnpj = list.get(0).getCnpjBeneficiario();

            resumoMunicipios.add(new MunicipioAuditoriaResumoDTO(
                    mun, munUf, munCnpj, munTotal, munValor, munConforme, munAlerta, munNaoConforme, munTaxa
            ));
        });

        resumoMunicipios.sort(Comparator.comparing(MunicipioAuditoriaResumoDTO::totalNaoConforme).reversed());

        List<EmendaEspecialInconformidadeEntity> criticas = inconformidadeRepository.findTopBySeveridade(SeveridadeInconformidade.CRITICO);
        List<AlertaInconformidadeResumoDTO> alertasCriticos = criticas.stream()
                .limit(20)
                .map(i -> new AlertaInconformidadeResumoDTO(
                        i.getId(),
                        i.getPlanoAcao().getIdPlanoAcao(),
                        i.getPlanoAcao().getCodigoPlanoAcao(),
                        i.getPlanoAcao().getNomeBeneficiario(),
                        i.getPlanoAcao().getNomeParlamentar(),
                        i.getTipoInconformidade() != null ? i.getTipoInconformidade().name() : null,
                        i.getSeveridade() != null ? i.getSeveridade().name() : null,
                        i.getDescricao(),
                        i.getDataDeteccao()
                ))
                .toList();

        return new AuditoriaAdpf854ResponseDTO(
                totalEmendas,
                valorTotal != null ? valorTotal : BigDecimal.ZERO,
                valorRisco != null ? valorRisco : BigDecimal.ZERO,
                totalConforme,
                totalAlerta,
                totalNaoConforme,
                taxaConformidade,
                resumoMunicipios,
                alertasCriticos
        );
    }
}
