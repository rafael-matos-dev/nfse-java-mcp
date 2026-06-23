package br.com.nfse.mcp;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.api.EmitirNfseRequest;
import br.com.nfse.sdk.api.NfseRunner;
import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsReemissao;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servidor MCP (stdio) que expoe a emissao de NFS-e Nacional para agentes de IA.
 *
 * <p>Padrao homologacao; producao exige {@code confirmarProducao=true} (documento fiscal REAL).
 * O certificado A1 vem das envs NFSE_CERT_PATH / NFSE_CERT_PASSWORD ou dos parametros da ferramenta.
 *
 * <p>IMPORTANTE: stdout e o canal do protocolo JSON-RPC; nada deve ser escrito em System.out aqui.
 */
public final class NfseMcpServer {

    // Mapper JSON do proprio SDK (Jackson 3). Usado tambem para converter/serializar nossos dados,
    // evitando trazer Jackson 2 para este modulo (que conflitaria com o Jackson 3 do MCP).
    private static McpJsonMapper MAPPER;

    public static void main(String[] args) throws Exception {
        MAPPER = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        StdioServerTransportProvider transport = new StdioServerTransportProvider(MAPPER);

        McpServer.sync(transport)
            .serverInfo("nfse-java-mcp", "0.1.0")
            .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
            .tools(ferramentas(MAPPER))
            .build();

        new CountDownLatch(1).await(); // mantem o processo vivo enquanto o transporte stdio roda
    }

