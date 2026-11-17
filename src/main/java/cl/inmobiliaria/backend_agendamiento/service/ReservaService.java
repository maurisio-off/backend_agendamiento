package cl.inmobiliaria.backend_agendamiento.service;

import cl.inmobiliaria.backend_agendamiento.client.CatalogoClient;
import cl.inmobiliaria.backend_agendamiento.client.UsuariosClient;
import cl.inmobiliaria.backend_agendamiento.dto.PropiedadCatalogoResponse;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.dto.UsuarioPerfilResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import cl.inmobiliaria.backend_agendamiento.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
    private static final String ROL_ADMIN = "ADMIN";

    private final ReservaRepository reservaRepository;
    private final UsuariosClient usuariosClient;
    private final CatalogoClient catalogoClient;

    public ReservaResponse crear(String authorization, ReservaRequest request) {
        // 1. quién es el usuario (se obtiene del token) y que sea cliente
        UsuarioPerfilResponse perfil = usuariosClient.obtenerPerfil(authorization);
        if (esAdmin(perfil)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Las cuentas de administrador no pueden agendar visitas.");
        }

        // 2. reglas de fecha y hora (no necesitan llamar a otros MS)
        validarAnticipacion(request.fecha());
        validarHorario(request.hora());
        validarBloqueDe30Minutos(request.hora());

        // 3. la propiedad existe en Catálogo y está disponible
        PropiedadCatalogoResponse propiedad = catalogoClient.obtenerPropiedad(request.propiedadId());
        if (!propiedad.disponible()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La propiedad no está disponible para agendar visitas.");
        }

        // 4. choques de horario
        if (reservaRepository.existsByPropiedadIdAndFechaAndHora(
                request.propiedadId(), request.fecha(), request.hora())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esta propiedad ya tiene una visita agendada en ese mismo horario.");
        }
        if (reservaRepository.existsByUsuarioIdAndFechaAndHora(
                perfil.id(), request.fecha(), request.hora())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya tienes otra visita agendada en esa misma fecha y hora.");
        }

        // 5. se guarda con una copia de los datos del momento de la reserva
        Reserva reserva = Reserva.builder()
                .usuarioId(perfil.id())
                .usuarioNombre(nombreCompleto(perfil))
                .usuarioCorreo(perfil.correo())
                .propiedadId(propiedad.id())
                .tituloPropiedad(propiedad.titulo())
                .fecha(request.fecha())
                .hora(request.hora())
                .build();

        return ReservaResponse.desde(reservaRepository.save(reserva));
    }

    // TODAS LAS RESERVAS -> ADMIN
    public List<ReservaResponse> listarTodas(String authorization) {
        UsuarioPerfilResponse perfil = usuariosClient.obtenerPerfil(authorization);
        if (!esAdmin(perfil)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Solo los administradores pueden ver todas las reservas.");
        }
        return reservaRepository.findAllByOrderByFechaAscHoraAsc().stream()
                .map(ReservaResponse::desde)
                .toList();
    }

    // las reservas del usuario dueño del token
    public List<ReservaResponse> listarMias(String authorization) {
        UsuarioPerfilResponse perfil = usuariosClient.obtenerPerfil(authorization);
        return reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(perfil.id()).stream()
                .map(ReservaResponse::desde)
                .toList();
    }

    // RESERVA POR ID ADMIN O CLIENTE
    public ReservaResponse obtener(String authorization, Long id) {
        UsuarioPerfilResponse perfil = usuariosClient.obtenerPerfil(authorization);
        Reserva reserva = buscarOFallar(id);
        validarAcceso(perfil, reserva);
        return ReservaResponse.desde(reserva);
    }

    // CANCELAR CLIENTE O ADMIN
    public void cancelar(String authorization, Long id) {
        UsuarioPerfilResponse perfil = usuariosClient.obtenerPerfil(authorization);
        Reserva reserva = buscarOFallar(id);
        validarAcceso(perfil, reserva);
        reservaRepository.delete(reserva);
    }

    // PERMISOS Y VALIDACIONES

    private boolean esAdmin(UsuarioPerfilResponse perfil) {
        return ROL_ADMIN.equalsIgnoreCase(perfil.tipoRol());
    }

    private void validarAcceso(UsuarioPerfilResponse perfil, Reserva reserva) {
        if (!esAdmin(perfil) && !reserva.getUsuarioId().equals(perfil.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta reserva.");
        }
    }

    private String nombreCompleto(UsuarioPerfilResponse perfil) {
        String apellido = perfil.apellido();
        return (apellido == null || apellido.isBlank()) ? perfil.nombre() : perfil.nombre() + " " + apellido;
    }

    // Reglas de negocio fecha y hora

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