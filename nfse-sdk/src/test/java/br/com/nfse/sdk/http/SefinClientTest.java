package br.com.nfse.sdk.http;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.xml.XmlPayloadCodec;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SefinClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPostCompressedDpsXmlToNfseEndpoint() throws Exception {
        startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/SefinNacional/nfse", exchange.getRequestURI().getPath());
            assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
            String body = requestBody(exchange);
            String payload = extractJsonValue(body, "dpsXmlGZipB64");
            assertEquals("<DPS>teste</DPS>", XmlPayloadCodec.ungzipBase64(payload));
            respond(exchange, 201, "{\"chaveAcesso\":\"abc\"}");
        });
        SefinClient client = new SefinClient(context());

        NfseHttpResponse response = client.emitirNfseXml("<DPS>teste</DPS>");

        assertEquals(201, response.statusCode());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldConsultNfseByEncodedAccessKey() throws Exception {
        startServer(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/SefinNacional/nfse/chave%20com%20espaco", exchange.getRequestURI().getRawPath());
            respond(exchange, 200, "{\"chaveAcesso\":\"ok\"}");
        });
        SefinClient client = new SefinClient(context());

        NfseHttpResponse response = client.consultarNfse("chave com espaco");

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldConsultAndVerifyDps() throws Exception {
        startServer(exchange -> {
            assertEquals("/SefinNacional/dps/DPS123", exchange.getRequestURI().getPath());
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            assertEquals("GET", exchange.getRequestMethod());
            respond(exchange, 200, "{\"idDps\":\"DPS123\"}");
        });
        SefinClient client = new SefinClient(context());

        assertEquals(200, client.consultarDps("DPS123").statusCode());
        assertTrue(client.verificarDps("DPS123"));
    }

    @Test
    void shouldPostCompressedEventXml() throws Exception {
        startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/SefinNacional/nfse/CHAVE1/eventos", exchange.getRequestURI().getPath());
            String payload = extractJsonValue(requestBody(exchange), "pedidoRegistroEventoXmlGZipB64");
            assertEquals("<Evento/>", XmlPayloadCodec.ungzipBase64(payload));
            respond(exchange, 200, "{\"eventoXmlGZipB64\":\"ok\"}");
        });
        SefinClient client = new SefinClient(context());

        NfseHttpResponse response = client.registrarEventoXml("CHAVE1", "<Evento/>");

        assertEquals(200, response.statusCode());
    }

    private NfseContext context() {
        return NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .endpointResolver(EndpointResolver.withSefinEndpoints(baseUri(), URI.create("https://producao.test")))
            .build();
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/SefinNacional");
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/SefinNacional/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    private static String requestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String extractJsonValue(String json, String key) {
        String prefix = "\"" + key + "\":\"";
        int start = json.indexOf(prefix);
        if (start < 0) {
            throw new AssertionError("JSON sem chave " + key + ": " + json);
        }
        start += prefix.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
