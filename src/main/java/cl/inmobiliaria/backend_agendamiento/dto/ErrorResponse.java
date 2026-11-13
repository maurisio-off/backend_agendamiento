package cl.inmobiliaria.backend_agendamiento.dto;

import java.util.List;

// devuelve un JSON que entrega la API cuando algo sale mal
public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {
}