package br.com.nfse.sdk.xml.dps;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DpsXmlBuilderTest {

    @Test
    void shouldBuildMinimalDpsXmlUsingProductionExampleShape() {
        Dps dps = new Dps(
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
                    new Dps.Endereco(
                        "3129806",
                        "32432025",
                        "RUA EXEMPLO",
                        "01",
                        null,
                        "CENTRO"
                    ),
                    null,
                    null
                ),
                new Dps.Servico(
                    "3129806",
                    "141001",
                    "Servico de lavanderia conforme pedido de teste"
                ),
                new Dps.Valores(
                    new BigDecimal("442.00"),
                    new Dps.Tributacao(1, 1, 0)
                )
            )
        );

        String xml = new DpsXmlBuilder().build(dps);

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><DPS "));
        assertTrue(xml.contains("xmlns=\"http://www.sped.fazenda.gov.br/nfse\""));
        assertTrue(xml.contains("versao=\"1.01\""));
        assertTrue(xml.contains("<infDPS Id=\"DPS312980621234567800019570000000000000000024\">"));
        assertTrue(xml.contains("<tpAmb>2</tpAmb>"));
        assertTrue(xml.contains("<prest><CNPJ>12345678000195</CNPJ>"));
        assertTrue(xml.contains("<fone>31999999999</fone>"));
        assertTrue(xml.contains("<toma><CNPJ>98765432000198</CNPJ><xNome>TOMADOR EXEMPLO LTDA</xNome>"));
        assertTrue(xml.contains("<cTribNac>141001</cTribNac>"));
        assertTrue(xml.contains("<vServ>442.00</vServ>"));
        assertTrue(xml.contains("<indTotTrib>0</indTotTrib>"));
        // dhEmi sempre no fuso oficial (-03:00), mesmo o instante vindo em -03:00 aqui.
        assertTrue(xml.contains("<dhEmi>2026-06-09T12:46:36-03:00</dhEmi>"), xml);
    }

    @Test
    void dhEmiSempreNoFusoDeSaoPauloMesmoComInstanteEmUtc() {
        // instante UTC equivalente a 2026-06-09T12:46:36-03:00 => 15:46:36Z
        Dps dps = new Dps("1.01", new Dps.InfDps(
            DpsIdGenerator.generate("12345678000195", "3129806", "1", 1),
            2, OffsetDateTime.parse("2026-06-09T15:46:36Z"), "x", "1", 1,
            LocalDate.parse("2026-05-22"), 1, "3129806",
            new Dps.Prestador("12345678000195", null, null, null, new Dps.RegimeTributario(2, 0)),
            new Dps.Tomador(null, "11144477735", "X", null, null, null),
            new Dps.Servico("3129806", "141001", "x"),
            new Dps.Valores(new BigDecimal("10.00"), new Dps.Tributacao(1, 1, 0))));

        String xml = new DpsXmlBuilder().build(dps);

        assertTrue(xml.contains("<dhEmi>2026-06-09T12:46:36-03:00</dhEmi>"), xml);
    }
}
