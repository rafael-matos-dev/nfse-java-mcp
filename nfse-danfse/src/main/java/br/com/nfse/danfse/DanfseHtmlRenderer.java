package br.com.nfse.danfse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renderiza o {@link Danfse} em XHTML (bem-formado, para o OpenHTMLtoPDF), seguindo o layout
 * nacional do DANFSe: cabecalho, chave + QR, identificacao, emitente, tomador, intermediario,
 * servico, tributacao municipal/federal, valor total, totais aproximados e — quando presente —
 * a secao IBS/CBS (NT 009). Campos ausentes sao renderizados como "-", como no documento oficial.
 */
public final class DanfseHtmlRenderer {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DanfseHtmlRenderer() {
    }

    public static String render(Danfse d, String qrDataUri) {
        StringBuilder h = new StringBuilder(8192);
        h.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        h.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        h.append("<style>").append(css()).append("</style></head><body>");

        cabecalho(h, d);
        chaveEQr(h, d, qrDataUri);
        identificacao(h, d.identificacao());
        pessoa(h, "PRESTADOR DO SERVICO", "EMITENTE DA NFS-e", d.prestador(), true);
        pessoa(h, "TOMADOR DO SERVICO", "TOMADOR DO SERVICO", d.tomador(), false);
        band(h, "INTERMEDIARIO DO SERVICO NAO IDENTIFICADO NA NFS-e", true);
        servico(h, d.servico());
        tributacaoMunicipal(h, d);
        tributacaoFederal(h);
        if (d.ibsCbs() != null) {
            ibsCbs(h, d.ibsCbs());
        }
        valorTotal(h, d.valores());
        totaisAproximados(h, d.valores());
        informacoesComplementares(h, d.informacoesComplementares());

        h.append("</body></html>");
        return h.toString();
    }

    private static void cabecalho(StringBuilder h, Danfse d) {
        h.append("<table class=\"hdr\"><tr>")
            .append("<td class=\"hdr-l\"><div class=\"danfse-tit\">DANFSe</div>")
            .append("<div class=\"danfse-sub\">Documento Auxiliar da NFS-e</div></td>")
            .append("<td class=\"hdr-c\"><div class=\"mun\">")
            .append(esc(municipio(d))).append("</div></td>")
            .append("<td class=\"hdr-r\"><div class=\"sml\">A autenticidade desta NFS-e pode ser verificada ")
            .append("pela leitura do codigo QR ou pela consulta da chave de acesso no portal nacional da NFS-e.</div></td>")
            .append("</tr></table>");
    }

    private static void chaveEQr(StringBuilder h, Danfse d, String qrDataUri) {
        h.append("<table class=\"grid\"><tr>")
            .append("<td class=\"k\"><div class=\"lbl\">Chave de Acesso da NFS-e</div>")
            .append("<div class=\"chave\">").append(esc(dash(d.chaveAcesso()))).append("</div></td>")
            .append("<td class=\"qr\">");
        if (qrDataUri != null) {
            h.append("<img src=\"").append(qrDataUri).append("\" width=\"90\" height=\"90\" alt=\"QR\"/>");
        }
        h.append("</td></tr></table>");
    }

    private static void identificacao(StringBuilder h, Danfse.Identificacao id) {
        h.append("<table class=\"grid\">");
        row(h,
            campo("Numero da NFS-e", dash(id.numeroNfse())),
            campo("Competencia da NFS-e", data(id.competencia())),
            campo("Data e Hora da emissao da NFS-e", dataHora(id.emissaoNfse())));
        row(h,
            campo("Numero da DPS", dash(id.numeroDps())),
            campo("Serie da DPS", dash(id.serieDps())),
            campo("Data e Hora da emissao da DPS", dataHora(id.emissaoDps())));
        h.append("</table>");
    }

    private static void pessoa(StringBuilder h, String tituloRole, String band, Danfse.Pessoa p, boolean prestador) {
        band(h, band, false);
        h.append("<table class=\"grid\">");
        row(h,
            campo(prestador ? "Prestador do Servico" : "", ""),
            campo("CNPJ / CPF / NIF", documento(p)),
            campo("Inscricao Municipal", p == null ? "-" : dash(p.inscricaoMunicipal())),
            campo("Telefone", p == null ? "-" : telefone(p.telefone())));
        row(h,
            campoSpan("Nome / Nome Empresarial", p == null ? "-" : dash(p.nome()), 3),
            campo("E-mail", p == null ? "-" : dash(p.email())));
        row(h,
            campoSpan("Endereco", endereco(p), 2),
            campo("Municipio", p == null ? "-" : municipioPessoa(p)),
            campo("CEP", p == null || p.endereco() == null ? "-" : cep(p.endereco().cep())));
        if (prestador) {
            row(h,
                campoSpan("Simples Nacional na Data de Competencia", p == null ? "-" : dash(p.regimeSimplesNacional()), 2),
                campoSpan("Regime de Apuracao Tributaria pelo SN", p == null ? "-" : dash(p.regimeApuracaoSimplesNacional()), 2));
        }
        h.append("</table>");
    }

