package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AtualizarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PrefeituraService implements CadastrarPrefeituraUseCase, ConsultarPrefeituraUseCase, AtualizarPrefeituraUseCase {

    private final PrefeituraRepositoryPort prefeituraRepository;
    private final ConsultoriaRepositoryPort consultoriaRepository;

    public PrefeituraService(PrefeituraRepositoryPort prefeituraRepository,
                             ConsultoriaRepositoryPort consultoriaRepository) {
        this.prefeituraRepository = prefeituraRepository;
        this.consultoriaRepository = consultoriaRepository;
    }

    @Override
    public Prefeitura cadastrar(CadastrarPrefeituraCommand command) {
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
        return prefeituraRepository.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prefeitura> listar(int page, int size, Boolean ativo) {
        return prefeituraRepository.listar(page, size, ativo);
    }

    @Override
    @Transactional(readOnly = true)
    public long contar(Boolean ativo) {
        return prefeituraRepository.contar(ativo);
    }

    @Override
    public Prefeitura atualizar(UUID id, AtualizarPrefeituraCommand command) {
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
        Prefeitura prefeitura = prefeituraRepository.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));
        prefeitura.inativar();
        prefeituraRepository.salvar(prefeitura);
    }

    @Override
    public void ativar(UUID id) {
        Prefeitura prefeitura = prefeituraRepository.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));
        prefeitura.ativar();
        prefeituraRepository.salvar(prefeitura);
    }
}