    private static SyncToolSpecification[] ferramentas(McpJsonMapper jsonMapper) {
        List<SyncToolSpecification> tools = new ArrayList<>();
        tools.add(tool(jsonMapper, "validar_certificado",
            "Valida o certificado A1 e retorna CNPJ/CPF, emissor e validade.",
            """
            {"type":"object","properties":{
              "caminhoCertificado":{"type":"string","description":"Caminho do .pfx/.p12. Se omitido, usa NFSE_CERT_PATH."},
              "senhaCertificado":{"type":"string","description":"Senha do certificado. Se omitida, usa NFSE_CERT_PASSWORD."}
            }}""",
            (ex, req) -> ok(NfseRunner.cert(certificado(req.arguments())))));

        tools.add(tool(jsonMapper, "emitir_nfse",
            "Emite uma NFS-e a partir dos dados informados. Padrao homologacao; producao exige confirmarProducao=true (documento fiscal REAL).",
            """
            {"type":"object","required":["dados"],"properties":{
              "dados":{"type":"object","description":"Payload da nota: codigoMunicipio, numero, valorServico, prestador{cnpj/cpf,opcaoSimplesNacional,regimeApuracaoSimplesNacional,regimeEspecialTributacao,inscricaoMunicipal}, tomador{cnpj/cpf,nome, e opcionalmente codigoMunicipio,cep,logradouro,numero,bairro}, servico{codigoTributacaoNacional,descricao,codigoLocalPrestacao,codigoTributacaoMunicipal,codigoNbs}, tributacao{tributacaoIssqn,tipoRetencaoIssqn, e indicadorTotalTributos OU percentualTotalTributosSimplesNacional}."},
              "ambiente":{"type":"string","enum":["homologacao","producao"],"default":"homologacao"},
              "confirmarProducao":{"type":"boolean","default":false,"description":"Obrigatorio true para emitir em producao."},
              "caminhoCertificado":{"type":"string"},"senhaCertificado":{"type":"string"}
            }}""",
            (ex, req) -> {
                Map<String, Object> a = req.arguments();
                EmitirNfseRequest dados = MAPPER.convertValue(exigir(a, "dados"), EmitirNfseRequest.class);
                return ok(NfseRunner.emitir(dados, ambiente(a), certificado(a), confirmar(a)));
            }));

        tools.add(tool(jsonMapper, "emitir_de_exemplo",
            "Reaproveita uma nota existente (XML de DPS ou NFSe) trocando so o que mudar (tomador, descricao, valor, numero). Ideal quando a pessoa aponta uma nota antiga como modelo.",
            """
            {"type":"object","required":["caminhoXmlExemplo"],"properties":{
              "caminhoXmlExemplo":{"type":"string","description":"Caminho de um XML de nota anterior usado como modelo."},
              "numero":{"type":"integer","description":"Novo numero da DPS (recomendado para evitar duplicidade)."},
              "serie":{"type":"string"},
              "descricao":{"type":"string","description":"Nova descricao do servico."},
              "valor":{"type":"number","description":"Novo valor do servico."},
              "competencia":{"type":"string","description":"Data de competencia (YYYY-MM-DD)."},
              "tomador":{"type":"object","description":"Novo tomador: {cpf|cnpj, nome, e opcionalmente codigoMunicipio,cep,logradouro,numero,bairro,complemento}."},
              "ambiente":{"type":"string","enum":["homologacao","producao"],"default":"homologacao"},
              "confirmarProducao":{"type":"boolean","default":false},
              "caminhoCertificado":{"type":"string"},"senhaCertificado":{"type":"string"}
            }}""",
            (ex, req) -> {
                Map<String, Object> a = req.arguments();
                String xml = lerArquivo(exigirTexto(a, "caminhoXmlExemplo"));
                DpsReemissao.Overrides overrides = new DpsReemissao.Overrides(
                    longOrNull(a.get("numero"), "numero"),
                    texto(a, "serie"),
                    tomadorOverride(a.get("tomador")),
                    texto(a, "descricao"),
                    decimalOrNull(a.get("valor"), "valor"),
                    dateOrNull(a.get("competencia"), "competencia"),
                    texto(a, "versao"));
                return ok(NfseRunner.emitirDeExemplo(xml, overrides, ambiente(a), certificado(a), confirmar(a)));
            }));

        tools.add(tool(jsonMapper, "consultar_nfse",
            "Consulta uma NFS-e pela chave de acesso.",
            """
            {"type":"object","required":["chaveAcesso"],"properties":{
              "chaveAcesso":{"type":"string"},
              "ambiente":{"type":"string","enum":["homologacao","producao"],"default":"homologacao"},
              "caminhoCertificado":{"type":"string"},"senhaCertificado":{"type":"string"}
            }}""",
            (ex, req) -> {
                Map<String, Object> a = req.arguments();
                return ok(NfseRunner.consultar(exigirTexto(a, "chaveAcesso"), ambiente(a), certificado(a)));
            }));

        tools.add(tool(jsonMapper, "cancelar_nfse",
            "Cancela uma NFS-e (evento 101101). Producao exige confirmarProducao=true.",
            """
            {"type":"object","required":["chaveAcesso","codigoMotivo","descricaoMotivo"],"properties":{
              "chaveAcesso":{"type":"string"},
              "codigoMotivo":{"type":"string"},
              "descricaoMotivo":{"type":"string"},
              "numeroPedido":{"type":"integer","default":1},
              "autorCpfCnpj":{"type":"string","description":"Se omitido, usa o CNPJ/CPF do certificado."},
              "ambiente":{"type":"string","enum":["homologacao","producao"],"default":"homologacao"},
              "confirmarProducao":{"type":"boolean","default":false},
              "caminhoCertificado":{"type":"string"},"senhaCertificado":{"type":"string"}
            }}""",
            (ex, req) -> {
                Map<String, Object> a = req.arguments();
                return ok(NfseRunner.cancelar(
                    exigirTexto(a, "chaveAcesso"), texto(a, "autorCpfCnpj"), intOr(a.get("numeroPedido"), 1, "numeroPedido"),
                    exigirTexto(a, "codigoMotivo"), exigirTexto(a, "descricaoMotivo"),
                    ambiente(a), certificado(a), confirmar(a)));
            }));

        tools.add(tool(jsonMapper, "baixar_danfse",
            "Baixa o DANFSe/PDF de uma NFS-e e salva no caminho informado.",
            """
            {"type":"object","required":["chaveAcesso"],"properties":{
              "chaveAcesso":{"type":"string"},
              "caminhoSaida":{"type":"string","description":"Arquivo PDF de saida. Padrao: danfse-<chave>.pdf no diretorio atual."},
              "ambiente":{"type":"string","enum":["homologacao","producao"],"default":"homologacao"},
              "caminhoCertificado":{"type":"string"},"senhaCertificado":{"type":"string"}
            }}""",
            (ex, req) -> {
                Map<String, Object> a = req.arguments();
                String chave = exigirTexto(a, "chaveAcesso");
                Path saida = Path.of(textoOu(a, "caminhoSaida", "danfse-" + chave + ".pdf"));
                return ok(NfseRunner.baixarPdf(chave, saida, ambiente(a), certificado(a)));
            }));

        return tools.toArray(new SyncToolSpecification[0]);
    }

