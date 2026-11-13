package cl.inmobiliaria.backend_agendamiento.exception;

import org.springframework.http.HttpStatus;

// error conocido de la aplicación, lleva el estado del HTTP con el que se debe responder
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}