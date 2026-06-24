package br.com.nfse.danfse;

/**
 * Configuração opcional do DANFSe: identificação do município (brasão + contato da prefeitura) e o
 * logo do emitente (prestador) no cabeçalho.
 *
 * <p>Esses dados NÃO vêm no XML da NFS-e — o emissor os informa se quiser. Sem config, o DANFSe mostra
 * apenas o nome do município (que vem do XML) e o logo oficial da NFS-e.
 *
 * <p>Imagens ({@code brasaoDataUri}, {@code logoEmitenteDataUri}) devem ser data URIs
 * (ex.: {@code data:image/png;base64,...}). O logo do emitente é limitado por CSS para nunca quebrar
 * o layout; <b>tamanho sugerido: ~300×120 px (proporção ~2,5:1), PNG com fundo transparente</b>.
 * Todos os campos são opcionais ({@code null} = omitido).
 */
public record DanfseConfig(
    String municipioNome,
    String brasaoDataUri,
    String departamento,
    String telefone,
    String email,
    String logoEmitenteDataUri
) {

    /** Config vazia (sem branding municipal nem logo do emitente). */
    public static DanfseConfig vazio() {
        return new DanfseConfig(null, null, null, null, null, null);
    }

    /** Atalho para o caso comum: apenas o logo do emitente no cabeçalho. */
    public static DanfseConfig comLogoEmitente(String logoEmitenteDataUri) {
        return new DanfseConfig(null, null, null, null, null, logoEmitenteDataUri);
    }

    public boolean temBrasao() {
        return naoVazio(brasaoDataUri);
    }

    public boolean temLogoEmitente() {
        return naoVazio(logoEmitenteDataUri);
    }

    public boolean temContato() {
        return naoVazio(departamento) || naoVazio(telefone) || naoVazio(email);
    }

    private static boolean naoVazio(String v) {
        return v != null && !v.isBlank();
    }
}
