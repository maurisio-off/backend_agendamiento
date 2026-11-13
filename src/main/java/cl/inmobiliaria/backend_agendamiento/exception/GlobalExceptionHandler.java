package cl.inmobiliaria.backend_agendamiento.exception;

import cl.inmobiliaria.backend_agendamiento.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

// convierte los errores conocidos en respuestas HTTP con estado y mensaje JSON para la lectura del cliente
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. errores de negocio lanzados desde el servicio (404, 409, 400...)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> manejarApiException(ApiException ex) {
        return construir(ex.getStatus(), ex.getMessage(), List.of());
    }

    // 2. el cuerpo no cumple las validaciones del DTO (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.", detalles);
    }

    // 3. JSON mal formado o fecha/hora con formato incorrecto
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido. Use fecha aaaa-mm-dd y hora HH:mm.", List.of());
    }

    // 4. un id que debía ser número (por ejemplo /api/reservas/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El valor de '" + ex.getName() + "' no es válido.", List.of());
    }

    // 5. la base de datos rechazó una restricción única para dos peticiones al mismo tiempo
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        String mensaje = "No se pudo guardar la reserva por un conflicto de datos.";
        if (causa.contains("uk_reserva_propiedad_horario")) {
            mensaje = "Esta propiedad ya tiene una visita agendada en ese mismo horario.";
        } else if (causa.contains("uk_reserva_usuario_horario")) {
            mensaje = "Ya tienes una visita agendada en esa misma fecha y hora.";
        }
        return construir(HttpStatus.CONFLICT, mensaje, List.of());
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, List<String> detalles) {
        ErrorResponse cuerpo = new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}