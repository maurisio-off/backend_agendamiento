package cl.inmobiliaria.backend_agendamiento.repository;

import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Pruebas de Repositorio - ReservaRepository")
class ReservaRepositoryTest {

    @Autowired
    private ReservaRepository reservaRepository;

    private final LocalDate manana = LocalDate.now().plusDays(1);
    private final LocalDate pasadoManana = LocalDate.now().plusDays(2);

    private Reserva reserva(Long usuarioId, Long propiedadId, LocalDate fecha, LocalTime hora) {
        return Reserva.builder()
                .usuarioId(usuarioId)
                .usuarioNombre("Usuario " + usuarioId)
                .usuarioCorreo("usuario" + usuarioId + "@correo.cl")
                .propiedadId(propiedadId)
                .tituloPropiedad("Propiedad " + propiedadId)
                .fecha(fecha)
                .hora(hora)
                .build();
    }

    @BeforeEach
    void setUp() {
        // El usuario 7 tiene dos visitas (guardadas en desorden a propósito) y el usuario 8 tiene una
        reservaRepository.save(reserva(7L, 15L, pasadoManana, LocalTime.of(9, 0)));
        reservaRepository.save(reserva(7L, 16L, manana, LocalTime.of(11, 0)));
        reservaRepository.save(reserva(8L, 15L, manana, LocalTime.of(10, 0)));
        reservaRepository.flush();
    }

    // Consultas que detectan choques de horario

    @Test
    @DisplayName("Debe detectar que la propiedad ya tiene una visita en esa fecha y hora")
    void existsByPropiedad_DebeSerTrueCuandoHayChoque() {
        assertTrue(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, manana, LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("Debe indicar que la propiedad está libre si cambia la hora, la fecha o la propiedad")
    void existsByPropiedad_DebeSerFalseSiAlgoCambia() {
        assertFalse(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, manana, LocalTime.of(10, 30)));
        assertFalse(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, pasadoManana, LocalTime.of(10, 0)));
        assertFalse(reservaRepository.existsByPropiedadIdAndFechaAndHora(99L, manana, LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("Debe detectar que el usuario ya tiene una visita en esa fecha y hora")
    void existsByUsuario_DebeSerTrueCuandoHayChoque() {
        assertTrue(reservaRepository.existsByUsuarioIdAndFechaAndHora(7L, manana, LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Debe indicar que el usuario está libre si cambia la hora, la fecha o el usuario")
    void existsByUsuario_DebeSerFalseSiAlgoCambia() {
        assertFalse(reservaRepository.existsByUsuarioIdAndFechaAndHora(7L, manana, LocalTime.of(11, 30)));
        assertFalse(reservaRepository.existsByUsuarioIdAndFechaAndHora(7L, pasadoManana, LocalTime.of(11, 0)));
        assertFalse(reservaRepository.existsByUsuarioIdAndFechaAndHora(99L, manana, LocalTime.of(11, 0)));
    }

    // Listados ordenados

    @Test
    @DisplayName("Debe devolver solo las reservas del usuario, de la más próxima a la más lejana")
    void findByUsuario_FiltraYOrdena() {
        List<Reserva> reservas = reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(7L);

        assertEquals(2, reservas.size());
        assertEquals(manana, reservas.get(0).getFecha());
        assertEquals(pasadoManana, reservas.get(1).getFecha());
        assertTrue(reservas.stream().allMatch(r -> r.getUsuarioId().equals(7L)));
    }

    @Test
    @DisplayName("Debe devolver una lista vacía cuando el usuario no tiene reservas")
    void findByUsuario_SinReservasDevuelveVacio() {
        assertTrue(reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(99L).isEmpty());
    }

    @Test
    @DisplayName("Debe devolver todas las reservas ordenadas por fecha y luego por hora")
    void findAll_OrdenaPorFechaYHora() {
        List<Reserva> reservas = reservaRepository.findAllByOrderByFechaAscHoraAsc();

        assertEquals(3, reservas.size());
        // mañana 10:00 (usuario 8), mañana 11:00 (usuario 7), pasado mañana 09:00 (usuario 7)
        assertEquals(LocalTime.of(10, 0), reservas.get(0).getHora());
        assertEquals(LocalTime.of(11, 0), reservas.get(1).getHora());
        assertEquals(pasadoManana, reservas.get(2).getFecha());
    }

    // Guardado y restricciones únicas de la tabla

    @Test
    @DisplayName("Debe completar la fecha de creación automáticamente al guardar")
    void save_CompletaCreadaEn() {
        Reserva guardada = reservaRepository.saveAndFlush(reserva(9L, 20L, manana, LocalTime.of(15, 0)));

        assertNotNull(guardada.getId());
        assertNotNull(guardada.getCreadaEn());
    }

    @Test
    @DisplayName("Debe rechazar dos visitas de la misma propiedad en el mismo horario (uk_reserva_propiedad_horario)")
    void save_RechazaPropiedadDuplicadaEnElMismoHorario() {
        Reserva duplicada = reserva(9L, 15L, manana, LocalTime.of(10, 0));

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> reservaRepository.saveAndFlush(duplicada));

        assertTrue(ex.getMostSpecificCause().getMessage().toLowerCase().contains("uk_reserva_propiedad_horario"));
    }

    @Test
    @DisplayName("Debe rechazar dos visitas del mismo usuario en el mismo horario (uk_reserva_usuario_horario)")
    void save_RechazaUsuarioDuplicadoEnElMismoHorario() {
        Reserva duplicada = reserva(7L, 30L, manana, LocalTime.of(11, 0));

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> reservaRepository.saveAndFlush(duplicada));

        assertTrue(ex.getMostSpecificCause().getMessage().toLowerCase().contains("uk_reserva_usuario_horario"));
    }

    @Test
    @DisplayName("Debe permitir a la misma propiedad otra visita en un horario distinto")
    void save_PermiteLaMismaPropiedadEnOtroHorario() {
        Reserva otraHora = reservaRepository.saveAndFlush(reserva(9L, 15L, manana, LocalTime.of(12, 0)));

        assertNotNull(otraHora.getId());
    }
}
