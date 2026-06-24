package br.com.nfse.danfse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Cobertura estrutural da seção IBS/CBS (NT 009) — sem exemplo oficial, validamos estrutura. */
class IbsCbsTest {

    private static String xml() throws Exception {
        try (var in = IbsCbsTest.class.getResourceAsStream("/nfse-exemplo-ibscbs.xml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void leTotaisIbsCbsDoBloco() throws Exception {
        Danfse.IbsCbs ibs = NfseXmlReader.read(xml()).ibsCbs();
        assertNotNull(ibs, "bloco IBSCBS deve ser lido");
        assertTrue(ibs.valorTotalIbs().compareTo(new BigDecimal("88.50")) == 0);
        assertTrue(ibs.valorTotalCbs().compareTo(new BigDecimal("91.50")) == 0);
    }

    @Test
    void renderizaSecaoIbsCbsNoPdf() throws Exception {
        byte[] pdf = DanfseGenerator.gerarPdf(xml(), false);
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(texto.contains("IBS / CBS"), "secao IBS/CBS presente");
            assertTrue(texto.contains("R$ 88,50"), "valor IBS");
            assertTrue(texto.contains("R$ 91,50"), "valor CBS");
        }
    }
}
