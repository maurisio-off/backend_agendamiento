package cl.inmobiliaria.backend_agendamiento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Datos que Usuarios devuelve en GET /api/auth/perfil (solo los campos que usamos en este ms))
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioPerfilResponse(
        Long id,
        String correo,
        String nombre,
        String apellido,
        String telefono,
        String tipoRol
) {
}