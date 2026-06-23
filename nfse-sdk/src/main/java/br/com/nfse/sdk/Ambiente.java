package br.com.nfse.sdk;

public enum Ambiente {
    PRODUCAO,
    HOMOLOGACAO;

    /**
     * Codigo de ambiente (tpAmb) usado na DPS e validado pela SEFIN: 1 = producao, 2 = homologacao.
     * Manter esse valor casado com o endpoint evita a rejeicao E0006.
     */
    public int tipoAmbiente() {
        return switch (this) {
            case PRODUCAO -> 1;
            case HOMOLOGACAO -> 2;
        };
    }
}
