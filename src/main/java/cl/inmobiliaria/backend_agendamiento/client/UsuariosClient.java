package cl.inmobiliaria.backend_agendamiento.client;

import cl.inmobiliaria.backend_agendamiento.dto.UsuarioPerfilResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Llamamos al microservicio de Usuarios en el puerto 8081
@Component
public class UsuariosClient {

    private final RestClient restClient;

    public UsuariosClient(@Value("${servicios.usuarios.url}") String urlBase) {
        this.restClient = RestClientFactory.crear(urlBase);
    }

    // Reenvía el token que mandó el cliente a /api/auth/perfil y devuelve los datos del usuario dueño del token
    public UsuarioPerfilResponse obtenerPerfil(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Falta el token de autenticación. Inicia sesión e incluye el encabezado Authorization: Bearer <token>.");
        }

        try {
            UsuarioPerfilResponse perfil = restClient.get()
                    .uri("/api/auth/perfil")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(UsuarioPerfilResponse.class);

            if (perfil == null || perfil.id() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "El servicio de Usuarios devolvió una respuesta vacía.");
            }
            return perfil;

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // el ms Usuarios responde 403 cuando el token falta, es inválido o expiró con su msj
            throw new ApiException(HttpStatus.UNAUTHORIZED, "El token no es válido o expiró. Inicia sesión nuevamente.");
        } catch (RestClientResponseException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "El servicio de Usuarios respondió con un error.");
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de Usuarios no está disponible.");
        }
    }
}