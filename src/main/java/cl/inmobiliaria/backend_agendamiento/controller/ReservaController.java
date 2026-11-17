package cl.inmobiliaria.backend_agendamiento.controller;

import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    // agendar una visita (el usuario sale del token)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ReservaRequest request) {
        return reservaService.crear(authorization, request);
    }

    // todas las reservas (panel de administración, solo ADMIN)
    @GetMapping
    public List<ReservaResponse> listar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return reservaService.listarTodas(authorization);
    }

    // las reservas del usuario que inició sesión (Mi Panel)
    @GetMapping("/mias")
    public List<ReservaResponse> listarMias(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return reservaService.listarMias(authorization);
    }

    // una reserva por id (su dueño o un ADMIN)
    @GetMapping("/{id}")
    public ReservaResponse obtener(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long id) {
        return reservaService.obtener(authorization, id);
    }

    // cancelar una visita (su dueño o un ADMIN)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long id) {
        reservaService.cancelar(authorization, id);
    }
}