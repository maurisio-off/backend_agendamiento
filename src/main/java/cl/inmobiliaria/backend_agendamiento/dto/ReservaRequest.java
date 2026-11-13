package cl.inmobiliaria.backend_agendamiento.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// datos que envía el cliente (ya sea en react o postman) para poder agendar una visita
public record ReservaRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long usuarioId,

        @NotNull(message = "La propiedad es obligatoria")
        Long propiedadId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora
) {
}