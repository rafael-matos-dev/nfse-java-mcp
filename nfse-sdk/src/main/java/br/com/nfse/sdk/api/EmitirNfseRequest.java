package br.com.nfse.sdk.api;

import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Modelo de payload de emissao, mapeado para {@link Dps}. E um record simples (sem dependencia de
 * JSON): as camadas de I/O (CLI, MCP) usam o mapeador delas para preenche-lo.
 *
 * <p>O endereco do tomador e opcional (ex.: tomador pessoa fisica por CPF). O tpAmb nao e definido
 * aqui: a SDK ajusta conforme o ambiente do contexto na hora de emitir.
 */
public record EmitirNfseRequest(
    String versaoDps,
    String codigoMunicipio,
    String serie,
    Long numero,
    LocalDate dataCompetencia,
    BigDecimal valorServico,
    PrestadorRequest prestador,
    TomadorRequest tomador,
    ServicoRequest servico,
    TributacaoRequest tributacao
) {

    public Dps toDps(String cpfCnpjCertificado) {
        String prestadorCnpj = valueOr(prestador == null ? null : prestador.cnpj(), cpfCnpjCertificado);
        String prestadorCpf = prestador == null ? null : prestador.cpf();
        String municipio = required(codigoMunicipio, "codigoMunicipio");
        String serieDps = valueOr(serie, "1");
        long numeroDps = required(numero, "numero");
        if (numeroDps <= 0) {
            throw new IllegalArgumentException("numero deve ser maior que zero (recebido: " + numeroDps + ").");
        }
        LocalDate competencia = valueOr(dataCompetencia, LocalDate.now());

        return new Dps(
            valueOr(versaoDps, "1.00"),
            new Dps.InfDps(
                DpsIdGenerator.generate(valueOr(prestadorCnpj, prestadorCpf), municipio, serieDps, numeroDps),
                2,
                OffsetDateTime.now(),
                "nfse-java-mcp",
                serieDps,
                numeroDps,
                competencia,
                1,
                municipio,
                prestadorDps(prestadorCnpj, prestadorCpf),
                tomadorDps(),
                servicoDps(municipio),
                new Dps.Valores(required(valorServico, "valorServico"), tributacaoDps())
            )
        );
    }

    private Dps.Prestador prestadorDps(String prestadorCnpj, String prestadorCpf) {
        return new Dps.Prestador(
            prestadorCnpj,
            prestadorCpf,
            prestador == null ? null : prestador.inscricaoMunicipal(),
            prestador == null ? null : prestador.telefone(),
            prestador == null ? null : prestador.email(),
            new Dps.RegimeTributario(
                prestador == null ? 2 : valueOr(prestador.opcaoSimplesNacional(), 2),
                prestador == null ? null : prestador.regimeApuracaoSimplesNacional(),
                prestador == null ? 0 : valueOr(prestador.regimeEspecialTributacao(), 0)
            )
        );
    }

    private Dps.Tomador tomadorDps() {
        if (tomador == null) {
            throw new IllegalArgumentException("tomador e obrigatorio.");
        }
        return new Dps.Tomador(
            tomador.cnpj(),
            tomador.cpf(),
            required(tomador.nome(), "tomador.nome"),
            enderecoDps(),
            tomador.telefone(),
            tomador.email()
        );
    }

    // Endereco do tomador e opcional; so exige os campos obrigatorios quando algum for informado.
    private Dps.Endereco enderecoDps() {
        boolean algumCampoEndereco = hasText(tomador.codigoMunicipio())
            || hasText(tomador.cep())
            || hasText(tomador.logradouro())
            || hasText(tomador.numero())
            || hasText(tomador.bairro())
            || hasText(tomador.complemento());
        if (!algumCampoEndereco) {
            return null;
        }
        return new Dps.Endereco(
            required(tomador.codigoMunicipio(), "tomador.codigoMunicipio"),
            required(tomador.cep(), "tomador.cep"),
            required(tomador.logradouro(), "tomador.logradouro"),
            required(tomador.numero(), "tomador.numero"),
            tomador.complemento(),
            required(tomador.bairro(), "tomador.bairro")
        );
    }

    private Dps.Servico servicoDps(String codigoMunicipio) {
        if (servico == null) {
            throw new IllegalArgumentException("servico e obrigatorio.");
        }
        return new Dps.Servico(
            valueOr(servico.codigoLocalPrestacao(), codigoMunicipio),
            required(servico.codigoTributacaoNacional(), "servico.codigoTributacaoNacional"),
            servico.codigoTributacaoMunicipal(),
            required(servico.descricao(), "servico.descricao"),
            servico.codigoNbs()
        );
    }

    private Dps.Tributacao tributacaoDps() {
        if (tributacao == null) {
            throw new IllegalArgumentException("tributacao e obrigatoria.");
        }
        Integer indicadorTotalTributos = tributacao.indicadorTotalTributos();
        BigDecimal percentual = tributacao.percentualTotalTributosSimplesNacional();
        Integer opcaoSimples = prestador == null ? 2 : valueOr(prestador.opcaoSimplesNacional(), 2);

        if (opcaoSimples == 3 && indicadorTotalTributos != null) {
            throw new IllegalArgumentException(
                "Para ME/EPP, nao informe tributacao.indicadorTotalTributos; informe percentualTotalTributosSimplesNacional.");
        }
        if (indicadorTotalTributos == null && percentual == null) {
            throw new IllegalArgumentException(
                "Informe tributacao.indicadorTotalTributos ou tributacao.percentualTotalTributosSimplesNacional.");
        }

        return new Dps.Tributacao(
            valueOr(tributacao.tributacaoIssqn(), 1),
            valueOr(tributacao.tipoRetencaoIssqn(), 1),
            indicadorTotalTributos,
            percentual
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obrigatorio.");
        }
        return value;
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " e obrigatorio.");
        }
        return value;
    }

    private static <T> T valueOr(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String valueOr(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PrestadorRequest(
        String cnpj,
        String cpf,
        String inscricaoMunicipal,
        String telefone,
        String email,
        Integer opcaoSimplesNacional,
        Integer regimeApuracaoSimplesNacional,
        Integer regimeEspecialTributacao
    ) {
    }

    public record TomadorRequest(
        String cnpj,
        String cpf,
        String nome,
        String codigoMunicipio,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String telefone,
        String email
    ) {
    }

    public record ServicoRequest(
        String codigoLocalPrestacao,
        String codigoTributacaoNacional,
        String codigoTributacaoMunicipal,
        String descricao,
        String codigoNbs
    ) {
    }

    public record TributacaoRequest(
        Integer tributacaoIssqn,
        Integer tipoRetencaoIssqn,
        Integer indicadorTotalTributos,
        BigDecimal percentualTotalTributosSimplesNacional
    ) {
    }
}
