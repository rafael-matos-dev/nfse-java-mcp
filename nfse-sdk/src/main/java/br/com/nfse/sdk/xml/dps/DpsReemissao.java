package br.com.nfse.sdk.xml.dps;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Reaproveita uma DPS lida de um exemplo ({@link DpsXmlReader}) para uma NOVA emissao, aplicando
 * apenas os campos que mudam (tomador, descricao, valor, numero/serie, competencia). Regenera o Id
 * da DPS e usa data/hora de emissao atual. O tpAmb e ajustado depois pela SDK conforme o ambiente.
 */
public final class DpsReemissao {

    private static final String VER_APLIC = "nfse-java-mcp";

    private DpsReemissao() {
    }

    public record Overrides(
        Long numero,
        String serie,
        Dps.Tomador tomador,
        String descricaoServico,
        BigDecimal valorServico,
        LocalDate dataCompetencia,
        String versao
    ) {
    }

    public static Dps reemitir(Dps exemplo, Overrides overrides) {
        Objects.requireNonNull(exemplo, "exemplo is required");
        Objects.requireNonNull(overrides, "overrides is required");

        Dps.InfDps base = exemplo.infDps();
        if (base.prestador() == null) {
            throw new DpsXmlException("Exemplo nao contem prestador; nao da para reemitir.");
        }
        String documento = valueOr(base.prestador().cnpj(), base.prestador().cpf());
        if (documento == null || documento.isBlank()) {
            throw new DpsXmlException("Exemplo nao contem CNPJ/CPF do prestador.");
        }
        if (base.codigoLocalEmissao() == null) {
            throw new DpsXmlException("Exemplo nao contem cLocEmi (municipio de emissao).");
        }

        String serie = valueOr(overrides.serie(), base.serie());
        long numero = overrides.numero() != null ? overrides.numero() : base.numero();
        String id = DpsIdGenerator.generate(documento, base.codigoLocalEmissao(), serie, numero);

        Dps.Tomador tomador = overrides.tomador() != null ? overrides.tomador() : base.tomador();
        Dps.Servico servico = comDescricao(base.servico(), overrides.descricaoServico());
        Dps.Valores valores = comValor(base.valores(), overrides.valorServico());
        LocalDate competencia = overrides.dataCompetencia() != null
            ? overrides.dataCompetencia()
            : valueOr(base.dataCompetencia(), LocalDate.now());
        String versao = valueOr(overrides.versao(), exemplo.versao());

        Dps.InfDps inf = new Dps.InfDps(
            id,
            base.tipoAmbiente(),
            OffsetDateTime.now(),
            VER_APLIC,
            serie,
            numero,
            competencia,
            base.tipoEmitente(),
            base.codigoLocalEmissao(),
            base.prestador(),
            tomador,
            servico,
            valores
        );
        return new Dps(versao, inf);
    }

    private static Dps.Servico comDescricao(Dps.Servico servico, String descricao) {
        if (servico == null || descricao == null || descricao.isBlank()) {
            return servico;
        }
        return new Dps.Servico(
            servico.codigoLocalPrestacao(),
            servico.codigoTributacaoNacional(),
            servico.codigoTributacaoMunicipal(),
            descricao,
            servico.codigoNbs()
        );
    }

    private static Dps.Valores comValor(Dps.Valores valores, BigDecimal valorServico) {
        if (valores == null || valorServico == null) {
            return valores;
        }
        return new Dps.Valores(valorServico, valores.tributacao());
    }

    private static <T> T valueOr(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static String valueOr(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
