package cl.inmobiliaria.backend_agendamiento.dto;

import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// datos que devuelve la API
public record ReservaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioCorreo,
        Long propiedadId,
        String tituloPropiedad,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime hora,
        LocalDateTime creadaEn
) {

    // convierte la entidad en la respuesta 
    public static ReservaResponse desde(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuarioId(),
                reserva.getUsuarioNombre(),
                reserva.getUsuarioCorreo(),
                reserva.getPropiedadId(),
                reserva.getTituloPropiedad(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getCreadaEn()
        );
    }
}