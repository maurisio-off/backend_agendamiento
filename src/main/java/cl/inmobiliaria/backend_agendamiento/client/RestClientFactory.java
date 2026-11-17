package cl.inmobiliaria.backend_agendamiento.client;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// Crea el RestClient que usan los clientes de otros microservicios
// tiempos máximo de espera para que Agendamiento no se cuelgue si otro ms no responde
final class RestClientFactory {

    private RestClientFactory() {
    }

    static RestClient crear(String urlBase) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .build();
    }
}