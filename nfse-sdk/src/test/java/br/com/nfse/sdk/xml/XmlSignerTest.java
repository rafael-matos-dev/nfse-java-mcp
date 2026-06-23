package br.com.nfse.sdk.xml;

import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.certificate.TestPkcs12Factory;
import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsIdGenerator;
import br.com.nfse.sdk.xml.dps.DpsXmlBuilder;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlSignerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSignInfDpsAndValidateXmlSignature() throws Exception {
        char[] password = "senha-teste".toCharArray();
        Path certificatePath = tempDir.resolve("certificado-teste.p12");
        TestPkcs12Factory.create(certificatePath, password, "nfse-test", "12345678000195");
        CertificadoA1 certificado = CertificadoA1.fromFile(certificatePath, password);

        String xml = new DpsXmlBuilder().build(minimalDps());
        String signedXml = XmlSigner.signInfDps(xml, certificado);

        Document document = parse(signedXml);
        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, signatures.getLength());
        assertTrue(signedXml.contains("<Signature"));
        assertTrue(isSignatureValid(document, certificado));
    }

    private static boolean isSignatureValid(Document document, CertificadoA1 certificado) throws Exception {
        Element infDps = (Element) document.getElementsByTagNameNS("*", "infDPS").item(0);
        infDps.setIdAttribute("Id", true);

        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        DOMValidateContext context = new DOMValidateContext(
            certificado.certificate().getPublicKey(),
            signatures.item(0)
        );
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        return signature.validate(context);
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder()
            .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
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
}
