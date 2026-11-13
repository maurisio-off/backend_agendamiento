package cl.inmobiliaria.backend_agendamiento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "reservas",
        uniqueConstraints = {
                // Usamos uk por unique key
                // una propiedad no puede tener dos visitas en la misma fecha y hora
                @UniqueConstraint(name = "uk_reserva_propiedad_horario", columnNames = {"propiedad_id", "fecha", "hora"}),
                // un usuario no puede tener dos visitas en la misma fecha y hora
                @UniqueConstraint(name = "uk_reserva_usuario_horario", columnNames = {"usuario_id", "fecha", "hora"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencias a los otros microservicios, solo se guarda el id, sin relaciones JPA (many to one, etc)
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    // Copia de datos del momento de la reserva, se completan al conectar con  ms Usuarios y Catálogo
    @Column(name = "usuario_nombre", length = 120)
    private String usuarioNombre;

    @Column(name = "usuario_correo", length = 150)
    private String usuarioCorreo;

    @Column(name = "titulo_propiedad", length = 200)
    private String tituloPropiedad;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    void antesDeGuardar() {
        this.creadaEn = LocalDateTime.now();
    }
    
}
