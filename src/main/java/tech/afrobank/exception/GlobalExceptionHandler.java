package tech.afrobank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ProblemDetail handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Saldo Insuficiente");
        problem.setType(URI.create("https://afrobank.tech/errors/saldo-insuficiente"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IdempotencyException.class)
    public ProblemDetail handleIdempotency(IdempotencyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de Idempotência");
        problem.setType(URI.create("https://afrobank.tech/errors/idempotencia-conflito"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://afrobank.tech/errors/recurso-nao-encontrado"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail handleNegocio(NegocioException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Violação de Regra de Negócio");
        problem.setType(URI.create("https://afrobank.tech/errors/regra-de-negocio"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro de validação nos campos informados.");
        problem.setTitle("Requisição Inválida");
        problem.setType(URI.create("https://afrobank.tech/errors/validacao"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> erros = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erros.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeral(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado.");
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("https://afrobank.tech/errors/erro-interno"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
