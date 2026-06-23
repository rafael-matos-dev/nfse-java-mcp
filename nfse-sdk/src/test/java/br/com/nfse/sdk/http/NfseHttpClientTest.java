package br.com.nfse.sdk.http;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.NfseContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfseHttpClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExecuteGetRequestWithHeaders() throws Exception {
        startServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("abc-123", exchange.getRequestHeaders().getFirst("X-Request-Id"));
            respond(exchange, 200, "ok");
        });
        NfseHttpClient client = NfseHttpClient.create(context());

        NfseHttpResponse response = client.get(uri("/status"), Map.of("X-Request-Id", "abc-123"));

        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldExecutePostRequestWithBodyAndContentType() throws Exception {
        startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("application/xml", exchange.getRequestHeaders().getFirst("Content-Type"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("<pedido/>", body);
            respond(exchange, 202, "<protocolo>1</protocolo>");
        });
        NfseHttpClient client = NfseHttpClient.create(context());

        NfseHttpResponse response = client.post(uri("/nfse"), "<pedido/>", "application/xml");

        assertEquals(202, response.statusCode());
        assertEquals("<protocolo>1</protocolo>", response.body());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldExposeUnsuccessfulHttpStatusWithoutThrowing() throws Exception {
        startServer(exchange -> respond(exchange, 503, "indisponivel"));
        NfseHttpClient client = NfseHttpClient.create(context());

        NfseHttpResponse response = client.get(uri("/status"));

        assertEquals(503, response.statusCode());
        assertEquals("indisponivel", response.body());
        assertFalse(response.isSuccessful());
    }

    @Test
    void shouldExecuteHeadRequest() throws Exception {
        startServer(exchange -> {
            assertEquals("HEAD", exchange.getRequestMethod());
            exchange.sendResponseHeaders(204, -1);
        });
        NfseHttpClient client = NfseHttpClient.create(context());

        NfseHttpResponse response = client.head(uri("/dps/DPS1"));

        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldWrapTransportFailures() {
        NfseHttpClient client = NfseHttpClient.withHttpClient(
            java.net.http.HttpClient.newHttpClient(),
            Duration.ofMillis(200)
        );

        assertThrows(
            NfseHttpException.class,
            () -> client.get(URI.create("http://127.0.0.1:1/status"))
        );
    }

    private NfseContext context() {
        return NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .build();
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
