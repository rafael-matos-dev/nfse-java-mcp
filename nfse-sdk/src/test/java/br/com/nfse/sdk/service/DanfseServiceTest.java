package br.com.nfse.sdk.service;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.http.EndpointResolver;
import br.com.nfse.sdk.http.NfseBinaryResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanfseServiceTest {
    private HttpServer server;

    @TempDir
    Path tempDir;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSaveDownloadedPdfWhenResponseIsSuccessful() throws Exception {
        byte[] pdf = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/danfse/CHAVE1", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, pdf.length);
            exchange.getResponseBody().write(pdf);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        DanfseService service = new DanfseService(context());
        Path outputPath = tempDir.resolve("nota.pdf");

        NfseBinaryResponse response = service.baixarPdf("CHAVE1", outputPath);

        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccessful());
        assertArrayEquals(pdf, Files.readAllBytes(outputPath));
    }

    private NfseContext context() {
        URI danfse = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/danfse");
        return NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .endpointResolver(EndpointResolver.withEndpoints(
                URI.create("http://sefin-homologacao.test"),
                URI.create("http://sefin-producao.test"),
                danfse,
                URI.create("http://danfse-producao.test")
            ))
            .build();
    }
}
