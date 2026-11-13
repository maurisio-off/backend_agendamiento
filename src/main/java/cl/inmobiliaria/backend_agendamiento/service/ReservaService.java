package cl.inmobiliaria.backend_agendamiento.service;

import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import cl.inmobiliaria.backend_agendamiento.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(20, 0);

    private final ReservaRepository reservaRepository;

    @Transactional
    public ReservaResponse crear(ReservaRequest request) {
        // Reglas de negocio -> comprobar con Usuarios que el usuario existe y es cliente,
        // y con Catálogo que la propiedad existe (y traer su título (solo el título))

        validarAnticipacion(request.fecha());
        validarHorario(request.hora());
        validarBloqueDe30Minutos(request.hora());

        if (reservaRepository.existsByPropiedadIdAndFechaAndHora(
                request.propiedadId(), request.fecha(), request.hora())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esta propiedad ya tiene una visita agendada en ese mismo horario.");
        }
        if (reservaRepository.existsByUsuarioIdAndFechaAndHora(
                request.usuarioId(), request.fecha(), request.hora())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya tienes otra visita agendada en esa misma fecha y hora.");
        }

        Reserva reserva = Reserva.builder()
                .usuarioId(request.usuarioId())
                .propiedadId(request.propiedadId())
                .fecha(request.fecha())
                .hora(request.hora())
                .build();

        return ReservaResponse.desde(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAllByOrderByFechaAscHoraAsc().stream()
                .map(ReservaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(usuarioId).stream()
                .map(ReservaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return ReservaResponse.desde(buscarOFallar(id));
    }

    @Transactional
    public void cancelar(Long id) {
        reservaRepository.delete(buscarOFallar(id));
    }

    // VALIDACIONES DE LA RESERVA

    // la visita debe ser desde mañana, al menos 24 horas de anticipación (REVISAR PORQUE EN EL FRONT SE PUEDE AGENDAR PARA HOY)
    private void validarAnticipacion(LocalDate fecha) {
        LocalDate manana = LocalDate.now(ZONA).plusDays(1);
        if (fecha.isBefore(manana)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Las reservas deben realizarse con al menos 24 horas de anticipación.");
        }
    }

    // entre las 08:00 y las 20:00
    private void validarHorario(LocalTime hora) {
        if (hora.isBefore(HORA_APERTURA) || hora.isAfter(HORA_CIERRE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Las visitas solo se pueden agendar entre las 08:00 y las 20:00 hrs.");
        }
    }

    // minutos 00 o 30, sin segundos
    private void validarBloqueDe30Minutos(LocalTime hora) {
        if (hora.getMinute() % 30 != 0 || hora.getSecond() != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Las visitas se deben agendar en bloques de 30 minutos (ej. 10:00 o 10:30).");
        }
    }

    private Reserva buscarOFallar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No existe una reserva con id " + id + "."));
    }
}