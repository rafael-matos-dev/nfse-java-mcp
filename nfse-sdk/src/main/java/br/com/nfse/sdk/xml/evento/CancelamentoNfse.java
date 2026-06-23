package br.com.nfse.sdk.xml.evento;

import java.time.OffsetDateTime;
import java.util.Objects;

public record CancelamentoNfse(
    String chaveAcesso,
    String cpfCnpjAutor,
    OffsetDateTime dataHoraEvento,
    int numeroPedido,
    String codigoMotivo,
    String descricaoMotivo,
    String versaoAplicativo
) {
    public static final String TIPO_EVENTO = "101101";
    public static final String DESCRICAO_EVENTO = "Cancelamento de NFS-e";

    public CancelamentoNfse {
        Objects.requireNonNull(chaveAcesso, "chaveAcesso is required");
        Objects.requireNonNull(cpfCnpjAutor, "cpfCnpjAutor is required");
        Objects.requireNonNull(dataHoraEvento, "dataHoraEvento is required");
        Objects.requireNonNull(codigoMotivo, "codigoMotivo is required");
        Objects.requireNonNull(descricaoMotivo, "descricaoMotivo is required");
        Objects.requireNonNull(versaoAplicativo, "versaoAplicativo is required");

        if (chaveAcesso.isBlank()) {
            throw new IllegalArgumentException("Chave de acesso da NFS-e e obrigatoria.");
        }
        String documento = onlyDigits(cpfCnpjAutor);
        if (documento.length() != 11 && documento.length() != 14) {
            throw new IllegalArgumentException("CPF/CNPJ do autor deve ter 11 ou 14 digitos.");
        }
        cpfCnpjAutor = documento;
        if (numeroPedido < 1 || numeroPedido > 999) {
            throw new IllegalArgumentException("Numero do pedido de evento deve estar entre 1 e 999.");
        }
        if (codigoMotivo.isBlank()) {
            throw new IllegalArgumentException("Codigo do motivo de cancelamento e obrigatorio.");
        }
        if (descricaoMotivo.isBlank()) {
            throw new IllegalArgumentException("Descricao do motivo de cancelamento e obrigatoria.");
        }
        if (versaoAplicativo.isBlank()) {
            throw new IllegalArgumentException("Versao do aplicativo e obrigatoria.");
        }
    }

    public String idPedidoRegistroEvento() {
        return "PRE" + chaveAcesso + TIPO_EVENTO + "%03d".formatted(numeroPedido);
    }

    public boolean autorPessoaJuridica() {
        return cpfCnpjAutor.length() == 14;
    }

    private static String onlyDigits(String value) {
        return value.replaceAll("\\D", "");
    }
}
