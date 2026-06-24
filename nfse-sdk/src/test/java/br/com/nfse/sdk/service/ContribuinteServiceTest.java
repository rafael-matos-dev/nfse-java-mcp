package br.com.nfse.sdk.service;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.certificate.TestPkcs12Factory;
import br.com.nfse.sdk.http.EndpointResolver;
import br.com.nfse.sdk.http.NfseHttpResponse;
import br.com.nfse.sdk.xml.XmlPayloadCodec;
import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsIdGenerator;
import br.com.nfse.sdk.xml.evento.CancelamentoNfse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContribuinteServiceTest {
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
    void shouldBuildSignAndPostDpsWhenEmitting() throws Exception {
        CertificadoA1 certificado = certificado();
        startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/SefinNacional/nfse", exchange.getRequestURI().getPath());

            String payload = extractJsonValue(requestBody(exchange), "dpsXmlGZipB64");
            String signedXml = XmlPayloadCodec.ungzipBase64(payload);
            assertTrue(signedXml.contains("<Signature"));
            assertValidSignature(signedXml, certificado);
            respond(exchange, 201, "{\"chaveAcesso\":\"abc\"}");
        });

        ContribuinteService service = new ContribuinteService(context(certificado));

        NfseHttpResponse response = service.emitir(minimalDps());

        assertEquals(201, response.statusCode());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldRequireCertificateWhenEmittingDps() {
        NfseContext context = NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .build();
        ContribuinteService service = new ContribuinteService(context);

        ContribuinteServiceException exception = assertThrows(
            ContribuinteServiceException.class,
            () -> service.emitir(minimalDps())
        );

        assertEquals("Certificado A1 e obrigatorio para emitir DPS.", exception.getMessage());
    }

    @Test
    void shouldBuildSignAndPostCancellationEvent() throws Exception {
        CertificadoA1 certificado = certificado();
        startServer(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals(
                "/SefinNacional/nfse/NFSE123/eventos",
                exchange.getRequestURI().getPath()
            );

            String payload = extractJsonValue(requestBody(exchange), "pedidoRegistroEventoXmlGZipB64");
            String signedXml = XmlPayloadCodec.ungzipBase64(payload);
            // jan/2026: nPedRegEvento removido do Id (TSIdPedRegEvt: PRE[0-9]{56}) — sem o sufixo "001".
            assertTrue(signedXml.contains("<infPedReg Id=\"PRENFSE123101101\">"));
            assertTrue(signedXml.contains("<tpAmb>2</tpAmb>"));
            assertTrue(signedXml.contains("<CNPJAutor>12345678000195</CNPJAutor>"));
            assertTrue(signedXml.contains("<e101101><xDesc>Cancelamento de NFS-e</xDesc>"));
            assertTrue(signedXml.contains("<cMotivo>2</cMotivo>"));
            assertTrue(signedXml.contains("<xMotivo>Servico nao prestado</xMotivo>"));
            assertValidSignature(signedXml, "infPedReg", certificado);
            respond(exchange, 200, "{\"evento\":\"ok\"}");
        });

        ContribuinteService service = new ContribuinteService(context(certificado));

        NfseHttpResponse response = service.cancelar(cancelamento());

        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccessful());
    }

    @Test
    void shouldRequireCertificateWhenCancellingNfse() {
        NfseContext context = NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .build();
        ContribuinteService service = new ContribuinteService(context);

        ContribuinteServiceException exception = assertThrows(
            ContribuinteServiceException.class,
            () -> service.cancelar(cancelamento())
        );

        assertEquals("Certificado A1 e obrigatorio para cancelar NFS-e.", exception.getMessage());
    }

    private CertificadoA1 certificado() throws Exception {
        char[] password = "senha-teste".toCharArray();
        Path certificatePath = tempDir.resolve("certificado-teste.p12");
        TestPkcs12Factory.create(certificatePath, password, "nfse-test", "12345678000195");
        return CertificadoA1.fromFile(certificatePath, password);
    }

    private NfseContext context(CertificadoA1 certificado) {
        return NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .certificado(certificado)
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

    private static boolean isSignatureValid(String xml, CertificadoA1 certificado) throws Exception {
        return isSignatureValid(xml, "infDPS", certificado);
    }

    private static boolean isSignatureValid(String xml, String signedElement, CertificadoA1 certificado) throws Exception {
        Document document = parse(xml);
        Element element = (Element) document.getElementsByTagNameNS("*", signedElement).item(0);
        element.setIdAttribute("Id", true);

        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        DOMValidateContext context = new DOMValidateContext(
            certificado.certificate().getPublicKey(),
            signatures.item(0)
        );
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        return signature.validate(context);
    }

    private static void assertValidSignature(String xml, CertificadoA1 certificado) {
        assertValidSignature(xml, "infDPS", certificado);
    }

    private static void assertValidSignature(String xml, String signedElement, CertificadoA1 certificado) {
        try {
            assertTrue(isSignatureValid(xml, signedElement, certificado));
        } catch (Exception exception) {
            throw new AssertionError("Assinatura XML invalida.", exception);
        }
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder()
            .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
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

    private static Dps minimalDps() {
        return new Dps(
            "1.01",
            new Dps.InfDps(
                DpsIdGenerator.generate("12345678000195", "3129806", "70000", 24),
                2,
                OffsetDateTime.parse("2026-06-09T12:46:36-03:00"),
                "nfse-nacional-kit",
                "70000",
                24,
                LocalDate.parse("2026-05-22"),
                1,
                "3129806",
                new Dps.Prestador(
                    "12345678000195",
                    null,
                    "31999999999",
                    "prestador@example.test",
                    new Dps.RegimeTributario(2, 0)
                ),
                new Dps.Tomador(
                    "98765432000198",
                    null,
                    "TOMADOR EXEMPLO LTDA",
                    new Dps.Endereco("3129806", "32432025", "RUA EXEMPLO", "01", null, "CENTRO"),
                    null,
                    null
                ),
                new Dps.Servico("3129806", "141001", "Servico de lavanderia conforme pedido de teste"),
                new Dps.Valores(new BigDecimal("442.00"), new Dps.Tributacao(1, 1, 0))
            )
        );
    }

    private static CancelamentoNfse cancelamento() {
        return new CancelamentoNfse(
            "NFSE123",
            "12.345.678/0001-95",
            OffsetDateTime.parse("2026-06-09T13:10:00-03:00"),
            1,
            "2",
            "Servico nao prestado",
            "nfse-nacional-kit"
        );
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
