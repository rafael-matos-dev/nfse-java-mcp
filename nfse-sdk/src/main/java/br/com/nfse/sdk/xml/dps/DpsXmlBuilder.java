package br.com.nfse.sdk.xml.dps;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.StringWriter;

public final class DpsXmlBuilder {
    private static final String NAMESPACE = "http://www.sped.fazenda.gov.br/nfse";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    // dhEmi sempre no fuso oficial (America/Sao_Paulo), independente do fuso do servidor.
    private static final ZoneId ZONA_BR = ZoneId.of("America/Sao_Paulo");

    /** Formata um instante no fuso oficial (America/Sao_Paulo), p/ dhEmi e dhEvento. */
    public static String formatarDataHora(OffsetDateTime dataHora) {
        return DATE_TIME_FORMATTER.format(dataHora.atZoneSameInstant(ZONA_BR));
    }

    public String build(Dps dps) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().newDocument();

            Element root = document.createElementNS(NAMESPACE, "DPS");
            root.setAttribute("versao", dps.versao());
            document.appendChild(root);

            Element infDps = element(root, "infDPS");
            infDps.setAttribute("Id", dps.infDps().id());
            buildInfDps(infDps, dps.infDps());

            return toXml(document);
        } catch (Exception exception) {
            throw new DpsXmlException("Nao foi possivel gerar XML da DPS.", exception);
        }
    }

    private void buildInfDps(Element parent, Dps.InfDps data) {
        append(parent, "tpAmb", data.tipoAmbiente());
        append(parent, "dhEmi", formatarDataHora(data.dataHoraEmissao()));
        append(parent, "verAplic", data.versaoAplicativo());
        append(parent, "serie", data.serie());
        append(parent, "nDPS", data.numero());
        append(parent, "dCompet", data.dataCompetencia());
        append(parent, "tpEmit", data.tipoEmitente());
        append(parent, "cLocEmi", data.codigoLocalEmissao());
        buildPrestador(parent, data.prestador());
        buildTomador(parent, data.tomador());
        buildServico(parent, data.servico());
        buildValores(parent, data.valores());
    }

    private void buildPrestador(Element parent, Dps.Prestador data) {
        Element prestador = element(parent, "prest");
        append(prestador, "CNPJ", data.cnpj());
        append(prestador, "CPF", data.cpf());
        append(prestador, "IM", data.inscricaoMunicipal());
        append(prestador, "fone", data.telefone());
        append(prestador, "email", data.email());

        if (data.regimeTributario() != null) {
            Element regime = element(prestador, "regTrib");
            append(regime, "opSimpNac", data.regimeTributario().opcaoSimplesNacional());
            append(regime, "regApTribSN", data.regimeTributario().regimeApuracaoSimplesNacional());
            append(regime, "regEspTrib", data.regimeTributario().regimeEspecialTributacao());
        }
    }

    private void buildTomador(Element parent, Dps.Tomador data) {
        Element tomador = element(parent, "toma");
        append(tomador, "CNPJ", data.cnpj());
        append(tomador, "CPF", data.cpf());
        append(tomador, "xNome", data.nome());
        if (data.endereco() != null) {
            buildEndereco(tomador, data.endereco());
        }
        append(tomador, "fone", data.telefone());
        append(tomador, "email", data.email());
    }

    private void buildEndereco(Element parent, Dps.Endereco data) {
        Element endereco = element(parent, "end");
        if (hasText(data.codigoMunicipio()) || hasText(data.cep())) {
            Element nacional = element(endereco, "endNac");
            append(nacional, "cMun", data.codigoMunicipio());
            append(nacional, "CEP", data.cep());
        }
        append(endereco, "xLgr", data.logradouro());
        append(endereco, "nro", data.numero());
        append(endereco, "xCpl", data.complemento());
        append(endereco, "xBairro", data.bairro());
    }

    private void buildServico(Element parent, Dps.Servico data) {
        Element servico = element(parent, "serv");
        Element local = element(servico, "locPrest");
        append(local, "cLocPrestacao", data.codigoLocalPrestacao());
        Element codigoServico = element(servico, "cServ");
        append(codigoServico, "cTribNac", data.codigoTributacaoNacional());
        append(codigoServico, "cTribMun", data.codigoTributacaoMunicipal());
        append(codigoServico, "xDescServ", data.descricao());
        append(codigoServico, "cNBS", data.codigoNbs());
    }

    private void buildValores(Element parent, Dps.Valores data) {
        Element valores = element(parent, "valores");
        Element servicoPrestado = element(valores, "vServPrest");
        append(servicoPrestado, "vServ", money(data.valorServico()));

        Element trib = element(valores, "trib");
        Element tribMun = element(trib, "tribMun");
        append(tribMun, "tribISSQN", data.tributacao().tributacaoIssqn());
        append(tribMun, "tpRetISSQN", data.tributacao().tipoRetencaoIssqn());

        if (
            data.tributacao().indicadorTotalTributos() != null
                || data.tributacao().percentualTotalTributosSimplesNacional() != null
        ) {
            Element totalTributos = element(trib, "totTrib");
            append(totalTributos, "indTotTrib", data.tributacao().indicadorTotalTributos());
            append(totalTributos, "pTotTribSN", percentage(data.tributacao().percentualTotalTributosSimplesNacional()));
        }
    }

    private static Element element(Element parent, String name) {
        Document document = parent.getOwnerDocument();
        Element element = document.createElement(name);
        parent.appendChild(element);
        return element;
    }

    private static void append(Element parent, String name, Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            return;
        }

        Element element = element(parent, name);
        element.appendChild(parent.getOwnerDocument().createTextNode(value.toString()));
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String percentage(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
