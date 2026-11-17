package cl.inmobiliaria.backend_agendamiento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// datos que Catálogo devuelve en GET /api/propiedades/{id} (solo los campos que usamos en este ms)
@JsonIgnoreProperties(ignoreUnknown = true) // ignora campos que no necesitamos
public record PropiedadCatalogoResponse(
        Long id,
        String titulo,
        String ubicacion,
        boolean disponible,
        boolean esProyecto
) {
}