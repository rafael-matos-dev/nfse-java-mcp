package br.com.nfse.sdk.xml.dps;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Le um XML existente de DPS (ou de NFSe que embrulha uma DPS, como o DANFSe/retorno da SEFIN)
 * e reconstroi o modelo {@link Dps}. Usado para o fluxo "emitir a partir de uma nota de exemplo":
 * a pessoa aponta uma nota antiga e so troca tomador/descricao/valor (ver {@link DpsReemissao}).
 *
 * <p>Implementacao com DOM do JDK para manter a SDK sem dependencias de runtime.
 */
public final class DpsXmlReader {

    private DpsXmlReader() {
    }

    public static Dps read(String xml) {
        Objects.requireNonNull(xml, "xml is required");
        try {
            Document document = parse(xml);
            Element infDps = firstByLocalName(document.getDocumentElement(), "infDPS");
            if (infDps == null) {
                throw new DpsXmlException("XML nao contem elemento infDPS.");
            }
            String versao = versaoDaDps(infDps);

            Dps.InfDps inf = new Dps.InfDps(
                attr(infDps, "Id"),
                intOrDefault(text(infDps, "tpAmb"), 2),
                offsetOrNull(text(infDps, "dhEmi")),
                text(infDps, "verAplic"),
                text(infDps, "serie"),
                longOrZero(text(infDps, "nDPS")),
                dateOrNull(text(infDps, "dCompet")),
                intOrDefault(text(infDps, "tpEmit"), 1),
                text(infDps, "cLocEmi"),
                prestador(firstByLocalName(infDps, "prest")),
                tomador(firstByLocalName(infDps, "toma")),
                servico(firstByLocalName(infDps, "serv")),
                valores(firstByLocalName(infDps, "valores"))
            );
            return new Dps(versao, inf);
        } catch (DpsXmlException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DpsXmlException("Nao foi possivel ler o XML de exemplo da DPS.", exception);
        }
    }

    private static Dps.Prestador prestador(Element prest) {
        if (prest == null) {
            return null;
        }
        Element regTrib = firstByLocalName(prest, "regTrib");
        Dps.RegimeTributario regime = regTrib == null ? null : new Dps.RegimeTributario(
            integerOrNull(text(regTrib, "opSimpNac")),
            integerOrNull(text(regTrib, "regApTribSN")),
            integerOrNull(text(regTrib, "regEspTrib"))
        );
        return new Dps.Prestador(
            text(prest, "CNPJ"),
            text(prest, "CPF"),
            text(prest, "IM"),
            text(prest, "fone"),
            text(prest, "email"),
            regime
        );
    }

    private static Dps.Tomador tomador(Element toma) {
        if (toma == null) {
            return null;
        }
        Element end = firstByLocalName(toma, "end");
        Dps.Endereco endereco = null;
        if (end != null) {
            endereco = new Dps.Endereco(
                text(end, "cMun"),
                text(end, "CEP"),
                text(end, "xLgr"),
                text(end, "nro"),
                text(end, "xCpl"),
                text(end, "xBairro")
            );
        }
        return new Dps.Tomador(
            text(toma, "CNPJ"),
            text(toma, "CPF"),
            text(toma, "xNome"),
            endereco,
            text(toma, "fone"),
            text(toma, "email")
        );
    }

    private static Dps.Servico servico(Element serv) {
        if (serv == null) {
            return null;
        }
        return new Dps.Servico(
            text(serv, "cLocPrestacao"),
            text(serv, "cTribNac"),
            text(serv, "cTribMun"),
            text(serv, "xDescServ"),
            text(serv, "cNBS")
        );
    }

    private static Dps.Valores valores(Element valores) {
        if (valores == null) {
            return null;
        }
        Dps.Tributacao tributacao = new Dps.Tributacao(
            integerOrNull(text(valores, "tribISSQN")),
            integerOrNull(text(valores, "tpRetISSQN")),
            integerOrNull(text(valores, "indTotTrib")),
            decimalOrNull(text(valores, "pTotTribSN"))
        );
        return new Dps.Valores(decimalOrNull(text(valores, "vServ")), tributacao);
    }

    private static String versaoDaDps(Element infDps) {
        Node parent = infDps.getParentNode();
        if (parent instanceof Element dps && "DPS".equals(localName(dps))) {
            String versao = dps.getAttribute("versao");
            if (versao != null && !versao.isBlank()) {
                return versao;
            }
        }
        return "1.00";
    }

    // ---- helpers DOM ----

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Primeiro descendente (busca em profundidade) com o local name informado, ou null. */
    private static Element firstByLocalName(Element scope, String localName) {
        if (scope == null) {
            return null;
        }
        NodeList children = scope.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element) {
                if (localName.equals(localName(element))) {
                    return element;
                }
                Element nested = firstByLocalName(element, localName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /** Texto do primeiro descendente com o local name informado, ou null. */
    private static String text(Element scope, String localName) {
        Element element = firstByLocalName(scope, localName);
        if (element == null) {
            return null;
        }
        String content = element.getTextContent();
        return content == null || content.isBlank() ? null : content.trim();
    }

    private static String attr(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static String localName(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
    }

    private static Integer integerOrNull(String value) {
        return value == null ? null : Integer.valueOf(value.trim());
    }

    private static int intOrDefault(String value, int fallback) {
        return value == null ? fallback : Integer.parseInt(value.trim());
    }

    private static long longOrZero(String value) {
        return value == null ? 0L : Long.parseLong(value.trim());
    }

    private static BigDecimal decimalOrNull(String value) {
        return value == null ? null : new BigDecimal(value.trim());
    }

    private static LocalDate dateOrNull(String value) {
        return value == null ? null : LocalDate.parse(value.trim());
    }

    private static OffsetDateTime offsetOrNull(String value) {
        return value == null ? null : OffsetDateTime.parse(value.trim());
    }
}
