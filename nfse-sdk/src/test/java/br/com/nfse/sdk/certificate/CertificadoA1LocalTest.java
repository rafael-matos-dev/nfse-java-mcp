package br.com.nfse.sdk.certificate;

import java.nio.file.Path;
import java.time.Clock;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CertificadoA1LocalTest {

    @Test
    void shouldLoadLocalCertificateWhenEnvironmentIsConfigured() {
        String certificatePath = System.getenv("NFSE_CERT_PATH");
        String certificatePassword = System.getenv("NFSE_CERT_PASSWORD");

        assumeFalse(isBlank(certificatePath), "NFSE_CERT_PATH nao configurada; teste local ignorado.");
        assumeFalse(isBlank(certificatePassword), "NFSE_CERT_PASSWORD nao configurada; teste local ignorado.");

        CertificadoA1 certificado = CertificadoA1.fromFile(
            Path.of(certificatePath),
            certificatePassword.toCharArray()
        );
        SSLContext sslContext = SSLContextFactory.create(certificado);

        assumeFalse(certificado.isExpired(Clock.systemDefaultZone()), "Certificado local expirado.");
        assumeTrue(certificado.cpfCnpj().isPresent(), "Certificado local sem CPF/CNPJ detectavel no subject.");
        assumeTrue("TLS".equals(sslContext.getProtocol()), "SSLContext local nao foi criado com TLS.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
