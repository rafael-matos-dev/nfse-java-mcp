package br.com.nfse.danfse;

/**
 * Configuracao opcional de identificacao do municipio no DANFSe (brasão + contato da prefeitura).
 *
 * <p>Esses dados NAO vem no XML da NFS-e e nao ha API publica que os forneca — o emissor os informa
 * se quiser reproduzir o bloco do municipio como no documento oficial. Sem config, o DANFSe mostra
 * apenas o nome do municipio (que vem do XML) e o logo oficial da NFS-e.
 *
 * <p>{@code brasaoDataUri} deve ser uma data URI de imagem (ex.: {@code data:image/png;base64,...}).
 * Todos os campos sao opcionais ({@code null} = omitido).
 */
public record DanfseConfig(
    String municipioNome,
    String brasaoDataUri,
    String departamento,
    String telefone,
    String email
) {

    /** Config vazia (sem branding municipal). */
    public static DanfseConfig vazio() {
        return new DanfseConfig(null, null, null, null, null);
    }

    public boolean temBrasao() {
        return brasaoDataUri != null && !brasaoDataUri.isBlank();
    }

    public boolean temContato() {
        return naoVazio(departamento) || naoVazio(telefone) || naoVazio(email);
    }

    private static boolean naoVazio(String v) {
        return v != null && !v.isBlank();
    }
}
