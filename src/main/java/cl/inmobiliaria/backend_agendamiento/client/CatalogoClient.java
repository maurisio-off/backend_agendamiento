package cl.inmobiliaria.backend_agendamiento.client;

import cl.inmobiliaria.backend_agendamiento.dto.PropiedadCatalogoResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Llamamos al microservicio de Catálogo en el puerto 8082
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Value("${servicios.catalogo.url}") String urlBase) {
        this.restClient = RestClientFactory.crear(urlBase);
    }

    // comprobamos que la propiedad exista y trae el título
    public PropiedadCatalogoResponse obtenerPropiedad(Long propiedadId) {
        try {
            PropiedadCatalogoResponse propiedad = restClient.get()
                    .uri("/api/propiedades/{id}", propiedadId)
                    .retrieve()
                    .body(PropiedadCatalogoResponse.class);

            if (propiedad == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "El servicio de Catálogo devolvió una respuesta vacía.");
            }
            return propiedad;

        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La propiedad " + propiedadId + " no existe en el catálogo.");
        } catch (RestClientResponseException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "El servicio de Catálogo respondió con un error.");
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de Catálogo no está disponible.");
        }
    }
}