package br.com.nfse.sdk.xml.dps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class DpsXmlReaderTest {

    private static Dps exemplo() {
        Dps.InfDps inf = new Dps.InfDps(
            DpsIdGenerator.generate("12345678000199", "3550308", "1", 10),
            2,
            OffsetDateTime.parse("2026-01-15T10:00:00-03:00"),
            "nfse-java-mcp",
            "1",
            10,
            LocalDate.parse("2026-01-15"),
            1,
            "3550308",
            new Dps.Prestador("12345678000199", null, "123", "1133334444", "prestador@exemplo.com",
                new Dps.RegimeTributario(3, 1, 0)),
            new Dps.Tomador(null, "11122233344", "Fulano de Tal",
                new Dps.Endereco("3550308", "01000000", "Rua A", "100", null, "Centro"), null, null),
            new Dps.Servico("3550308", "010101", "001", "Servico de teste", "123012100"),
            new Dps.Valores(new BigDecimal("250.00"), new Dps.Tributacao(1, 1, null, new BigDecimal("6.00")))
        );
        return new Dps("1.00", inf);
    }

    @Test
    void leDeVoltaOQueFoiConstruido() {
        String xml = new DpsXmlBuilder().build(exemplo());

        Dps lido = DpsXmlReader.read(xml);

        assertEquals("3550308", lido.infDps().codigoLocalEmissao());
        assertEquals("12345678000199", lido.infDps().prestador().cnpj());
        assertEquals(3, lido.infDps().prestador().regimeTributario().opcaoSimplesNacional());
        assertEquals("11122233344", lido.infDps().tomador().cpf());
        assertEquals("Centro", lido.infDps().tomador().endereco().bairro());
        assertEquals("010101", lido.infDps().servico().codigoTributacaoNacional());
        assertEquals(0, lido.infDps().valores().valorServico().compareTo(new BigDecimal("250.00")));
        assertEquals(0, lido.infDps().valores().tributacao()
            .percentualTotalTributosSimplesNacional().compareTo(new BigDecimal("6.00")));
    }

    @Test
    void reemiteAplicandoOverridesERegeneraId() {
        Dps lido = DpsXmlReader.read(new DpsXmlBuilder().build(exemplo()));

        Dps re = DpsReemissao.reemitir(lido, new DpsReemissao.Overrides(
            99L,
            null,
            new Dps.Tomador(null, "55566677788", "Beltrano", null, null, null),
            "Nova descricao",
            new BigDecimal("500.00"),
            null,
            null
        ));

        assertEquals(99, re.infDps().numero());
        assertEquals(DpsIdGenerator.generate("12345678000199", "3550308", "1", 99), re.infDps().id());
        assertEquals("55566677788", re.infDps().tomador().cpf());
        assertEquals("Beltrano", re.infDps().tomador().nome());
        assertNull(re.infDps().tomador().endereco());
        assertEquals("Nova descricao", re.infDps().servico().descricao());
        assertEquals(0, re.infDps().valores().valorServico().compareTo(new BigDecimal("500.00")));
        // tributacao e prestador preservados do exemplo
        assertEquals(0, re.infDps().valores().tributacao()
            .percentualTotalTributosSimplesNacional().compareTo(new BigDecimal("6.00")));
        assertEquals("12345678000199", re.infDps().prestador().cnpj());
        assertEquals("nfse-java-mcp", re.infDps().versaoAplicativo());
    }
}