    private static void servico(StringBuilder h, Danfse.Servico s) {
        band(h, "SERVICO PRESTADO", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Codigo de Tributacao Nacional", tribNac(s)),
            campo("Codigo de Tributacao Municipal", s == null ? "-" : dash(s.codigoTributacaoMunicipal())),
            campo("Local da Prestacao", s == null ? "-" : dash(s.localPrestacao())),
            campo("Pais da Prestacao", "Brasil"));
        row(h, campoSpan("Descricao do Servico", s == null ? "-" : dash(s.descricao()), 4));
        h.append("</table>");
    }

    private static void tributacaoMunicipal(StringBuilder h, Danfse d) {
        Danfse.Servico s = d.servico();
        band(h, "TRIBUTACAO MUNICIPAL", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Tributacao do ISSQN", s == null ? "-" : dash(s.tributacaoIssqn())),
            campo("Pais Resultado da Prestacao", "-"),
            campo("Municipio de Incidencia do ISSQN", s == null ? "-" : dash(s.municipioIncidencia())),
            campo("Regime Especial de Tributacao", "Nenhum"));
        row(h,
            campo("Valor do Servico", money(d.valores() == null ? null : d.valores().valorServico())),
            campo("Desconto Incondicionado", money(d.valores() == null ? null : d.valores().descontoIncondicionado())),
            campo("BC ISSQN", "-"),
            campo("Retencao do ISSQN", s == null ? "-" : dash(s.tipoRetencaoIssqn())));
        h.append("</table>");
    }

    private static void tributacaoFederal(StringBuilder h) {
        band(h, "TRIBUTACAO FEDERAL", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("IRRF", "-"),
            campo("Contribuicao Previdenciaria - Retida", "-"),
            campo("PIS - Debito Apuracao Propria", "-"),
            campo("COFINS - Debito Apuracao Propria", "-"));
        h.append("</table>");
    }

    private static void ibsCbs(StringBuilder h, Danfse.IbsCbs ibs) {
        band(h, "IBS / CBS (REFORMA TRIBUTARIA)", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Valor do IBS", money(ibs.valorTotalIbs())),
            campo("Valor da CBS", money(ibs.valorTotalCbs())),
            campoSpan("Observacao", dash(ibs.observacao()), 2));
        h.append("</table>");
    }

    private static void valorTotal(StringBuilder h, Danfse.Valores v) {
        band(h, "VALOR TOTAL DA NFS-e", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Valor do Servico", money(v == null ? null : v.valorServico())),
            campo("Desconto Condicionado", money(v == null ? null : v.descontoCondicionado())),
            campo("Desconto Incondicionado", money(v == null ? null : v.descontoIncondicionado())),
            campo("Valor Liquido da NFS-e", money(v == null ? null : v.valorLiquido())));
        h.append("</table>");
    }

    private static void totaisAproximados(StringBuilder h, Danfse.Valores v) {
        band(h, "TOTAIS APROXIMADOS DOS TRIBUTOS", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Federais", money(v == null ? null : v.totalTributosFederais())),
            campo("Estaduais", money(v == null ? null : v.totalTributosEstaduais())),
            campo("Municipais", money(v == null ? null : v.totalTributosMunicipais())));
        h.append("</table>");
    }

    private static void informacoesComplementares(StringBuilder h, String texto) {
        band(h, "INFORMACOES COMPLEMENTARES", false);
        h.append("<table class=\"grid\"><tr><td class=\"free\">")
            .append(esc(dash(texto))).append("</td></tr></table>");
    }

    // ---- celulas / bandas ----

    private static void band(StringBuilder h, String titulo, boolean centro) {
        h.append("<div class=\"band").append(centro ? " center" : "").append("\">")
            .append(esc(titulo)).append("</div>");
    }

    private static void row(StringBuilder h, String... cells) {
        h.append("<tr>");
        for (String c : cells) {
            h.append(c);
        }
        h.append("</tr>");
    }

    private static String campo(String label, String valueHtml) {
        return "<td><div class=\"lbl\">" + esc(label) + "</div><div class=\"val\">" + valueHtml + "</div></td>";
    }

    private static String campoSpan(String label, String valueHtml, int span) {
        return "<td colspan=\"" + span + "\"><div class=\"lbl\">" + esc(label)
            + "</div><div class=\"val\">" + valueHtml + "</div></td>";
    }

