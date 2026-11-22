package cl.inmobiliaria.backend_agendamiento.controller;

import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import cl.inmobiliaria.backend_agendamiento.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
@DisplayName("Pruebas Unitarias - ReservaController")
class ReservaControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String ORIGEN_FRONT = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    private LocalDate manana;
    private ReservaResponse respuestaMock;

    @BeforeEach
    void setUp() {
        manana = LocalDate.now().plusDays(1);
        respuestaMock = new ReservaResponse(5L, 7L, "Camila Soto", "camila@correo.cl",
                15L, "Casa en Ñuñoa", manana, LocalTime.of(10, 30), LocalDateTime.of(2026, 11, 20, 9, 0));
    }

    private String cuerpo(String fecha, String hora) {
        return """
                {"propiedadId": 15, "fecha": "%s", "hora": "%s"}
                """.formatted(fecha, hora);
    }

    // Agendar una visita: POST /api/reservas

    @Test
    @DisplayName("Debe responder 201 con el JSON de la reserva (hora como HH:mm) y reenviar el token al servicio")
    void crear_Retorna201() throws Exception {
        // GIVEN
        when(reservaService.crear(eq(TOKEN), any(ReservaRequest.class))).thenReturn(respuestaMock);

        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(manana.toString(), "10:30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.usuarioNombre").value("Camila Soto"))
                .andExpect(jsonPath("$.tituloPropiedad").value("Casa en Ñuñoa"))
                .andExpect(jsonPath("$.fecha").value(manana.toString()))
                .andExpect(jsonPath("$.hora").value("10:30"));

        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservaService).crear(eq(TOKEN), captor.capture());
        assertEquals(15L, captor.getValue().propiedadId());
        assertEquals(manana, captor.getValue().fecha());
        assertEquals(LocalTime.of(10, 30), captor.getValue().hora());
    }

    @Test
    @DisplayName("Debe responder 401 con el mensaje de error cuando no llega el encabezado Authorization")
    void crear_SinTokenRecibe401() throws Exception {
        // GIVEN: sin encabezado el controlador pasa null y el servicio (vía UsuariosClient) responde 401
        when(reservaService.crear(isNull(), any(ReservaRequest.class)))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de autenticación."));

        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(manana.toString(), "10:30")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Falta el token de autenticación."));
    }

    @Test
    @DisplayName("Debe responder 400 con el detalle de cada campo obligatorio faltante y no llamar al servicio")
    void crear_CamposFaltantesRecibe400() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Hay datos inválidos en la solicitud."))
                .andExpect(jsonPath("$.detalles.length()").value(3));

        verifyNoInteractions(reservaService);
    }

    @Test
    @DisplayName("Debe responder 400 cuando el cuerpo no es un JSON válido")
    void crear_JsonRotoRecibe400() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El cuerpo de la solicitud no es válido. Use fecha aaaa-mm-dd y hora HH:mm."));

        verifyNoInteractions(reservaService);
    }

    @Test
    @DisplayName("Debe responder 400 cuando la hora o la fecha tienen un formato incorrecto")
    void crear_FormatoDeHoraInvalidoRecibe400() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(manana.toString(), "25:99")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservaService);
    }

    @Test
    @DisplayName("Debe traducir un ApiException 409 del servicio a una respuesta 409 con su mensaje")
    void crear_ConflictoDelServicioRecibe409() throws Exception {
        // GIVEN
        when(reservaService.crear(eq(TOKEN), any(ReservaRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT,
                        "Esta propiedad ya tiene una visita agendada en ese mismo horario."));

        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(manana.toString(), "10:30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value("Esta propiedad ya tiene una visita agendada en ese mismo horario."));
    }

    @ParameterizedTest(name = "restricción: {0}")
    @CsvSource(delimiter = '|', textBlock = """
            Unique index violation: UK_RESERVA_PROPIEDAD_HORARIO | Esta propiedad ya tiene una visita agendada en ese mismo horario.
            Unique index violation: UK_RESERVA_USUARIO_HORARIO | Ya tienes una visita agendada en esa misma fecha y hora.
            Otra restricción cualquiera | No se pudo guardar la reserva por un conflicto de datos.
            """)
    @DisplayName("Debe responder 409 con el mensaje que corresponde cuando la base de datos rechaza una restricción única")
    void crear_ViolacionDeRestriccionRecibe409(String causa, String mensajeEsperado) throws Exception {
        // GIVEN: dos peticiones simultáneas pasaron la validación y la base de datos rechazó la segunda
        when(reservaService.crear(eq(TOKEN), any(ReservaRequest.class)))
                .thenThrow(new DataIntegrityViolationException("error de integridad", new RuntimeException(causa)));

        // WHEN & THEN
        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(manana.toString(), "10:30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(mensajeEsperado));
    }

    // Consultar reservas: GET

    @Test
    @DisplayName("Debe responder 200 con la lista de todas las reservas (panel de administración)")
    void listar_Retorna200() throws Exception {
        // GIVEN
        when(reservaService.listarTodas(TOKEN)).thenReturn(List.of(respuestaMock));

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @DisplayName("Debe responder 403 cuando el servicio rechaza a un cliente que lista todas las reservas")
    void listar_ClienteRecibe403() throws Exception {
        // GIVEN
        when(reservaService.listarTodas(TOKEN))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "Solo los administradores pueden ver todas las reservas."));

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo los administradores pueden ver todas las reservas."));
    }

    @Test
    @DisplayName("Debe atender /mias con listarMias y no confundirlo con un id")
    void listarMias_Retorna200() throws Exception {
        // GIVEN
        when(reservaService.listarMias(TOKEN)).thenReturn(List.of(respuestaMock));

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas/mias").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(7));

        verify(reservaService).listarMias(TOKEN);
    }

    @Test
    @DisplayName("Debe responder 200 con la reserva pedida por id")
    void obtener_Retorna200() throws Exception {
        // GIVEN
        when(reservaService.obtener(TOKEN, 5L)).thenReturn(respuestaMock);

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas/5").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.hora").value("10:30"));
    }

    @Test
    @DisplayName("Debe responder 404 cuando la reserva no existe")
    void obtener_InexistenteRecibe404() throws Exception {
        // GIVEN
        when(reservaService.obtener(TOKEN, 99L))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "No existe una reserva con id 99."));

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas/99").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe una reserva con id 99."));
    }

    @Test
    @DisplayName("Debe responder 400 cuando el id no es un número")
    void obtener_IdNoNumericoRecibe400() throws Exception {
        // WHEN & THEN
        mockMvc.perform(get("/api/reservas/abc").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor de 'id' no es válido."));

        verifyNoInteractions(reservaService);
    }

    // Cancelar una reserva: DELETE

    @Test
    @DisplayName("Debe responder 204 sin cuerpo al cancelar y pasar el id y el token al servicio")
    void cancelar_Retorna204() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/reservas/5").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isNoContent());

        verify(reservaService).cancelar(TOKEN, 5L);
    }

    @Test
    @DisplayName("Debe responder 403 cuando el servicio no deja cancelar la reserva de otro usuario")
    void cancelar_ReservaAjenaRecibe403() throws Exception {
        // GIVEN
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta reserva."))
                .when(reservaService).cancelar(TOKEN, 5L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/reservas/5").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para acceder a esta reserva."));
    }

    // CORS: permisos para que el front llame desde el navegador

    @Test
    @DisplayName("Debe aceptar la petición previa (preflight) del front permitido con los encabezados Authorization y Content-Type")
    void cors_OrigenPermitidoPasaElPreflight() throws Exception {
        // WHEN & THEN
        mockMvc.perform(options("/api/reservas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONT));
    }

    @Test
    @DisplayName("Debe rechazar con 403 el preflight de un origen que no está permitido")
    void cors_OrigenNoPermitidoEsRechazado() throws Exception {
        // WHEN & THEN
        mockMvc.perform(options("/api/reservas")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe incluir el encabezado CORS en una respuesta normal al origen permitido")
    void cors_RespuestaNormalIncluyeElOrigenPermitido() throws Exception {
        // GIVEN
        when(reservaService.listarMias(TOKEN)).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/api/reservas/mias")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .header(HttpHeaders.ORIGIN, ORIGEN_FRONT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONT));
    }
}
