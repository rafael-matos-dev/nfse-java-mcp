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
        return render(d, qrDataUri, null, DanfseConfig.vazio());
    }

    public static String render(Danfse d, String qrDataUri, String logoDataUri, DanfseConfig config) {
        DanfseConfig cfg = config == null ? DanfseConfig.vazio() : config;
        StringBuilder h = new StringBuilder(8192);
        h.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        h.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        h.append("<style>").append(css()).append("</style></head><body>");

        cabecalho(h, d, qrDataUri, logoDataUri, cfg);
        identificacao(h, d.identificacao());
        pessoa(h, "PRESTADOR DO SERVIÇO", "EMITENTE DA NFS-e", d.prestador(), true);
        pessoa(h, "TOMADOR DO SERVIÇO", "TOMADOR DO SERVIÇO", d.tomador(), false);
        band(h, "INTERMEDIÁRIO DO SERVIÇO NÃO IDENTIFICADO NA NFS-e", true);
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

    private static void cabecalho(StringBuilder h, Danfse d, String qrDataUri, String logoDataUri, DanfseConfig cfg) {
        // Topo em 3 colunas: logo NFS-e | titulo + municipio + aviso de ambiente | QR
        h.append("<table class=\"hdr\"><tr>");

        h.append("<td class=\"hdr-l\">");
        if (logoDataUri != null) {
            h.append("<img class=\"logo\" src=\"").append(logoDataUri).append("\" alt=\"NFS-e\"/>");
        } else {
            h.append("<div class=\"danfse-tit\">NFS-e</div>");
        }
        h.append("</td>");

        h.append("<td class=\"hdr-c\">")
            .append("<div class=\"danfse-tit\">DANFSe v1.0</div>")
            .append("<div class=\"danfse-sub\">Documento Auxiliar da NFS-e</div>");
        municipioHeader(h, d, cfg);
        if (d.homologacao()) {
            h.append("<div class=\"aviso\">NFS-e SEM VALIDADE JURÍDICA</div>")
                .append("<div class=\"sml\">Ambiente de Produção Restrita - Documento sem valor legal</div>");
        }
        h.append("</td>");

        h.append("<td class=\"hdr-r\">");
        if (qrDataUri != null) {
            h.append("<img src=\"").append(qrDataUri).append("\" width=\"86\" height=\"86\" alt=\"QR\"/>");
        }
        h.append("<div class=\"sml\">Consulte pela chave de acesso ou pelo QR no portal nacional da NFS-e.</div>")
            .append("</td>");

        h.append("</tr></table>");

        // Barra da chave de acesso
        h.append("<table class=\"grid\"><tr>")
            .append("<td class=\"k\"><div class=\"lbl\">Chave de Acesso da NFS-e</div>")
            .append("<div class=\"chave\">").append(esc(dash(d.chaveAcesso()))).append("</div></td>")
            .append("</tr></table>");
    }

    private static void municipioHeader(StringBuilder h, Danfse d, DanfseConfig cfg) {
        String nome = cfg.municipioNome() != null && !cfg.municipioNome().isBlank()
            ? cfg.municipioNome()
            : municipio(d);
        h.append("<div class=\"mun\">");
        if (cfg.temBrasao()) {
            h.append("<img class=\"brasao\" src=\"").append(cfg.brasaoDataUri()).append("\" alt=\"\"/>");
        }
        h.append(esc(dash(nome))).append("</div>");
        if (cfg.temContato()) {
            StringBuilder c = new StringBuilder();
            juntar(c, cfg.departamento());
            juntar(c, cfg.telefone());
            juntar(c, cfg.email());
            h.append("<div class=\"sml\">").append(esc(c.toString())).append("</div>");
        }
    }

    private static void identificacao(StringBuilder h, Danfse.Identificacao id) {
        h.append("<table class=\"grid\">");
        row(h,
            campo("Número da NFS-e", dash(id.numeroNfse())),
            campo("Competência da NFS-e", data(id.competencia())),
            campo("Data e Hora da emissão da NFS-e", dataHora(id.emissaoNfse())));
        row(h,
            campo("Número da DPS", dash(id.numeroDps())),
            campo("Série da DPS", dash(id.serieDps())),
            campo("Data e Hora da emissão da DPS", dataHora(id.emissaoDps())));
        h.append("</table>");
    }

    private static void pessoa(StringBuilder h, String tituloRole, String band, Danfse.Pessoa p, boolean prestador) {
        band(h, band, false);
        h.append("<table class=\"grid\">");
        row(h,
            campo(prestador ? "Prestador do Serviço" : "", ""),
            campo("CNPJ / CPF / NIF", documento(p)),
            campo("Inscrição Municipal", p == null ? "-" : dash(p.inscricaoMunicipal())),
            campo("Telefone", p == null ? "-" : telefone(p.telefone())));
        row(h,
            campoSpan("Nome / Nome Empresarial", p == null ? "-" : dash(p.nome()), 3),
            campo("E-mail", p == null ? "-" : dash(p.email())));
        row(h,
            campoSpan("Endereço", endereco(p), 2),
            campo("Município", p == null ? "-" : municipioPessoa(p)),
            campo("CEP", p == null || p.endereco() == null ? "-" : cep(p.endereco().cep())));
        if (prestador) {
            row(h,
                campoSpan("Simples Nacional na Data de Competência", p == null ? "-" : dash(p.regimeSimplesNacional()), 2),
                campoSpan("Regime de Apuração Tributária pelo SN", p == null ? "-" : dash(p.regimeApuracaoSimplesNacional()), 2));
        }
        h.append("</table>");
    }

    private static void servico(StringBuilder h, Danfse.Servico s) {
        band(h, "SERVIÇO PRESTADO", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Código de Tributação Nacional", tribNac(s)),
            campo("Código de Tributação Municipal", tribMun(s)),
            campo("Local da Prestação", s == null ? "-" : dash(s.localPrestacao())),
            campo("País da Prestação", "Brasil"));
        row(h, campoSpan("Código NBS", nbs(s), 4));
        row(h, campoSpan("Descrição do Serviço", s == null ? "-" : dash(s.descricao()), 4));
        h.append("</table>");
    }

    private static String nbs(Danfse.Servico s) {
        if (s == null || s.codigoNbs() == null) {
            return "-";
        }
        String desc = s.descricaoNbs();
        return esc(s.codigoNbs() + (desc == null ? "" : " - " + desc));
    }

    private static void tributacaoMunicipal(StringBuilder h, Danfse d) {
        Danfse.Servico s = d.servico();
        band(h, "TRIBUTAÇÃO MUNICIPAL", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Tributação do ISSQN", s == null ? "-" : dash(s.tributacaoIssqn())),
            campo("País Resultado da Prestação", "-"),
            campo("Município de Incidência do ISSQN", s == null ? "-" : dash(s.municipioIncidencia())),
            campo("Regime Especial de Tributação", "Nenhum"));
        row(h,
            campo("Valor do Serviço", money(d.valores() == null ? null : d.valores().valorServico())),
            campo("Desconto Incondicionado", money(d.valores() == null ? null : d.valores().descontoIncondicionado())),
            campo("BC ISSQN", "-"),
            campo("Retenção do ISSQN", s == null ? "-" : dash(s.tipoRetencaoIssqn())));
        h.append("</table>");
    }

    private static void tributacaoFederal(StringBuilder h) {
        band(h, "TRIBUTAÇÃO FEDERAL", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("IRRF", "-"),
            campo("Contribuição Previdenciária - Retida", "-"),
            campo("PIS - Débito Apuração Própria", "-"),
            campo("COFINS - Débito Apuração Própria", "-"));
        h.append("</table>");
    }

    private static void ibsCbs(StringBuilder h, Danfse.IbsCbs ibs) {
        band(h, "IBS / CBS (REFORMA TRIBUTÁRIA)", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Valor do IBS", money(ibs.valorTotalIbs())),
            campo("Valor da CBS", money(ibs.valorTotalCbs())),
            campoSpan("Observação", dash(ibs.observacao()), 2));
        h.append("</table>");
    }

    private static void valorTotal(StringBuilder h, Danfse.Valores v) {
        band(h, "VALOR TOTAL DA NFS-e", false);
        h.append("<table class=\"grid\">");
        row(h,
            campo("Valor do Serviço", money(v == null ? null : v.valorServico())),
            campo("Desconto Condicionado", money(v == null ? null : v.descontoCondicionado())),
            campo("Desconto Incondicionado", money(v == null ? null : v.descontoIncondicionado())),
            campo("Valor Líquido da NFS-e", money(v == null ? null : v.valorLiquido())));
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
        band(h, "INFORMAÇÕES COMPLEMENTARES", false);
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
        // Se o municipio ainda e um codigo IBGE (nao veio nomeado no XML), tenta resolver no IBGE.
        // A resolucao ja inclui a UF ("Cidade - UF"); fallback gracioso para codigo + UF do endereco.
        if (mun != null && mun.matches("\\d{7}")) {
            var resolvido = MunicipioResolver.resolver(mun);
            if (resolvido.isPresent()) {
                return esc(resolvido.get());
            }
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

    private static String tribMun(Danfse.Servico s) {
        if (s == null || s.codigoTributacaoMunicipal() == null) {
            return "-";
        }
        String desc = s.descricaoTributacaoMunicipal();
        return esc(s.codigoTributacaoMunicipal() + (desc == null ? "" : " - " + desc));
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
            @page { size: A4 portrait; margin: 8mm; }
            * { box-sizing: border-box; }
            body { font-family: Arial, 'Helvetica', sans-serif; font-size: 7pt; color: #000; }
            table { width: 100%; border-collapse: collapse; table-layout: fixed; }
            .hdr td { border: 0.6pt solid #000; padding: 3pt 4pt; vertical-align: middle; }
            .hdr-l { width: 30%; text-align: center; }
            .hdr-c { width: 46%; text-align: center; }
            .hdr-r { width: 24%; text-align: center; }
            .logo { max-width: 96%; max-height: 46pt; }
            .brasao { max-height: 18pt; vertical-align: middle; margin-right: 3pt; }
            .danfse-tit { font-size: 13pt; font-weight: bold; }
            .danfse-sub { font-size: 7.5pt; }
            .mun { font-size: 8.5pt; font-weight: bold; margin-top: 2pt; }
            .aviso { color: #cc0000; font-size: 8.5pt; font-weight: bold; margin-top: 2pt; }
            .sml { font-size: 5.5pt; color: #333; margin-top: 1pt; }
            .grid { margin-top: -0.6pt; }
            .grid td { border: 0.6pt solid #000; padding: 1.5pt 4pt; vertical-align: top; }
            .lbl { font-size: 5.2pt; color: #444; line-height: 1.05; }
            .val { font-size: 7.5pt; font-weight: bold; line-height: 1.1; min-height: 8.5pt; }
            .chave { font-size: 9.5pt; font-weight: bold; letter-spacing: 0.4pt; }
            .k { vertical-align: middle; text-align: center; }
            .k .lbl { text-align: left; }
            .band { background: #c9c9c9; border: 0.6pt solid #000; margin-top: -0.6pt;
                    font-size: 7pt; font-weight: bold; padding: 1.5pt 4pt; }
            .band.center { text-align: center; }
            .free { min-height: 34pt; vertical-align: top; padding: 4pt; }
            """;
    }
}
