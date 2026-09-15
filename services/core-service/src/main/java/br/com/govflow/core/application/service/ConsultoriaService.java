package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarConsultoriaUseCase;
import br.com.govflow.core.application.port.in.ConsultarConsultoriaUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.domain.exception.ConsultoriaJaCadastradaException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConsultoriaService implements CadastrarConsultoriaUseCase, ConsultarConsultoriaUseCase {

    private final ConsultoriaRepositoryPort consultoriaRepository;

    public ConsultoriaService(ConsultoriaRepositoryPort consultoriaRepository) {
        this.consultoriaRepository = consultoriaRepository;
    }

    @Override
    public Consultoria cadastrar(CadastrarConsultoriaCommand command) {
        Cnpj cnpj = new Cnpj(command.cnpj());

        if (consultoriaRepository.existePorCnpj(cnpj)) {
            throw new ConsultoriaJaCadastradaException("Já existe uma consultoria cadastrada com o CNPJ " + cnpj.getFormatted());
        }

        Consultoria novaConsultoria = Consultoria.criarNova(
                cnpj,
                command.razaoSocial(),
                command.nomeFantasia(),
                command.emailContato(),
                command.telefoneContato(),
                command.plano()
        );

        return consultoriaRepository.salvar(novaConsultoria);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Consultoria> buscarPorId(UUID id) {
        return consultoriaRepository.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Consultoria> buscarPorCnpj(String cnpj) {
        return consultoriaRepository.buscarPorCnpj(new Cnpj(cnpj));
    }
}