    // ---- formatadores ----

    private static String municipio(Danfse d) {
        Danfse.Identificacao id = d.identificacao();
        return id == null ? "-" : dash(id.municipioPrestacao());
    }

    private static String documento(Danfse.Pessoa p) {
        if (p == null) {
            return "-";
        }
        if (p.cnpj() != null) {
            return mascaraCnpj(p.cnpj());
        }
        if (p.cpf() != null) {
            return mascaraCpf(p.cpf());
        }
        return "-";
    }

    private static String endereco(Danfse.Pessoa p) {
        if (p == null || p.endereco() == null) {
            return "-";
        }
        Danfse.Endereco e = p.endereco();
        StringBuilder sb = new StringBuilder();
        juntar(sb, e.logradouro());
        juntar(sb, e.numero());
        juntar(sb, e.complemento());
        juntar(sb, e.bairro());
        return sb.length() == 0 ? "-" : esc(sb.toString());
    }

    private static String municipioPessoa(Danfse.Pessoa p) {
        if (p.endereco() == null) {
            return "-";
        }
        String mun = p.endereco().municipio();
        String uf = p.endereco().uf();
        if (mun == null && uf == null) {
            return "-";
        }
        return esc((mun == null ? "" : mun) + (uf == null ? "" : " - " + uf));
    }

    private static void juntar(StringBuilder sb, String parte) {
        if (parte != null && !parte.isBlank()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(parte);
        }
    }

    private static String tribNac(Danfse.Servico s) {
        if (s == null || s.codigoTributacaoNacional() == null) {
            return "-";
        }
        String desc = s.descricaoTributacaoNacional();
        return esc(s.codigoTributacaoNacional() + (desc == null ? "" : " - " + desc));
    }

    private static String data(LocalDate d) {
        return d == null ? "-" : d.format(DATA);
    }

    private static String dataHora(OffsetDateTime d) {
        return d == null ? "-" : d.format(DATA_HORA);
    }

    private static String money(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        DecimalFormatSymbols s = new DecimalFormatSymbols(BR);
        DecimalFormat f = new DecimalFormat("#,##0.00", s);
        return "R$ " + f.format(v.setScale(2, RoundingMode.HALF_UP));
    }

    private static String telefone(String fone) {
        if (fone == null) {
            return "-";
        }
        String d = fone.replaceAll("\\D", "");
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return esc(fone);
    }

    private static String cep(String cep) {
        if (cep == null) {
            return "-";
        }
        String d = cep.replaceAll("\\D", "");
        return d.length() == 8 ? d.substring(0, 5) + "-" + d.substring(5) : esc(cep);
    }

    private static String mascaraCnpj(String v) {
        String d = v.replaceAll("\\D", "");
        if (d.length() != 14) {
            return esc(v);
        }
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
            + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    private static String mascaraCpf(String v) {
        String d = v.replaceAll("\\D", "");
        if (d.length() != 11) {
            return esc(v);
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    private static String dash(String v) {
        return v == null || v.isBlank() ? "-" : esc(v);
    }

    private static String esc(String v) {
        if (v == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(v.length() + 16);
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String css() {
        return """
            @page { size: A4 portrait; margin: 10mm; }
            * { box-sizing: border-box; }
            body { font-family: 'Helvetica', sans-serif; font-size: 7pt; color: #000; }
            table { width: 100%; border-collapse: collapse; table-layout: fixed; }
            .hdr td { border: 0.5pt solid #000; padding: 3pt; vertical-align: middle; }
            .hdr-l { width: 33%; } .hdr-c { width: 34%; text-align: center; } .hdr-r { width: 33%; }
            .danfse-tit { font-size: 14pt; font-weight: bold; }
            .danfse-sub { font-size: 8pt; }
            .mun { font-size: 9pt; font-weight: bold; }
            .sml { font-size: 6pt; }
            .grid { margin-top: -0.5pt; }
            .grid td { border: 0.5pt solid #000; padding: 2pt 3pt; vertical-align: top; }
            .lbl { font-size: 5.5pt; color: #333; }
            .val { font-size: 7.5pt; font-weight: bold; min-height: 9pt; }
            .chave { font-size: 9pt; font-weight: bold; letter-spacing: 0.3pt; }
            .qr { width: 96pt; text-align: center; vertical-align: middle; }
            .k { vertical-align: middle; }
            .band { background: #e9e9e9; border: 0.5pt solid #000; margin-top: -0.5pt;
                    font-size: 7pt; font-weight: bold; padding: 2pt 3pt; }
            .band.center { text-align: center; }
            .free { min-height: 40pt; vertical-align: top; padding: 4pt; }
            """;
    }
}
