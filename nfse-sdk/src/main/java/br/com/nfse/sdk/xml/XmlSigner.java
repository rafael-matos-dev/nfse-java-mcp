package br.com.nfse.sdk.xml;

import br.com.nfse.sdk.certificate.CertificadoA1;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class XmlSigner {
    private static final String INF_DPS = "infDPS";
    private static final String ID_ATTRIBUTE = "Id";

    private XmlSigner() {
    }

    public static String signInfDps(String xml, CertificadoA1 certificado) {
        return signElement(xml, INF_DPS, certificado);
    }

    public static String signElement(String xml, String elementName, CertificadoA1 certificado) {
        Objects.requireNonNull(xml, "xml is required");
        Objects.requireNonNull(elementName, "elementName is required");
        Objects.requireNonNull(certificado, "certificado is required");

        try {
            Document document = parse(xml);
            Element element = findElement(document, elementName);
            String referenceId = element.getAttribute(ID_ATTRIBUTE);
            if (referenceId.isBlank()) {
                throw new XmlSignerException("Elemento " + elementName + " nao contem atributo Id.");
            }
            element.setIdAttribute(ID_ATTRIBUTE, true);

            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = signatureFactory.newReference(
                "#" + referenceId,
                signatureFactory.newDigestMethod(DigestMethod.SHA256, null),
                List.of(signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                null
            );
            SignedInfo signedInfo = signatureFactory.newSignedInfo(
                signatureFactory.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null
                ),
                signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                List.of(reference)
            );

            X509Certificate certificate = certificado.certificate();
            KeyInfo keyInfo = keyInfo(signatureFactory, certificate);
            PrivateKey privateKey = certificado.privateKey();
            DOMSignContext signContext = new DOMSignContext(privateKey, document.getDocumentElement());

            XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
            signature.sign(signContext);

            return toXml(document);
        } catch (XmlSignerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XmlSignerException("Nao foi possivel assinar o XML.", exception);
        }
    }

    private static KeyInfo keyInfo(XMLSignatureFactory signatureFactory, X509Certificate certificate) {
        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(List.of(certificate));
        return keyInfoFactory.newKeyInfo(List.of(x509Data));
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder()
            .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Element findElement(Document document, String elementName) {
        NodeList elements = document.getElementsByTagNameNS("*", elementName);
        if (elements.getLength() != 1) {
            throw new XmlSignerException("XML deve conter exatamente um elemento " + elementName + ".");
        }
        return (Element) elements.item(0);
    }

    private static String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString().replace("\n", "").replace("\r", "");
    }
}
