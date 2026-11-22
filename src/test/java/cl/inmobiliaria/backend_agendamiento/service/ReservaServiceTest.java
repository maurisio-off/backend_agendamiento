package cl.inmobiliaria.backend_agendamiento.service;

import cl.inmobiliaria.backend_agendamiento.client.CatalogoClient;
import cl.inmobiliaria.backend_agendamiento.client.UsuariosClient;
import cl.inmobiliaria.backend_agendamiento.dto.PropiedadCatalogoResponse;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.dto.UsuarioPerfilResponse;
import cl.inmobiliaria.backend_agendamiento.exception.ApiException;
import cl.inmobiliaria.backend_agendamiento.model.Reserva;
import cl.inmobiliaria.backend_agendamiento.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ReservaService")
class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final String TOKEN = "Bearer token-de-prueba";

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuariosClient usuariosClient;

    @Mock
    private CatalogoClient catalogoClient;

    @InjectMocks
    private ReservaService reservaService;

    private UsuarioPerfilResponse cliente;
    private UsuarioPerfilResponse admin;
    private PropiedadCatalogoResponse propiedad;
    private LocalDate manana;

    @BeforeEach
    void setUp() {
        cliente = new UsuarioPerfilResponse(7L, "camila@correo.cl", "Camila", "Soto", "+56911111111", "USUARIO");
        admin = new UsuarioPerfilResponse(1L, "admin@inmobiliaria.cl", "Admin", "MVJ", null, "ADMIN");
        propiedad = new PropiedadCatalogoResponse(15L, "Casa en Ñuñoa", "Ñuñoa", true, false);
        manana = LocalDate.now(ZONA).plusDays(1);
    }

    // Métodos de apoyo para armar los datos de prueba

    private ReservaRequest solicitud(LocalDate fecha, String hora) {
        return new ReservaRequest(15L, fecha, LocalTime.parse(hora));
    }

    private Reserva reservaDe(Long id, Long usuarioId) {
        return Reserva.builder()
                .id(id)
                .usuarioId(usuarioId)
                .usuarioNombre("Alguien")
                .usuarioCorreo("alguien@correo.cl")
                .propiedadId(15L)
                .tituloPropiedad("Casa en Ñuñoa")
                .fecha(manana)
                .hora(LocalTime.of(10, 0))
                .build();
    }

    private ApiException assertApi(HttpStatus esperado, Executable accion) {
        ApiException ex = assertThrows(ApiException.class, accion);
        assertEquals(esperado, ex.getStatus());
        return ex;
    }

    // Crear una reserva: caso exitoso

    @Test
    @DisplayName("Debe guardar la reserva con copia del nombre del usuario y del título de la propiedad")
    void crear_GuardaReservaConCopiaDeDatos() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, manana, LocalTime.of(10, 30))).thenReturn(false);
        when(reservaRepository.existsByUsuarioIdAndFechaAndHora(7L, manana, LocalTime.of(10, 30))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        // WHEN
        ReservaResponse respuesta = reservaService.crear(TOKEN, solicitud(manana, "10:30"));

        // THEN
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        Reserva guardada = captor.getValue();
        assertEquals(7L, guardada.getUsuarioId());
        assertEquals("Camila Soto", guardada.getUsuarioNombre());
        assertEquals("camila@correo.cl", guardada.getUsuarioCorreo());
        assertEquals(15L, guardada.getPropiedadId());
        assertEquals("Casa en Ñuñoa", guardada.getTituloPropiedad());
        assertEquals(manana, guardada.getFecha());
        assertEquals(LocalTime.of(10, 30), guardada.getHora());

        assertEquals(99L, respuesta.id());
        assertEquals("Casa en Ñuñoa", respuesta.tituloPropiedad());
        assertEquals("Camila Soto", respuesta.usuarioNombre());
    }

    @ParameterizedTest(name = "apellido = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Debe usar solo el nombre cuando el usuario no tiene apellido (nulo, vacío o en blanco)")
    void crear_UsaSoloNombreCuandoNoHayApellido(String apellido) {
        // GIVEN
        UsuarioPerfilResponse sinApellido =
                new UsuarioPerfilResponse(8L, "sin@correo.cl", "Pedro", apellido, null, "USUARIO");
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(sinApellido);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ReservaResponse respuesta = reservaService.crear(TOKEN, solicitud(manana, "09:00"));

        // THEN
        assertEquals("Pedro", respuesta.usuarioNombre());
    }

    // Crear una reserva: validación del rol del usuario

    @Test
    @DisplayName("Debe rechazar con 403 que un administrador agende y no seguir con el proceso")
    void crear_AdminRecibe403() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(admin);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.FORBIDDEN,
                () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        assertTrue(ex.getMessage().contains("administrador"));
        verifyNoInteractions(catalogoClient, reservaRepository);
    }

    @Test
    @DisplayName("Debe reconocer el rol ADMIN sin importar mayúsculas o minúsculas")
    void crear_AdminEnMinusculasTambienEsRechazado() {
        // GIVEN
        UsuarioPerfilResponse adminMinuscula =
                new UsuarioPerfilResponse(2L, "a@a.cl", "Ana", "Pérez", null, "admin");
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(adminMinuscula);

        // WHEN & THEN
        assertApi(HttpStatus.FORBIDDEN, () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
    }

    // Crear una reserva: reglas de la fecha (24 horas de anticipación)

    @Test
    @DisplayName("Debe rechazar con 400 una reserva para hoy (menos de 24 horas)")
    void crear_FechaDeHoyRecibe400() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        LocalDate hoy = LocalDate.now(ZONA);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.BAD_REQUEST,
                () -> reservaService.crear(TOKEN, solicitud(hoy, "10:00")));
        assertTrue(ex.getMessage().contains("24 horas"));
        verifyNoInteractions(catalogoClient, reservaRepository);
    }

    @Test
    @DisplayName("Debe rechazar con 400 una fecha pasada")
    void crear_FechaPasadaRecibe400() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);

        // WHEN & THEN
        assertApi(HttpStatus.BAD_REQUEST,
                () -> reservaService.crear(TOKEN, solicitud(LocalDate.now(ZONA).minusDays(3), "10:00")));
    }

    @Test
    @DisplayName("Debe aceptar una reserva para mañana, que es el límite permitido")
    void crear_FechaDeMananaEsValida() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ReservaResponse respuesta = reservaService.crear(TOKEN, solicitud(manana, "10:00"));

        // THEN
        assertEquals(manana, respuesta.fecha());
    }

    // Crear una reserva: reglas de la hora (horario y bloques de 30 minutos)

    @ParameterizedTest(name = "{0} es una hora válida")
    @ValueSource(strings = {"08:00", "08:30", "12:00", "19:30", "20:00"})
    @DisplayName("Debe aceptar horas dentro del horario (08:00 a 20:00) en bloques de 30 minutos")
    void crear_HorasValidas(String hora) {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ReservaResponse respuesta = reservaService.crear(TOKEN, solicitud(manana, hora));

        // THEN
        assertEquals(LocalTime.parse(hora), respuesta.hora());
    }

    @ParameterizedTest(name = "{0} está fuera del horario")
    @ValueSource(strings = {"07:30", "07:59", "20:01", "20:30", "23:00"})
    @DisplayName("Debe rechazar con 400 horas fuera del horario de 08:00 a 20:00")
    void crear_HorasFueraDeHorarioRecibe400(String hora) {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.BAD_REQUEST,
                () -> reservaService.crear(TOKEN, solicitud(manana, hora)));
        assertTrue(ex.getMessage().contains("08:00") && ex.getMessage().contains("20:00"));
        verifyNoInteractions(catalogoClient, reservaRepository);
    }

    @ParameterizedTest(name = "{0} no es un bloque de 30 minutos")
    @ValueSource(strings = {"10:15", "10:45", "09:01", "10:00:30"})
    @DisplayName("Debe rechazar con 400 horas que no son bloques de 30 minutos")
    void crear_HorasQueNoSonBloqueRecibe400(String hora) {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.BAD_REQUEST,
                () -> reservaService.crear(TOKEN, solicitud(manana, hora)));
        assertTrue(ex.getMessage().contains("30 minutos"));
        verifyNoInteractions(catalogoClient, reservaRepository);
    }

    // Crear una reserva: disponibilidad de la propiedad y choques de horario

    @Test
    @DisplayName("Debe rechazar con 409 una propiedad que no está disponible")
    void crear_PropiedadNoDisponibleRecibe409() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L))
                .thenReturn(new PropiedadCatalogoResponse(15L, "Casa en Ñuñoa", "Ñuñoa", false, false));

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.CONFLICT,
                () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        assertTrue(ex.getMessage().contains("no está disponible"));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    @DisplayName("Debe propagar el 404 cuando la propiedad no existe en Catálogo y no guardar nada")
    void crear_PropiedadInexistenteRecibe404() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "La propiedad 15 no existe en el catálogo."));

        // WHEN & THEN
        assertApi(HttpStatus.NOT_FOUND, () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    @DisplayName("Debe rechazar con 409 cuando la propiedad ya tiene una visita en ese horario")
    void crear_ChoquePorPropiedadRecibe409() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, manana, LocalTime.of(10, 0))).thenReturn(true);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.CONFLICT,
                () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        assertTrue(ex.getMessage().contains("Esta propiedad"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar con 409 cuando el usuario ya tiene otra visita en ese horario")
    void crear_ChoquePorUsuarioRecibe409() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(catalogoClient.obtenerPropiedad(15L)).thenReturn(propiedad);
        when(reservaRepository.existsByPropiedadIdAndFechaAndHora(15L, manana, LocalTime.of(10, 0))).thenReturn(false);
        when(reservaRepository.existsByUsuarioIdAndFechaAndHora(7L, manana, LocalTime.of(10, 0))).thenReturn(true);

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.CONFLICT,
                () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        assertTrue(ex.getMessage().contains("Ya tienes otra visita"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar el 401 cuando el token no es válido y no consultar nada más")
    void crear_TokenInvalidoRecibe401() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "El token no es válido o expiró."));

        // WHEN & THEN
        assertApi(HttpStatus.UNAUTHORIZED, () -> reservaService.crear(TOKEN, solicitud(manana, "10:00")));
        verifyNoInteractions(catalogoClient, reservaRepository);
    }

    // Listar todas las reservas (solo administrador)

    @Test
    @DisplayName("Debe devolver todas las reservas cuando quien consulta es administrador")
    void listarTodas_AdminVeTodas() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(admin);
        when(reservaRepository.findAllByOrderByFechaAscHoraAsc())
                .thenReturn(List.of(reservaDe(1L, 7L), reservaDe(2L, 8L)));

        // WHEN
        List<ReservaResponse> resultado = reservaService.listarTodas(TOKEN);

        // THEN
        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).id());
        assertEquals(2L, resultado.get(1).id());
    }

    @Test
    @DisplayName("Debe rechazar con 403 que un cliente liste todas las reservas")
    void listarTodas_ClienteRecibe403() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);

        // WHEN & THEN
        assertApi(HttpStatus.FORBIDDEN, () -> reservaService.listarTodas(TOKEN));
        verifyNoInteractions(reservaRepository);
    }

    // Listar las reservas del usuario dueño del token

    @Test
    @DisplayName("Debe devolver solo las reservas del usuario dueño del token")
    void listarMias_DevuelveLasDelUsuario() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(7L))
                .thenReturn(List.of(reservaDe(5L, 7L)));

        // WHEN
        List<ReservaResponse> resultado = reservaService.listarMias(TOKEN);

        // THEN
        assertEquals(1, resultado.size());
        assertEquals(7L, resultado.get(0).usuarioId());
        verify(reservaRepository, never()).findAllByOrderByFechaAscHoraAsc();
    }

    @Test
    @DisplayName("Debe devolver una lista vacía cuando el usuario no tiene reservas")
    void listarMias_SinReservasDevuelveVacio() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findByUsuarioIdOrderByFechaAscHoraAsc(7L)).thenReturn(List.of());

        // WHEN & THEN
        assertTrue(reservaService.listarMias(TOKEN).isEmpty());
    }

    // Obtener una reserva por id

    @Test
    @DisplayName("Debe permitir que el dueño vea su reserva")
    void obtener_DuenoVeSuReserva() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reservaDe(5L, 7L)));

        // WHEN
        ReservaResponse respuesta = reservaService.obtener(TOKEN, 5L);

        // THEN
        assertEquals(5L, respuesta.id());
    }

    @Test
    @DisplayName("Debe permitir que un administrador vea la reserva de otro usuario")
    void obtener_AdminVeReservaAjena() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(admin);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reservaDe(5L, 7L)));

        // WHEN
        ReservaResponse respuesta = reservaService.obtener(TOKEN, 5L);

        // THEN
        assertEquals(7L, respuesta.usuarioId());
    }

    @Test
    @DisplayName("Debe rechazar con 403 que un cliente vea la reserva de otro")
    void obtener_ClienteNoVeReservaAjena() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reservaDe(5L, 999L)));

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.FORBIDDEN, () -> reservaService.obtener(TOKEN, 5L));
        assertTrue(ex.getMessage().contains("No tienes permiso"));
    }

    @Test
    @DisplayName("Debe responder 404 cuando la reserva no existe")
    void obtener_InexistenteRecibe404() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(123L)).thenReturn(Optional.empty());

        // WHEN & THEN
        ApiException ex = assertApi(HttpStatus.NOT_FOUND, () -> reservaService.obtener(TOKEN, 123L));
        assertTrue(ex.getMessage().contains("123"));
    }

    // Cancelar una reserva

    @Test
    @DisplayName("Debe eliminar la reserva cuando quien cancela es su dueño")
    void cancelar_DuenoCancela() {
        // GIVEN
        Reserva reserva = reservaDe(5L, 7L);
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        // WHEN
        reservaService.cancelar(TOKEN, 5L);

        // THEN
        verify(reservaRepository).delete(reserva);
    }

    @Test
    @DisplayName("Debe permitir que un administrador cancele la reserva de cualquier usuario")
    void cancelar_AdminCancelaReservaAjena() {
        // GIVEN
        Reserva reserva = reservaDe(5L, 7L);
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(admin);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        // WHEN
        reservaService.cancelar(TOKEN, 5L);

        // THEN
        verify(reservaRepository).delete(reserva);
    }

    @Test
    @DisplayName("Debe rechazar con 403 que un cliente cancele la reserva de otro y no eliminar nada")
    void cancelar_ClienteNoCancelaReservaAjena() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reservaDe(5L, 999L)));

        // WHEN & THEN
        assertApi(HttpStatus.FORBIDDEN, () -> reservaService.cancelar(TOKEN, 5L));
        verify(reservaRepository, never()).delete(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe responder 404 al cancelar una reserva que no existe y no eliminar nada")
    void cancelar_InexistenteRecibe404() {
        // GIVEN
        when(usuariosClient.obtenerPerfil(TOKEN)).thenReturn(cliente);
        when(reservaRepository.findById(anyLong())).thenReturn(Optional.empty());

        // WHEN & THEN
        assertApi(HttpStatus.NOT_FOUND, () -> reservaService.cancelar(TOKEN, 123L));
        verify(reservaRepository, never()).delete(any(Reserva.class));
    }
}
