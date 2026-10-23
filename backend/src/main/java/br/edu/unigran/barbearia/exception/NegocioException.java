package br.edu.unigran.barbearia.exception;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {
    private final HttpStatus status;
    public NegocioException(HttpStatus status, String mensagem) { super(mensagem); this.status = status; }
    public HttpStatus getStatus() { return status; }
    public static NegocioException invalido(String m) { return new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, m); }
    public static NegocioException conflito(String m) { return new NegocioException(HttpStatus.CONFLICT, m); }
    public static NegocioException naoEncontrado(String m) { return new NegocioException(HttpStatus.NOT_FOUND, m); }
    public static NegocioException proibido(String m) { return new NegocioException(HttpStatus.FORBIDDEN, m); }
}