    // ---- montagem de ferramenta ----

    private static SyncToolSpecification tool(McpJsonMapper jsonMapper, String nome, String descricao,
            String schema, BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange,
                McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
            .name(nome)
            .description(descricao)
            .inputSchema(jsonMapper, schema)
            .build();
        return new SyncToolSpecification(tool, (exchange, request) -> {
            try {
                return handler.apply(exchange, request);
            } catch (Exception exception) {
                return erro(exception);
            }
        });
    }

    private static McpSchema.CallToolResult ok(Object resultado) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(toJson(resultado))
            .isError(false)
            .build();
    }

    private static McpSchema.CallToolResult erro(Exception exception) {
        String mensagem = exception.getMessage() == null ? exception.toString() : exception.getMessage();
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("erro", mensagem);
        return McpSchema.CallToolResult.builder()
            .addTextContent(toJson(corpo))
            .isError(true)
            .build();
    }

    // ---- certificado / ambiente / args ----

    private static CertificadoA1 certificado(Map<String, Object> args) {
        String caminho = textoOu(args, "caminhoCertificado", System.getenv("NFSE_CERT_PATH"));
        String senha = textoOu(args, "senhaCertificado", System.getenv("NFSE_CERT_PASSWORD"));
        if (caminho == null || caminho.isBlank()) {
            throw new IllegalStateException("Informe caminhoCertificado ou configure NFSE_CERT_PATH.");
        }
        if (senha == null) {
            throw new IllegalStateException("Informe senhaCertificado ou configure NFSE_CERT_PASSWORD.");
        }
        return NfseRunner.carregarCertificado(caminho, senha);
    }

    private static Ambiente ambiente(Map<String, Object> args) {
        return NfseRunner.ambiente(texto(args, "ambiente"));
    }

    private static boolean confirmar(Map<String, Object> args) {
        Object value = args.get("confirmarProducao");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static Dps.Tomador tomadorOverride(Object tomador) {
        if (!(tomador instanceof Map<?, ?> map)) {
            return null;
        }
        Dps.Endereco endereco = null;
        if (map.get("codigoMunicipio") != null || map.get("cep") != null || map.get("logradouro") != null) {
            endereco = new Dps.Endereco(
                str(map.get("codigoMunicipio")), str(map.get("cep")), str(map.get("logradouro")),
                str(map.get("numero")), str(map.get("complemento")), str(map.get("bairro")));
        }
        return new Dps.Tomador(
            str(map.get("cnpj")), str(map.get("cpf")), str(map.get("nome")),
            endereco, str(map.get("telefone")), str(map.get("email")));
    }

    private static String lerArquivo(String caminho) {
        try {
            return Files.readString(Path.of(caminho));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Nao foi possivel ler o XML de exemplo: " + caminho, exception);
        }
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            return "{\"erro\":\"falha ao serializar resultado\"}";
        }
    }

    private static Object exigir(Map<String, Object> args, String chave) {
        Object value = args.get(chave);
        if (value == null) {
            throw new IllegalArgumentException("Parametro obrigatorio ausente: " + chave);
        }
        return value;
    }

    private static String exigirTexto(Map<String, Object> args, String chave) {
        String value = texto(args, chave);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parametro obrigatorio ausente: " + chave);
        }
        return value;
    }

    private static String texto(Map<String, Object> args, String chave) {
        return str(args.get(chave));
    }

    private static String textoOu(Map<String, Object> args, String chave, String fallback) {
        String value = texto(args, chave);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longOrNull(Object value, String campo) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(campo + " deve ser um inteiro (recebido: " + value + ").");
        }
    }

    private static int intOr(Object value, int fallback, String campo) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(campo + " deve ser um inteiro (recebido: " + value + ").");
        }
    }

    private static BigDecimal decimalOrNull(Object value, String campo) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(campo + " deve ser um numero (recebido: " + value + ").");
        }
    }

    private static LocalDate dateOrNull(Object value, String campo) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.toString().trim());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException(campo + " deve ser uma data YYYY-MM-DD (recebido: " + value + ").");
        }
    }

    private NfseMcpServer() {
    }
}
