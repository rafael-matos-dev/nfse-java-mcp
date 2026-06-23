package br.com.nfse.sdk.xml.dps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class DpsReemissaoTest {

    private static Dps exemplo() {
        Dps.InfDps inf = new Dps.InfDps(
            DpsIdGenerator.generate("12345678000199", "3550308", "00900", 10),
            1,
            OffsetDateTime.parse("2026-01-15T10:00:00-03:00"),
            "EmissorWeb",
            "00900",
            10,
            LocalDate.parse("2026-01-15"),
            1,
            "3550308",
            new Dps.Prestador("12345678000199", null, "IM123", null, null,
                new Dps.RegimeTributario(2, null, 0)),
            new Dps.Tomador(null, "11122233344", "Tomador Antigo", null, null, null),
            new Dps.Servico("3550308", "010101", "001", "Servico antigo", "123"),
            new Dps.Valores(new BigDecimal("100.00"), new Dps.Tributacao(1, 1, 0, null)));
        return new Dps("1.00", inf);
    }

    @Test
    void apenasNumeroPreservaRestoERegeneraId() {
        Dps re = DpsReemissao.reemitir(exemplo(), new DpsReemissao.Overrides(
            42L, null, null, null, null, null, null));

        assertEquals(42, re.infDps().numero());
        assertEquals(DpsIdGenerator.generate("12345678000199", "3550308", "00900", 42), re.infDps().id());
        // preserva o que nao foi sobrescrito
        assertEquals("Tomador Antigo", re.infDps().tomador().nome());
        assertEquals("Servico antigo", re.infDps().servico().descricao());
        assertEquals(0, re.infDps().valores().valorServico().compareTo(new BigDecimal("100.00")));
        assertEquals("12345678000199", re.infDps().prestador().cnpj());
        // verAplic e marcado como este kit
        assertEquals("nfse-java-mcp", re.infDps().versaoAplicativo());
    }

    @Test
    void sobrescreveTomadorDescricaoValor() {
        Dps re = DpsReemissao.reemitir(exemplo(), new DpsReemissao.Overrides(
            11L, null,
            new Dps.Tomador(null, "55566677788", "Novo Tomador", null, null, null),
            "Novo servico", new BigDecimal("250.00"), null, null));

        assertEquals("55566677788", re.infDps().tomador().cpf());
        assertEquals("Novo Tomador", re.infDps().tomador().nome());
        assertNull(re.infDps().tomador().endereco());
        assertEquals("Novo servico", re.infDps().servico().descricao());
        assertEquals(0, re.infDps().valores().valorServico().compareTo(new BigDecimal("250.00")));
        // tributacao do exemplo preservada
        assertEquals(0, re.infDps().valores().tributacao().indicadorTotalTributos());
    }

    @Test
    void overrideDeSerieEntraNoId() {
        Dps re = DpsReemissao.reemitir(exemplo(), new DpsReemissao.Overrides(
            7L, "00955", null, null, null, null, null));

        assertEquals("00955", re.infDps().serie());
        assertEquals(DpsIdGenerator.generate("12345678000199", "3550308", "00955", 7), re.infDps().id());
        assertNotEquals(exemplo().infDps().id(), re.infDps().id());
    }

    @Test
    void exemploSemPrestadorFalha() {
        Dps.InfDps inf = new Dps.InfDps(
            "DPS1", 1, OffsetDateTime.now(), "x", "1", 1, LocalDate.now(), 1, "3550308",
            null, // prestador ausente
            new Dps.Tomador(null, "1", "x", null, null, null),
            new Dps.Servico("3550308", "010101", "x"),
            new Dps.Valores(new BigDecimal("1.00"), new Dps.Tributacao(1, 1, 0)));
        Dps semPrestador = new Dps("1.00", inf);

        assertThrows(DpsXmlException.class, () -> DpsReemissao.reemitir(
            semPrestador, new DpsReemissao.Overrides(1L, null, null, null, null, null, null)));
    }
}
