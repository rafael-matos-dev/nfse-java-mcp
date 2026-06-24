package br.com.nfse.danfse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Modelo do DANFSe, extraido do XML autorizado da NFS-e ({@code <NFSe><infNFSe>}). Campos tipados;
 * a formatacao (mascaras, R$, datas) e o preenchimento de "-" ausentes ficam no
 * {@link DanfseHtmlRenderer}. Campos opcionais podem ser {@code null}.
 */
public record Danfse(
    String chaveAcesso,
    boolean homologacao,
    Identificacao identificacao,
    Pessoa prestador,
    Pessoa tomador,
    Servico servico,
    Valores valores,
    String informacoesComplementares,
    IbsCbs ibsCbs
) {

    /** Identificacao da NFS-e e da DPS de origem. */
    public record Identificacao(
        String numeroNfse,
        LocalDate competencia,
        OffsetDateTime emissaoNfse,
        String numeroDps,
        String serieDps,
        OffsetDateTime emissaoDps,
        String municipioEmissao,
        String municipioPrestacao
    ) {
    }

    /** Prestador ou tomador. Documento sem mascara (so digitos); a mascara e aplicada na renderizacao. */
    public record Pessoa(
        String cnpj,
        String cpf,
        String inscricaoMunicipal,
        String nome,
        String telefone,
        String email,
        Endereco endereco,
        String regimeSimplesNacional,
        String regimeApuracaoSimplesNacional
    ) {
    }

    public record Endereco(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String uf,
        String cep
    ) {
    }

    public record Servico(
        String codigoTributacaoNacional,
        String descricaoTributacaoNacional,
        String codigoTributacaoMunicipal,
        String localPrestacao,
        String descricao,
        String tributacaoIssqn,
        String tipoRetencaoIssqn,
        String municipioIncidencia
    ) {
    }

    public record Valores(
        BigDecimal valorServico,
        BigDecimal descontoIncondicionado,
        BigDecimal descontoCondicionado,
        BigDecimal valorLiquido,
        BigDecimal totalTributosFederais,
        BigDecimal totalTributosEstaduais,
        BigDecimal totalTributosMunicipais
    ) {
    }

    /** Bloco IBS/CBS da NT 009 (reforma tributaria). Estrutural; renderizado so quando presente. */
    public record IbsCbs(
        BigDecimal valorTotalIbs,
        BigDecimal valorTotalCbs,
        String observacao
    ) {
    }
}
