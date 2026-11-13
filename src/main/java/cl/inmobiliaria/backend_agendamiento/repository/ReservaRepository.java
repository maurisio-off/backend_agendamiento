package cl.inmobiliaria.backend_agendamiento.repository;

import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    // si la propiedad tiene una reserva en la fecha y hora, no se puede reservar
    boolean existsByPropiedadIdAndFechaAndHora(Long propiedadId, LocalDate fecha, LocalTime hora);

    // el usuario ya tiene una visita en esa fecha y hora?
    boolean existsByUsuarioIdAndFechaAndHora(Long usuarioId, LocalDate fecha, LocalTime hora);

    // reservas de un usuario, de la más próxima a la más lejana
    List<Reserva> findByUsuarioIdOrderByFechaAscHoraAsc(Long usuarioId);

    // todas las reservas (panel de admin) ordenadas de la más cercana a la más lejana
    List<Reserva> findAllByOrderByFechaAscHoraAsc();
}

