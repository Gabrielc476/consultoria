package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AtualizarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConsultoriaNaoEncontradaException;
import br.com.govflow.core.domain.exception.DomainException;
import br.com.govflow.core.domain.exception.LimitePrefeiturasExcedidoException;
import br.com.govflow.core.domain.exception.PrefeituraJaCadastradaException;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.CodigoIbge;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.Cpf;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.Uf;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PrefeituraService implements CadastrarPrefeituraUseCase, ConsultarPrefeituraUseCase, AtualizarPrefeituraUseCase {

    private final PrefeituraRepositoryPort prefeituraRepository;
    private final ConsultoriaRepositoryPort consultoriaRepository;
    private final UsuarioRepositoryPort usuarioRepository;

    public PrefeituraService(PrefeituraRepositoryPort prefeituraRepository,
                             ConsultoriaRepositoryPort consultoriaRepository,
                             UsuarioRepositoryPort usuarioRepository) {
        this.prefeituraRepository = prefeituraRepository;
        this.consultoriaRepository = consultoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Prefeitura cadastrar(CadastrarPrefeituraCommand command) {
        validarNaoEhAgente();

        // 1. Valida existência e limite do Tenant (Consultoria) se cadastrado
        Optional<Consultoria> consultoriaOpt = consultoriaRepository.buscarPorId(command.tenantId());
        if (consultoriaOpt.isPresent()) {
            Consultoria consultoria = consultoriaOpt.get();
            long totalPrefeituras = prefeituraRepository.contarPorTenantId(command.tenantId());
            if (!consultoria.podeCadastrarPrefeitura((int) totalPrefeituras)) {
                throw new LimitePrefeiturasExcedidoException(consultoria.getLimitePrefeituras());
            }
        }

        // 2. Criação dos Value Objects (dispara validações ricas de domínio)
        Cnpj cnpj = new Cnpj(command.cnpj());
        Uf uf = Uf.fromString(command.uf())
                .orElseThrow(() -> new IllegalArgumentException("UF inválida: " + command.uf()));
        CodigoIbge codigoIbge = new CodigoIbge(command.codigoIbge());
        Cpf cpfPrefeito = (command.cpfPrefeito() != null && !command.cpfPrefeito().isBlank())
                ? new Cpf(command.cpfPrefeito())
                : null;

        // 3. Verificação de unicidade no tenant
        if (prefeituraRepository.existePorCnpjETenantId(cnpj, command.tenantId())) {
            throw new PrefeituraJaCadastradaException("Já existe uma prefeitura cadastrada com o CNPJ " + cnpj.getFormatted() + " para esta consultoria.");
        }

        // 4. Criação e persistência do Agregado
        Prefeitura novaPrefeitura = Prefeitura.criarNova(
                command.tenantId(),
                cnpj,
                command.razaoSocial(),
                command.nomeMunicipio(),
                uf,
                codigoIbge,
                command.porteMunicipio(),
                command.nomePrefeito(),
                cpfPrefeito,
                command.inicioMandato(),
                command.fimMandato()
        );

        return prefeituraRepository.salvar(novaPrefeitura);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prefeitura> buscarPorId(UUID id) {
        Optional<Prefeitura> opt = prefeituraRepository.buscarPorId(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        if (UserContext.isAgente()) {
            Set<UUID> idsPermitidos = obterPrefeiturasDoAgente();
            if (!idsPermitidos.contains(id)) {
                throw new AcessoNegadoException("Você não possui permissão para acessar esta prefeitura.");
            }
        }

        return opt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prefeitura> listar(int page, int size, Boolean ativo) {
        if (UserContext.isAgente()) {
            Set<UUID> idsPermitidos = obterPrefeiturasDoAgente();
            if (idsPermitidos.isEmpty()) {
                return Collections.emptyList();
            }
            return prefeituraRepository.listarPorIds(idsPermitidos, page, size, ativo);
        }
        return prefeituraRepository.listar(page, size, ativo);
    }

    @Override
    @Transactional(readOnly = true)
    public long contar(Boolean ativo) {
        if (UserContext.isAgente()) {
            Set<UUID> idsPermitidos = obterPrefeiturasDoAgente();
            if (idsPermitidos.isEmpty()) {
                return 0;
            }
            return prefeituraRepository.contarPorIds(idsPermitidos, ativo);
        }
        return prefeituraRepository.contar(ativo);
    }

    @Override
    public Prefeitura atualizar(UUID id, AtualizarPrefeituraCommand command) {
        validarNaoEhAgente();

        Prefeitura prefeitura = prefeituraRepository.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));

        Cpf cpfPrefeito = (command.cpfPrefeito() != null && !command.cpfPrefeito().isBlank())
                ? new Cpf(command.cpfPrefeito())
                : null;

        prefeitura.atualizarDadosCadastrais(
                command.razaoSocial(),
                command.nomeMunicipio(),
                command.porteMunicipio(),
                command.nomePrefeito(),
                cpfPrefeito,
                command.inicioMandato(),
                command.fimMandato(),
                command.statusCauc()
        );

        return prefeituraRepository.salvar(prefeitura);
    }

    @Override
    public void inativar(UUID id) {
        validarNaoEhAgente();

        Prefeitura prefeitura = prefeituraRepository.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));
        prefeitura.inativar();
        prefeituraRepository.salvar(prefeitura);
    }

    @Override
    public void ativar(UUID id) {
        validarNaoEhAgente();

        Prefeitura prefeitura = prefeituraRepository.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));
        prefeitura.ativar();
        prefeituraRepository.salvar(prefeitura);
    }

    private void validarNaoEhAgente() {
        if (UserContext.isAgente()) {
            throw new AcessoNegadoException("Apenas administradores podem cadastrar, alterar ou inativar prefeituras.");
        }
    }

    private Set<UUID> obterPrefeiturasDoAgente() {
        Set<UUID> ids = UserContext.getPrefeiturasAtribuidasIds();
        if (ids != null && !ids.isEmpty()) {
            return ids;
        }
        UUID userId = UserContext.getUserId();
        if (userId != null) {
            return usuarioRepository.buscarPrefeiturasAtribuidas(userId);
        }
        return Collections.emptySet();
    }
}
