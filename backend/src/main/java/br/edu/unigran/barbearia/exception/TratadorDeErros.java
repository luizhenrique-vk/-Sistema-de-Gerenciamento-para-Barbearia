package br.edu.unigran.barbearia.exception;

import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;

@RestControllerAdvice
public class TratadorDeErros {
    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    private static ResponseEntity<Map<String, Object>> resp(HttpStatus s, String msg, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", s.value());
        corpo.put("mensagem", msg);
        if (campos != null && !campos.isEmpty()) corpo.put("campos", campos);
        return ResponseEntity.status(s).body(corpo);
    }

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<Map<String, Object>> negocio(NegocioException e) { return resp(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return resp(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos informados.", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> corpoInvalido(Exception e) { return resp(HttpStatus.BAD_REQUEST, "Requisição malformada.", null); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> negado(AccessDeniedException e) { return resp(HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação.", null); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade", e);
        return resp(HttpStatus.CONFLICT, "Operação não permitida: o registro viola uma restrição de integridade (duplicidade ou vínculo existente).", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> geral(Exception e) {
        log.error("Erro inesperado", e);
        return resp(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.", null);
    }
}
