package cl.inmobiliaria.backend_agendamiento.client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// Servidor HTTP mínimo (viene incluido en la JDK) que simula a Usuarios o a Catálogo en las pruebas de los clientes
class ServidorFalso implements AutoCloseable {

    private final HttpServer servidor;
    private volatile String ultimaRuta;
    private volatile String ultimoAuthorization;

    // Responde siempre con el estado y el cuerpo indicados (si el cuerpo va vacío, responde sin cuerpo)
    ServidorFalso(int estado, String cuerpoJson) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        servidor.createContext("/", exchange -> {
            ultimaRuta = exchange.getRequestURI().getPath();
            ultimoAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] cuerpo = cuerpoJson.getBytes(StandardCharsets.UTF_8);
            if (cuerpo.length == 0) {
                exchange.sendResponseHeaders(estado, -1);
            } else {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(estado, cuerpo.length);
                exchange.getResponseBody().write(cuerpo);
            }
            exchange.close();
        });
        servidor.start();
    }

    // Dirección de un servidor que ya fue apagado: sirve para simular que el otro microservicio está caído
    static String urlDeUnServidorApagado() throws IOException {
        try (ServidorFalso servidor = new ServidorFalso(200, "")) {
            return servidor.url();
        }
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    String ultimaRuta() {
        return ultimaRuta;
    }

    String ultimoAuthorization() {
        return ultimoAuthorization;
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
