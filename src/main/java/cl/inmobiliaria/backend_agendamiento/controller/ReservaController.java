package cl.inmobiliaria.backend_agendamiento.controller;

import cl.inmobiliaria.backend_agendamiento.dto.ReservaRequest;
import cl.inmobiliaria.backend_agendamiento.dto.ReservaResponse;
import cl.inmobiliaria.backend_agendamiento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    // agendar una visita
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request) {
        return reservaService.crear(request);
    }

    // todas las reservas (panel de administración)
    @GetMapping
    public List<ReservaResponse> listar() {
        return reservaService.listarTodas();
    }

    // reservas de un usuario (Mi Panel)
    @GetMapping("/usuario/{usuarioId}")
    public List<ReservaResponse> listarPorUsuario(@PathVariable Long usuarioId) {
        return reservaService.listarPorUsuario(usuarioId);
    }

    // una reserva por id
    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return reservaService.obtener(id);
    }

    // cancelar una visita
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id) {
        reservaService.cancelar(id);
    }
}