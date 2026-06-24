package br.com.nfse.danfse;

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
 * Le o XML autorizado da NFS-e ({@code <NFSe><infNFSe>...<DPS><infDPS>}) e monta o {@link Danfse}.
 * Combina campos do nivel administrativo (infNFSe: numeros, municipios por nome, emitente, valor
 * liquido, IBS/CBS) com os da DPS embutida (tomador, servico, tributacao). DOM do JDK (sem libs).
 */
public final class NfseXmlReader {

    private NfseXmlReader() {
    }

    public static Danfse read(String xml) {
        Objects.requireNonNull(xml, "xml is required");
        try {
            Document doc = parse(xml);
            Element infNfse = firstByLocalName(doc.getDocumentElement(), "infNFSe");
            if (infNfse == null) {
                throw new DanfseException("XML nao contem elemento infNFSe (esperado o XML autorizado da NFS-e).");
            }
            Element dps = firstByLocalName(infNfse, "infDPS");
            Element emit = firstByLocalName(infNfse, "emit");
            Element prest = dps == null ? null : firstByLocalName(dps, "prest");
            Element toma = dps == null ? null : firstByLocalName(dps, "toma");
            Element serv = dps == null ? null : firstByLocalName(dps, "serv");
            Element trib = dps == null ? null : firstByLocalName(dps, "trib");

            return new Danfse(
                chaveAcesso(infNfse),
                identificacao(infNfse, dps),
                prestador(emit, prest),
                tomador(toma),
                servico(infNfse, serv, trib),
                valores(infNfse, dps, trib),
                primeiroNaoVazio(text(infNfse, "xOutInf"), text(dps, "xInfComp")),
                ibsCbs(firstByLocalName(infNfse, "IBSCBS"))
            );
        } catch (DanfseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DanfseException("Nao foi possivel ler o XML da NFS-e.", exception);
        }
    }

    private static String chaveAcesso(Element infNfse) {
        String id = infNfse.getAttribute("Id");
        if (id == null || id.isBlank()) {
            return null;
        }
        return id.startsWith("NFS") ? id.substring(3) : id;
    }

    private static Danfse.Identificacao identificacao(Element infNfse, Element dps) {
        return new Danfse.Identificacao(
            text(infNfse, "nNFSe"),
            dateOrNull(text(dps, "dCompet")),
            offsetOrNull(text(infNfse, "dhProc")),
            text(dps, "nDPS"),
            text(dps, "serie"),
            offsetOrNull(text(dps, "dhEmi")),
            text(infNfse, "xLocEmi"),
            text(infNfse, "xLocPrestacao")
        );
    }

    private static Danfse.Pessoa prestador(Element emit, Element prest) {
        Element regTrib = prest == null ? null : firstByLocalName(prest, "regTrib");
        return new Danfse.Pessoa(
            text(emit, "CNPJ"),
            text(emit, "CPF"),
            text(emit, "IM"),
            text(emit, "xNome"),
            text(emit, "fone"),
            text(emit, "email"),
            endereco(emit == null ? null : firstByLocalName(emit, "enderNac"), false),
            simplesNacional(text(regTrib, "opSimpNac")),
            regimeApuracao(text(regTrib, "regApTribSN"))
        );
    }

    private static Danfse.Pessoa tomador(Element toma) {
        if (toma == null) {
            return null;
        }
        Element end = firstByLocalName(toma, "end");
        return new Danfse.Pessoa(
            text(toma, "CNPJ"),
            text(toma, "CPF"),
            text(toma, "IM"),
            text(toma, "xNome"),
            text(toma, "fone"),
            text(toma, "email"),
            endereco(end, true),
            null,
            null
        );
    }

    /** No emit o endereco fica direto em enderNac; no tomador ha um nivel endNac aninhado dentro de end. */
    private static Danfse.Endereco endereco(Element escopo, boolean tomador) {
        if (escopo == null) {
            return null;
        }
        Element nac = tomador ? firstByLocalName(escopo, "endNac") : escopo;
        return new Danfse.Endereco(
            text(escopo, "xLgr"),
            text(escopo, "nro"),
            text(escopo, "xCpl"),
            text(escopo, "xBairro"),
            text(nac, "cMun"),
            text(escopo, "UF"),
            text(nac, "CEP")
        );
    }

