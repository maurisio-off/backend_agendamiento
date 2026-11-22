package cl.inmobiliaria.backend_agendamiento.client;

import cl.inmobiliaria.backend_agendamiento.dto.PropiedadCatalogoResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - CatalogoClient")
class CatalogoClientTest {

    // Incluye un campo que el cliente no usa (precio) para comprobar que se ignora
    private static final String PROPIEDAD_JSON = """
            {"id": 15, "titulo": "Casa en Ñuñoa", "ubicacion": "Ñuñoa", "disponible": true,
             "esProyecto": false, "precio": 120000000}
            """;

    private ApiException assertApi(HttpStatus esperado, Executable accion) {
        ApiException ex = assertThrows(ApiException.class, accion);
        assertEquals(esperado, ex.getStatus());
        return ex;
    }

    @Test
    @DisplayName("Debe devolver los datos de la propiedad pedida a /api/propiedades/{id}")
    void obtenerPropiedad_DevuelveLaPropiedad() throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(200, PROPIEDAD_JSON)) {
            PropiedadCatalogoResponse propiedad = new CatalogoClient(servidor.url()).obtenerPropiedad(15L);

            assertEquals(15L, propiedad.id());
            assertEquals("Casa en Ñuñoa", propiedad.titulo());
            assertTrue(propiedad.disponible());
            assertEquals("/api/propiedades/15", servidor.ultimaRuta());
        }
    }

    @Test
    @DisplayName("Debe responder 404 con el id en el mensaje cuando la propiedad no existe en Catálogo")
    void obtenerPropiedad_InexistenteRecibe404() throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(404, "")) {
            CatalogoClient cliente = new CatalogoClient(servidor.url());

            ApiException ex = assertApi(HttpStatus.NOT_FOUND, () -> cliente.obtenerPropiedad(99L));
            assertTrue(ex.getMessage().contains("99"));
        }
    }

    @Test
    @DisplayName("Debe convertir un error de servidor de Catálogo en un 502")
    void obtenerPropiedad_ErrorDeCatalogoRecibe502() throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(500, "")) {
            CatalogoClient cliente = new CatalogoClient(servidor.url());

            assertApi(HttpStatus.BAD_GATEWAY, () -> cliente.obtenerPropiedad(15L));
        }
    }

    @Test
    @DisplayName("Debe responder 503 cuando el servicio de Catálogo no está disponible")
    void obtenerPropiedad_CatalogoCaidoRecibe503() throws Exception {
        CatalogoClient cliente = new CatalogoClient(ServidorFalso.urlDeUnServidorApagado());

        assertApi(HttpStatus.SERVICE_UNAVAILABLE, () -> cliente.obtenerPropiedad(15L));
    }
}
