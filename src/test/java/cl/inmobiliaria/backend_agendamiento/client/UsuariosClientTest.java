package cl.inmobiliaria.backend_agendamiento.client;

import cl.inmobiliaria.backend_agendamiento.dto.UsuarioPerfilResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - UsuariosClient")
class UsuariosClientTest {

    private static final String TOKEN = "Bearer token-de-prueba";

    // Incluye un campo que el cliente no usa (rut) para comprobar que se ignora
    private static final String PERFIL_JSON = """
            {"id": 7, "correo": "camila@correo.cl", "nombre": "Camila", "apellido": "Soto",
             "tipoRol": "USUARIO", "rut": "12.345.678-5"}
            """;

    private void assertApi(HttpStatus esperado, Executable accion) {
        assertEquals(esperado, assertThrows(ApiException.class, accion).getStatus());
    }

    @Test
    @DisplayName("Debe devolver el perfil y reenviar el token tal cual a /api/auth/perfil")
    void obtenerPerfil_DevuelveElPerfilYReenviaElToken() throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(200, PERFIL_JSON)) {
            UsuarioPerfilResponse perfil = new UsuariosClient(servidor.url()).obtenerPerfil(TOKEN);

            assertEquals(7L, perfil.id());
            assertEquals("Camila", perfil.nombre());
            assertEquals("USUARIO", perfil.tipoRol());
            assertEquals("/api/auth/perfil", servidor.ultimaRuta());
            assertEquals(TOKEN, servidor.ultimoAuthorization());
        }
    }

    @ParameterizedTest(name = "token = [{0}]")
    @NullAndEmptySource
    @DisplayName("Debe responder 401 sin llamar a Usuarios cuando falta el token")
    void obtenerPerfil_SinTokenRecibe401(String token) throws Exception {
        // Si llamara a Usuarios (que está apagado) respondería 503 en vez de 401
        UsuariosClient cliente = new UsuariosClient(ServidorFalso.urlDeUnServidorApagado());

        assertApi(HttpStatus.UNAUTHORIZED, () -> cliente.obtenerPerfil(token));
    }

    @ParameterizedTest(name = "Usuarios responde {0}")
    @ValueSource(ints = {401, 403})
    @DisplayName("Debe convertir un 401 o un 403 de Usuarios en un 401 (token inválido o vencido)")
    void obtenerPerfil_TokenRechazadoRecibe401(int estado) throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(estado, "")) {
            UsuariosClient cliente = new UsuariosClient(servidor.url());

            assertApi(HttpStatus.UNAUTHORIZED, () -> cliente.obtenerPerfil(TOKEN));
        }
    }

    @Test
    @DisplayName("Debe convertir un error de servidor de Usuarios en un 502")
    void obtenerPerfil_ErrorDeUsuariosRecibe502() throws Exception {
        try (ServidorFalso servidor = new ServidorFalso(500, "")) {
            UsuariosClient cliente = new UsuariosClient(servidor.url());

            assertApi(HttpStatus.BAD_GATEWAY, () -> cliente.obtenerPerfil(TOKEN));
        }
    }

    @Test
    @DisplayName("Debe responder 503 cuando el servicio de Usuarios no está disponible")
    void obtenerPerfil_UsuariosCaidoRecibe503() throws Exception {
        UsuariosClient cliente = new UsuariosClient(ServidorFalso.urlDeUnServidorApagado());

        assertApi(HttpStatus.SERVICE_UNAVAILABLE, () -> cliente.obtenerPerfil(TOKEN));
    }
}
