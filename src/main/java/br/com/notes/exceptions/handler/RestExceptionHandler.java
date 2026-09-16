package br.com.notes.exceptions.handler;

import br.com.notes.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Corpo de erro padronizado em RFC 7807 ({@link ProblemDetail}) — mesmo padrão do
 * workbox-api/budget-service, incluindo o catch-all {@link #handleUnexpected}: sem ele,
 * exceção não mapeada aqui cairia no whitelabel error padrão do Spring, potencialmente
 * vazando stack trace.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final var detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList());
        return detail;
    }

    /**
     * JSON malformado ou com shape errado é sempre erro do client, nunca do server — sem
     * este handler, {@code @ExceptionHandler(Exception.class)} abaixo capturava primeiro
     * (roda antes da resolução default do Spring MVC pra essa exceção) e devolvia 500 pra
     * um payload simplesmente mal formado (bug real achado e corrigido no workbox-api e
     * budget-service, aplicado aqui desde o início).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(final HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    /**
     * Rota inexistente também cai no catch-all genérico se não tratada aqui, virando 500
     * em vez de 404 (mesmo bug do {@code HttpMessageNotReadableException} acima, achado
     * originalmente via investigação de roteamento no workbox-api).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(final NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception exception, final HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ProblemDetail problem(final HttpStatus status, final String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
