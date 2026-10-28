package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.ConsultarLicitacoesConvenioUseCase;
import br.com.govflow.core.application.port.in.EmitirAioUseCase;
import br.com.govflow.core.application.port.in.HomologarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.RegistrarResultadoVrplUseCase;
import br.com.govflow.core.application.port.in.SubmeterVrplUseCase;
import br.com.govflow.core.application.port.in.ValidarAioParaMedicaoUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.LicitacaoRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.exception.LicitacaoNaoEncontradaException;
import br.com.govflow.core.domain.exception.RegraNegocioLicitacaoException;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.StatusVrpl;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class LicitacaoService implements
        CadastrarLicitacaoUseCase,
        ConsultarLicitacoesConvenioUseCase,
        HomologarLicitacaoUseCase,
        SubmeterVrplUseCase,
        RegistrarResultadoVrplUseCase,
        EmitirAioUseCase,
        ValidarAioParaMedicaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(LicitacaoService.class);

    private final ConvenioRepositoryPort convenioRepository;
    private final LicitacaoRepositoryPort licitacaoRepository;
    private final UsuarioRepositoryPort usuarioRepository;

    public LicitacaoService(ConvenioRepositoryPort convenioRepository,
                            LicitacaoRepositoryPort licitacaoRepository,
                            UsuarioRepositoryPort usuarioRepository) {
        this.convenioRepository = convenioRepository;
        this.licitacaoRepository = licitacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Licitacao cadastrar(CadastrarLicitacaoCommand command) {
        Convenio convenio = buscarConvenioComAcesso(command.convenioId());
        if (licitacaoRepository.existePorConvenioIdENumeroLicitacao(convenio.getId(), command.numeroLicitacao())) {
            throw new RegraNegocioLicitacaoException(
                    "Já existe licitação com o número '" + command.numeroLicitacao() + "' neste convênio."
            );
        }

        Licitacao nova = Licitacao.cadastrar(
                convenio.getTenantId(),
                convenio.getId(),
                convenio.getPrefeituraId(),
                command.numeroProcessoAdministrativo(),
                command.numeroLicitacao(),
                command.modalidade(),
                command.criterioJulgamento(),
                command.regimeExecucao(),
                command.objeto(),
                command.valorEstimado(),
                command.dataPublicacaoEdital(),
                command.dataAberturaPropostas(),
                command.linkPncp(),
                command.linkTransferegov(),
                command.linkSistemaCompras()
        );
        Licitacao salva = licitacaoRepository.salvar(nova);
        log.info("Licitação {} cadastrada no convênio {}", salva.getNumeroLicitacao(), convenio.getNumeroSiconv());
        return salva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Licitacao> listarPorConvenio(UUID convenioId) {
        Convenio convenio = buscarConvenioComAcesso(convenioId);
        return licitacaoRepository.listarPorConvenioId(convenio.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Licitacao buscarPorId(UUID convenioId, UUID licitacaoId) {
        return carregarDoConvenio(convenioId, licitacaoId);
    }

    @Override
    public Licitacao homologar(HomologarLicitacaoCommand command) {
        Licitacao licitacao = carregarDoConvenio(command.convenioId(), command.licitacaoId());
        licitacao.homologar(
                command.valorHomologado(),
                command.dataHomologacao(),
                command.cnpjVencedor(),
                command.razaoSocialVencedor()
        );
        return licitacaoRepository.salvar(licitacao);
    }

    @Override
    public Licitacao submeter(SubmeterVrplCommand command) {
        Licitacao licitacao = carregarDoConvenio(command.convenioId(), command.licitacaoId());
        licitacao.submeterVrpl(command.numeroVrplTransferegov(), command.dataEnvioVrpl());
        return licitacaoRepository.salvar(licitacao);
    }

    @Override
    public Licitacao registrar(RegistrarResultadoVrplCommand command) {
        Licitacao licitacao = carregarDoConvenio(command.convenioId(), command.licitacaoId());
        StatusVrpl resultado = command.resultado();
        if (resultado == StatusVrpl.DILIGENCIA) {
            licitacao.registrarDiligencia(command.observacoesDiligencia());
        } else if (resultado == StatusVrpl.ACEITO_HOMOLOGADO) {
            licitacao.aceitarVrpl(command.parecerTecnico(), command.dataAceiteVrpl());
        } else if (resultado == StatusVrpl.REJEITADO) {
            licitacao.rejeitarVrpl(command.parecerTecnico());
        } else {
            throw new RegraNegocioLicitacaoException(
                    "Resultado de VRPL inválido. Use DILIGENCIA, ACEITO_HOMOLOGADO ou REJEITADO."
            );
        }
        return licitacaoRepository.salvar(licitacao);
    }

    @Override
    public Licitacao emitir(EmitirAioCommand command) {
        Licitacao licitacao = carregarDoConvenio(command.convenioId(), command.licitacaoId());
        licitacao.emitirAio(command.numeroAio(), command.dataEmissaoAio());
        return licitacaoRepository.salvar(licitacao);
    }

    @Override
    @Transactional(readOnly = true)
    public void garantirAioEmitida(UUID licitacaoId) {
        Licitacao licitacao = licitacaoRepository.buscarPorId(licitacaoId)
                .orElseThrow(() -> new LicitacaoNaoEncontradaException(licitacaoId));
        validarAcessoPrefeitura(licitacao.getPrefeituraId());
        licitacao.garantirAioEmitidaParaMedicao();
    }

    private Licitacao carregarDoConvenio(UUID convenioId, UUID licitacaoId) {
        Convenio convenio = buscarConvenioComAcesso(convenioId);
        Licitacao licitacao = licitacaoRepository.buscarPorId(licitacaoId)
                .orElseThrow(() -> new LicitacaoNaoEncontradaException(licitacaoId));
        if (!convenio.getId().equals(licitacao.getConvenioId())) {
            throw new LicitacaoNaoEncontradaException(licitacaoId);
        }
        return licitacao;
    }

    private Convenio buscarConvenioComAcesso(UUID convenioId) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));
        validarAcessoPrefeitura(convenio.getPrefeituraId());
        return convenio;
    }

    private void validarAcessoPrefeitura(UUID prefeituraId) {
        if (prefeituraId == null) {
            return;
        }
        if (!UserContext.isAgente() || UserContext.isAdmin()) {
            return;
        }
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
        throw new AcessoNegadoException("Agente não possui acesso à prefeitura desta licitação.");
    }
}
