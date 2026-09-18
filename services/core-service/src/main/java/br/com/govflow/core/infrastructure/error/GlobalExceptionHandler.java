package br.com.govflow.core.infrastructure.error;

import br.com.govflow.core.domain.exception.ConsultoriaJaCadastradaException;
import br.com.govflow.core.domain.exception.ConsultoriaNaoEncontradaException;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.exception.DomainException;
import br.com.govflow.core.domain.exception.InconsistenciaMatematicaException;
import br.com.govflow.core.domain.exception.LimitePrefeiturasExcedidoException;
import br.com.govflow.core.domain.exception.PrefeituraJaCadastradaException;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingTenantHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingTenantHeader(MissingTenantHeaderException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Header de Tenant Ausente");
        problem.setType(URI.create("https://govflow.com.br/errors/missing-tenant"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(InvalidTenantHeaderException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTenantHeader(InvalidTenantHeaderException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Header de Tenant Inválido");
        problem.setType(URI.create("https://govflow.com.br/errors/invalid-tenant"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler({PrefeituraNaoEncontradaException.class, ConsultoriaNaoEncontradaException.class, DocumentoNaoEncontradoException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(DomainException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://govflow.com.br/errors/not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({PrefeituraJaCadastradaException.class, ConsultoriaJaCadastradaException.class})
    public ResponseEntity<ProblemDetail> handleConflict(DomainException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de Dados");
        problem.setType(URI.create("https://govflow.com.br/errors/conflict"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(LimitePrefeiturasExcedidoException.class)
    public ResponseEntity<ProblemDetail> handleLimitExceeded(LimitePrefeiturasExcedidoException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Limite do Plano Atingido");
        problem.setType(URI.create("https://govflow.com.br/errors/limit-exceeded"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(InconsistenciaMatematicaException.class)
    public ResponseEntity<ProblemDetail> handleInconsistenciaMatematica(InconsistenciaMatematicaException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Inconsistência Matemática Fiscal");
        problem.setType(URI.create("https://govflow.com.br/errors/inconsistencia-matematica"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("valorBruto", ex.getValorBruto());
        problem.setProperty("totalDeducoes", ex.getTotalDeducoes());
        problem.setProperty("valorLiquido", ex.getValorLiquido());
        problem.setProperty("diferenca", ex.getDiferenca());
        problem.setProperty("tolerancia", "0.00");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Regra de Domínio Violada");
        problem.setType(URI.create("https://govflow.com.br/errors/domain-violation"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetros da requisição são inválidos.");
        problem.setTitle("Erro de Validação");
        problem.setType(URI.create("https://govflow.com.br/errors/validation-error"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("invalidParams", errors);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requisição Inválida");
        problem.setType(URI.create("https://govflow.com.br/errors/bad-request"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado durante processamento da requisição {}: ", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado. Por favor, contate o suporte.");
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("https://govflow.com.br/errors/internal-error"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
