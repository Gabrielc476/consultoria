package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarConvenioUseCase;
import br.com.govflow.core.application.port.in.ConsultarConveniosUseCase;
import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConvenioService implements CadastrarConvenioUseCase, ConsultarConveniosUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConvenioService.class);

    private final ConvenioRepositoryPort convenioRepository;
    private final CondicionanteSuspensivaRepositoryPort condicionanteRepository;

    public ConvenioService(ConvenioRepositoryPort convenioRepository,
                           CondicionanteSuspensivaRepositoryPort condicionanteRepository) {
        this.convenioRepository = convenioRepository;
        this.condicionanteRepository = condicionanteRepository;
    }

    @Override
    public Convenio cadastrar(CadastrarConvenioCommand command) {
        Convenio convenio = Convenio.criarNovo(
                command.tenantId(),
                command.prefeituraId(),
                command.numeroSiconv(),
                command.numeroProcesso(),
                command.orgaoConcedente(),
                command.objeto(),
                command.valorGlobal(),
                command.valorRepasse(),
                command.valorContrapartida(),
                command.possuiClausulaSuspensiva(),
                command.prazoClausulaSuspensiva(),
                command.dataInicioVigencia(),
                command.dataFimVigencia()
        );

        Convenio salvo = convenioRepository.salvar(convenio);
        log.info("Novo convênio {} cadastrado com sucesso para prefeitura {}", salvo.getNumeroSiconv(), salvo.getPrefeituraId());

        if (salvo.isPossuiClausulaSuspensiva()) {
            List<CondicionanteSuspensiva> pilares = Arrays.stream(TipoCondicionanteSuspensiva.values())
                    .map(tipo -> CondicionanteSuspensiva.nova(salvo.getTenantId(), salvo.getId(), tipo))
                    .toList();
            condicionanteRepository.salvarTodas(pilares);
            log.info("Auto-seeding de 3 pilares obrigatórios da Caixa GIGOV (Fase 02) concluído para convênio {}", salvo.getId());
        }

        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Convenio> buscarPorId(UUID id) {
        return convenioRepository.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Convenio> buscarPorNumeroSiconv(String numeroSiconv) {
        return convenioRepository.buscarPorNumeroSiconv(numeroSiconv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Convenio> listarPorPrefeitura(UUID prefeituraId) {
        return convenioRepository.listarPorPrefeitura(prefeituraId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Convenio> listarPorTenant(UUID tenantId) {
        return convenioRepository.listarPorTenant(tenantId);
    }
}
