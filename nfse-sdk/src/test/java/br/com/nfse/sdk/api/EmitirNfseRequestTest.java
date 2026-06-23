package br.com.nfse.sdk.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.nfse.sdk.api.EmitirNfseRequest.PrestadorRequest;
import br.com.nfse.sdk.api.EmitirNfseRequest.ServicoRequest;
import br.com.nfse.sdk.api.EmitirNfseRequest.TomadorRequest;
import br.com.nfse.sdk.api.EmitirNfseRequest.TributacaoRequest;
import br.com.nfse.sdk.xml.dps.Dps;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EmitirNfseRequestTest {

    private static EmitirNfseRequest valida() {
        return new EmitirNfseRequest(
            null, "3550308", "00900", 5L, null, new BigDecimal("100.00"),
            new PrestadorRequest("12345678000199", null, null, null, null, 2, null, 0),
            new TomadorRequest(null, "11144477735", "Fulano", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "Servico de teste", null),
            new TributacaoRequest(1, 1, 0, null));
    }

    @Test
    void mapeiaCamposBasicosEDefaults() {
        Dps dps = valida().toDps("99999999000191");

        assertEquals("1.00", dps.versao());
        assertEquals("3550308", dps.infDps().codigoLocalEmissao());
        assertEquals("00900", dps.infDps().serie());
        assertEquals(5, dps.infDps().numero());
        assertEquals("12345678000199", dps.infDps().prestador().cnpj());
        assertEquals("11144477735", dps.infDps().tomador().cpf());
        assertEquals(0, dps.infDps().valores().valorServico().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void tomadorSemEnderecoFicaSemEndereco() {
        Dps dps = valida().toDps("99999999000191");
        assertNull(dps.infDps().tomador().endereco());
    }

    @Test
    void prestadorNuloHerdaDocumentoDoCertificadoEOpcaoSimplesPadrao() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", null, 1L, null, new BigDecimal("10.00"),
            null,
            new TomadorRequest(null, "11144477735", "Fulano", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, 0, null));

        Dps dps = req.toDps("99999999000191");

        assertEquals("99999999000191", dps.infDps().prestador().cnpj());
        assertEquals(2, dps.infDps().prestador().regimeTributario().opcaoSimplesNacional());
        assertEquals("1", dps.infDps().serie());
    }

    @Test
    void numeroNuloFalha() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", "1", null, null, new BigDecimal("10.00"),
            null,
            new TomadorRequest(null, "1", "x", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, 0, null));
        var ex = assertThrows(IllegalArgumentException.class, () -> req.toDps("99999999000191"));
        assertTrue(ex.getMessage().contains("numero"));
    }

    @Test
    void numeroZeroOuNegativoFalha() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", "1", 0L, null, new BigDecimal("10.00"),
            null,
            new TomadorRequest(null, "1", "x", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, 0, null));
        var ex = assertThrows(IllegalArgumentException.class, () -> req.toDps("99999999000191"));
        assertTrue(ex.getMessage().contains("maior que zero"));
    }

    @Test
    void meEppComIndicadorTotalTributosFalha() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", "1", 1L, null, new BigDecimal("10.00"),
            new PrestadorRequest("12345678000199", null, null, null, null, 3, 1, 0),
            new TomadorRequest(null, "1", "x", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, 0, null)); // indicadorTotalTributos com opSimpNac=3
        var ex = assertThrows(IllegalArgumentException.class, () -> req.toDps("99999999000191"));
        assertTrue(ex.getMessage().contains("ME/EPP"));
    }

    @Test
    void tributacaoSemIndicadorNemPercentualFalha() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", "1", 1L, null, new BigDecimal("10.00"),
            new PrestadorRequest("12345678000199", null, null, null, null, 2, null, 0),
            new TomadorRequest(null, "1", "x", null, null, null, null, null, null, null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, null, null));
        assertThrows(IllegalArgumentException.class, () -> req.toDps("99999999000191"));
    }

    @Test
    void enderecoParcialDoTomadorFalha() {
        EmitirNfseRequest req = new EmitirNfseRequest(
            null, "3550308", "1", 1L, null, new BigDecimal("10.00"),
            null,
            // so bairro preenchido -> exige os demais campos de endereco
            new TomadorRequest(null, "1", "x", null, null, null, null, null, "Centro", null, null),
            new ServicoRequest(null, "010101", null, "x", null),
            new TributacaoRequest(1, 1, 0, null));
        var ex = assertThrows(IllegalArgumentException.class, () -> req.toDps("99999999000191"));
        assertTrue(ex.getMessage().contains("tomador."));
    }
}
