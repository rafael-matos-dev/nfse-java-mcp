package br.com.nfse.sdk.xml.dps;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Dps(String versao, InfDps infDps) {

    public Dps {
        Objects.requireNonNull(versao, "versao is required");
        Objects.requireNonNull(infDps, "infDps is required");
    }

    public record InfDps(
        String id,
        int tipoAmbiente,
        OffsetDateTime dataHoraEmissao,
        String versaoAplicativo,
        String serie,
        long numero,
        LocalDate dataCompetencia,
        int tipoEmitente,
        String codigoLocalEmissao,
        Prestador prestador,
        Tomador tomador,
        Servico servico,
        Valores valores
    ) {
        /** Copia trocando apenas o tpAmb. Usado pela SDK para casar o XML com o ambiente do endpoint. */
        public InfDps withTipoAmbiente(int novoTipoAmbiente) {
            return new InfDps(
                id, novoTipoAmbiente, dataHoraEmissao, versaoAplicativo, serie, numero,
                dataCompetencia, tipoEmitente, codigoLocalEmissao, prestador, tomador, servico, valores
            );
        }
    }

    public record Prestador(
        String cnpj,
        String cpf,
        String inscricaoMunicipal,
        String telefone,
        String email,
        RegimeTributario regimeTributario
    ) {
        public Prestador(String cnpj, String cpf, String telefone, String email, RegimeTributario regimeTributario) {
            this(cnpj, cpf, null, telefone, email, regimeTributario);
        }
    }

    public record RegimeTributario(
        Integer opcaoSimplesNacional,
        Integer regimeApuracaoSimplesNacional,
        Integer regimeEspecialTributacao
    ) {
        public RegimeTributario(Integer opcaoSimplesNacional, Integer regimeEspecialTributacao) {
            this(opcaoSimplesNacional, null, regimeEspecialTributacao);
        }
    }

    public record Tomador(String cnpj, String cpf, String nome, Endereco endereco, String telefone, String email) {
    }

    public record Endereco(
        String codigoMunicipio,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro
    ) {
    }

    public record Servico(
        String codigoLocalPrestacao,
        String codigoTributacaoNacional,
        String codigoTributacaoMunicipal,
        String descricao,
        String codigoNbs
    ) {
        public Servico(String codigoLocalPrestacao, String codigoTributacaoNacional, String descricao) {
            this(codigoLocalPrestacao, codigoTributacaoNacional, null, descricao, null);
        }
    }

    public record Valores(BigDecimal valorServico, Tributacao tributacao) {
    }

    public record Tributacao(
        Integer tributacaoIssqn,
        Integer tipoRetencaoIssqn,
        Integer indicadorTotalTributos,
        BigDecimal percentualTotalTributosSimplesNacional
    ) {
        public Tributacao(Integer tributacaoIssqn, Integer tipoRetencaoIssqn, Integer indicadorTotalTributos) {
            this(tributacaoIssqn, tipoRetencaoIssqn, indicadorTotalTributos, null);
        }
    }
}