    private static Danfse.Servico servico(Element infNfse, Element serv, Element trib) {
        Element tribMun = trib == null ? null : firstByLocalName(trib, "tribMun");
        return new Danfse.Servico(
            text(serv, "cTribNac"),
            text(infNfse, "xTribNac"),
            text(serv, "cTribMun"),
            text(infNfse, "xLocPrestacao"),
            text(serv, "xDescServ"),
            tributacaoIssqn(text(tribMun, "tribISSQN")),
            retencaoIssqn(text(tribMun, "tpRetISSQN")),
            text(infNfse, "xLocIncid")
        );
    }

    private static Danfse.Valores valores(Element infNfse, Element dps, Element trib) {
        Element valoresDps = dps == null ? null : firstByLocalName(dps, "valores");
        Element vServPrest = valoresDps == null ? null : firstByLocalName(valoresDps, "vServPrest");
        Element vDescCondIncond = valoresDps == null ? null : firstByLocalName(valoresDps, "vDescCondIncond");
        Element infNfseValores = firstByLocalName(infNfse, "valores");
        return new Danfse.Valores(
            decimalOrNull(text(vServPrest, "vServ")),
            decimalOrNull(text(vDescCondIncond, "vDescIncond")),
            decimalOrNull(text(vDescCondIncond, "vDescCond")),
            decimalOrNull(text(infNfseValores, "vLiq")),
            null,
            null,
            null
        );
    }

    private static Danfse.IbsCbs ibsCbs(Element ibsCbs) {
        if (ibsCbs == null) {
            return null;
        }
        // Estrutural (NT 009): totais ficam em totCIBS (vIBSTot = IBS, vCBS = CBS).
        // Refinar quando houver exemplo real de nota com reforma tributaria.
        return new Danfse.IbsCbs(
            decimalOrNull(text(ibsCbs, "vIBSTot")),
            decimalOrNull(text(ibsCbs, "vCBS")),
            text(ibsCbs, "xLocalidadeIncid")
        );
    }

    // ---- mapeamentos de codigos para rotulos (fallback: o proprio codigo) ----

    private static String simplesNacional(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "1" -> "Nao Optante";
            case "2" -> "Optante - Microempreendedor Individual (MEI)";
            case "3" -> "Optante - Microempresa ou Empresa de Pequeno Porte (ME/EPP)";
            default -> codigo;
        };
    }

    private static String regimeApuracao(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "1" -> "Regime de apuracao dos tributos federais e municipal pelo SN";
            case "2" -> "Regime de apuracao dos tributos federais pelo SN e ISSQN por fora do SN";
            case "3" -> "Tributos federais e ISSQN por fora do SN";
            default -> codigo;
        };
    }

    private static String tributacaoIssqn(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "1" -> "Operacao Tributavel";
            case "2" -> "Imunidade";
            case "3" -> "Exportacao de servico";
            case "4" -> "Nao Incidencia";
            default -> codigo;
        };
    }

    private static String retencaoIssqn(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "1" -> "Nao Retido";
            case "2" -> "Retido pelo Tomador";
            case "3" -> "Retido pelo Intermediario";
            default -> codigo;
        };
    }

    // ---- helpers DOM (mesma tecnica do DpsXmlReader) ----

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Element firstByLocalName(Element scope, String localName) {
        if (scope == null) {
            return null;
        }
        NodeList children = scope.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
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

    private static String text(Element scope, String localName) {
        Element element = firstByLocalName(scope, localName);
        if (element == null) {
            return null;
        }
        String content = element.getTextContent();
        return content == null || content.isBlank() ? null : content.trim();
    }

    private static String localName(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
    }

    private static String primeiroNaoVazio(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
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
