package br.com.nfse.sdk.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.api.EmitirNfseRequest.ServicoRequest;
import br.com.nfse.sdk.api.EmitirNfseRequest.TomadorRequest;
import br.com.nfse.sdk.api.EmitirNfseRequest.TributacaoRequest;
import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.certificate.TestPkcs12Factory;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NfseRunnerTest {

    private static final char[] SENHA = "test-pass".toCharArray();
    private static final String CNPJ = "12345678000199";

    private static EmitirNfseRequest request() {
        return new EmitirNfseRequest(
            null, "3550308", "1", 1L, null, new BigDecimal("100.00"),
            null,
            new TomadorRequest(null, "11144477735", "Fulano", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "Servico de teste", null),
            new TributacaoRequest(1, 1, 0, null));
    }

    private static CertificadoA1 certValido(Path dir) throws Exception {
        Path p = dir.resolve("valido.p12");
        TestPkcs12Factory.create(p, SENHA, "1", CNPJ);
        return CertificadoA1.fromFile(p, SENHA);
    }

    @Test
    void ambienteResolveValores() {
        assertEquals(Ambiente.HOMOLOGACAO, NfseRunner.ambiente(null));
        assertEquals(Ambiente.HOMOLOGACAO, NfseRunner.ambiente("homologacao"));
        assertEquals(Ambiente.PRODUCAO, NfseRunner.ambiente("producao"));
        assertThrows(IllegalArgumentException.class, () -> NfseRunner.ambiente("xpto"));
    }

    @Test
    void certExtraiDadosDoCertificado(@TempDir Path dir) throws Exception {
        NfseRunner.CertInfo info = NfseRunner.cert(certValido(dir));
        assertEquals(CNPJ, info.cpfCnpj());
        assertFalse(info.expirado());
    }

    @Test
    void emitirEmProducaoSemConfirmacaoFalhaAntesDaRede(@TempDir Path dir) throws Exception {
        var ex = assertThrows(IllegalStateException.class,
            () -> NfseRunner.emitir(request(), Ambiente.PRODUCAO, certValido(dir), false));
        assertTrue(ex.getMessage().contains("PRODUCAO"));
    }

    @Test
    void cancelarEmProducaoSemConfirmacaoFalhaAntesDaRede(@TempDir Path dir) throws Exception {
        var ex = assertThrows(IllegalStateException.class,
            () -> NfseRunner.cancelar("CHAVE", null, 1, "1", "motivo",
                Ambiente.PRODUCAO, certValido(dir), false));
        assertTrue(ex.getMessage().contains("PRODUCAO"));
    }

    @Test
    void emitirComCertificadoExpiradoFalha(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("expirado.p12");
        TestPkcs12Factory.createExpired(p, SENHA, "1", CNPJ);
        CertificadoA1 expirado = CertificadoA1.fromFile(p, SENHA);

        var ex = assertThrows(IllegalStateException.class,
            () -> NfseRunner.emitir(request(), Ambiente.HOMOLOGACAO, expirado, false));
        assertTrue(ex.getMessage().contains("validade"));
    }
}
