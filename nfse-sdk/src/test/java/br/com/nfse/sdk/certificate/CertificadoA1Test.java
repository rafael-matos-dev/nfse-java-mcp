package br.com.nfse.sdk.certificate;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificadoA1Test {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadPkcs12CertificateAndExposeSafeMetadata() throws Exception {
        char[] password = "senha-teste".toCharArray();
        Path certificatePath = tempDir.resolve("certificado-teste.p12");
        TestPkcs12Factory.create(certificatePath, password, "nfse-test", "12345678000195");

        CertificadoA1 certificado = CertificadoA1.fromFile(certificatePath, password);

        assertEquals("nfse-test", certificado.alias());
        assertTrue(certificado.subject().contains("12345678000195"));
        assertEquals("12345678000195", certificado.cpfCnpj().orElseThrow());
        assertFalse(certificado.isExpired(Clock.fixed(Instant.now(), ZoneOffset.UTC)));
        assertNotNull(certificado.certificate());
    }

    @Test
    void shouldRejectWrongPasswordWithoutLeakingThePassword() throws Exception {
        char[] password = "senha-correta".toCharArray();
        Path certificatePath = tempDir.resolve("certificado-teste.p12");
        TestPkcs12Factory.create(certificatePath, password, "nfse-test", "12345678000195");

        CertificadoA1Exception exception = assertThrows(
            CertificadoA1Exception.class,
            () -> CertificadoA1.fromFile(certificatePath, "senha-errada".toCharArray())
        );

        assertFalse(exception.getMessage().contains("senha-errada"));
        assertFalse(exception.getMessage().contains(certificatePath.toString()));
    }

    @Test
    void shouldCreateSslContextFromCertificate() throws Exception {
        char[] password = "senha-teste".toCharArray();
        Path certificatePath = tempDir.resolve("certificado-teste.p12");
        TestPkcs12Factory.create(certificatePath, password, "nfse-test", "12345678000195");
        CertificadoA1 certificado = CertificadoA1.fromFile(certificatePath, password);

        SSLContext sslContext = SSLContextFactory.create(certificado);

        assertNotNull(sslContext);
        assertEquals("TLS", sslContext.getProtocol());
    }
}
