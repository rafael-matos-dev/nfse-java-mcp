package br.com.nfse.sdk.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanfseClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldDownloadDanfsePdfByAccessKey() throws Exception {
        byte[] pdf = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.UTF_8);
        startServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/danfse/CHAVE%20COM%20ESPACO", exchange.getRequestURI().getRawPath());
            assertEquals("application/pdf", exchange.getRequestHeaders().getFirst("Accept"));
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, pdf.length);
            exchange.getResponseBody().write(pdf);
        });

        DanfseClient client = new DanfseClient(
            NfseHttpClient.withHttpClient(java.net.http.HttpClient.newHttpClient(), Duration.ofSeconds(5)),
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/danfse")
        );

        NfseBinaryResponse response = client.baixarPdf("CHAVE COM ESPACO");

        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccessful());
        assertArrayEquals(pdf, response.body());
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/danfse/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
