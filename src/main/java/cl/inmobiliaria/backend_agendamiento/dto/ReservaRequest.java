package cl.inmobiliaria.backend_agendamiento.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// Datos que envía el cliente (ya sea React o Postman) para agendar una visita
// El usuario ya no viaja en el cuerpo: se obtiene del token
public record ReservaRequest(
        @NotNull(message = "La propiedad es obligatoria")
        Long propiedadId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora
) {
}