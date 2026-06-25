package br.com.nfse.danfse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DanfseGeneratorTest {

    private static String pdfTexto(byte[] pdf) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void geraPdfValidoComOsDadosDaNota() throws Exception {
        String xml = NfseXmlReaderTest.xmlExemplo();

        byte[] pdf = DanfseGenerator.gerarPdf(xml, false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000, "PDF deve ter conteudo");
        // header %PDF
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F', "deve ser um PDF");

        String texto = pdfTexto(pdf);
        assertTrue(texto.contains("DANFSe"), "tem o titulo");
        assertTrue(texto.contains("35503082212345678000199000000000001234500000000000001"), "tem a chave de acesso");
        assertTrue(texto.contains("12345"), "tem o numero da NFS-e");
        assertTrue(texto.contains("EMPRESA EXEMPLO LTDA"), "tem o prestador");
        assertTrue(texto.contains("Fulano de Tal"), "tem o tomador");
        assertTrue(texto.contains("R$ 250,00"), "tem o valor formatado");
        assertTrue(texto.contains("12.345.678/0001-99"), "CNPJ mascarado");
        assertTrue(texto.contains("111.444.777-35"), "CPF mascarado");
        // XML ficticio e homologacao (ambGer=2) -> aviso obrigatorio
        assertTrue(texto.contains("SEM VALIDADE"), "aviso de homologacao");
    }

    @Test
    void logoDoEmitenteEntraNoCabecalhoSomenteQuandoInformado() throws Exception {
        Danfse d = NfseXmlReader.read(NfseXmlReaderTest.xmlExemplo());
        String dataUri = "data:image/png;base64,AAAA";

        String comLogo = DanfseHtmlRenderer.render(d, null, null, DanfseConfig.comLogoEmitente(dataUri));
        assertTrue(comLogo.contains("class=\"logo-emit\""), "deve incluir a img do logo do emitente");
        assertTrue(comLogo.contains(dataUri), "deve embutir a imagem do emitente");

        String semLogo = DanfseHtmlRenderer.render(d, null, null, DanfseConfig.vazio());
        assertFalse(semLogo.contains("class=\"logo-emit\""), "sem config nao deve haver img do emitente");
        assertFalse(semLogo.contains(dataUri), "sem config nao deve embutir imagem");
    }

    @Test
    void producaoNaoMostraAvisoDeHomologacao() throws Exception {
        // produção = tpAmb=1 (ambGer permanece 2, como nas notas reais).
        String prod = NfseXmlReaderTest.xmlExemplo().replace("<tpAmb>2</tpAmb>", "<tpAmb>1</tpAmb>");
        byte[] pdf = DanfseGenerator.gerarPdf(prod, true);
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(!texto.contains("SEM VALIDADE"), "producao nao deve ter aviso de homologacao");
        }
    }

    @Test
    void gravaPdfNoArquivo(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path saida = dir.resolve("danfse.pdf");
        DanfseGenerator.gerarPdf(NfseXmlReaderTest.xmlExemplo(), false, saida);
        assertTrue(java.nio.file.Files.exists(saida));
        assertTrue(java.nio.file.Files.size(saida) > 1000);
    }
}
