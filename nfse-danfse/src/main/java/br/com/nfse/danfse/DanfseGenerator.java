package br.com.nfse.danfse;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

/**
 * Facade do modulo: gera o PDF do DANFSe a partir do XML autorizado da NFS-e.
 *
 * <p>Fluxo: {@link NfseXmlReader} extrai o modelo, {@link QrCodeGenerator} produz o QR,
 * {@link DanfseHtmlRenderer} monta o XHTML e o OpenHTMLtoPDF rasteriza em PDF.
 */
public final class DanfseGenerator {

    private static volatile String logoDataUri;

    private DanfseGenerator() {
    }

    /** Gera o PDF do DANFSe a partir do XML da NFS-e. {@code producao} controla a URL do QR. */
    public static byte[] gerarPdf(String nfseXml, boolean producao) {
        return gerarPdf(nfseXml, producao, DanfseConfig.vazio());
    }

    /** Como {@link #gerarPdf(String, boolean)}, com identificacao opcional do municipio. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseConfig config) {
        Objects.requireNonNull(nfseXml, "nfseXml is required");
        Danfse danfse = NfseXmlReader.read(nfseXml);
        String qr = danfse.chaveAcesso() == null
            ? null
            : QrCodeGenerator.dataUri(QrCodeGenerator.consultaUrl(danfse.chaveAcesso(), producao), 180);
        String html = DanfseHtmlRenderer.render(danfse, qr, logoOficial(), config == null ? DanfseConfig.vazio() : config);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new DanfseException("Nao foi possivel gerar o PDF do DANFSe.", exception);
        }
    }

    /** Gera o PDF e grava no caminho informado, retornando os bytes. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, Path saida) {
        return gerarPdf(nfseXml, producao, DanfseConfig.vazio(), saida);
    }

    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseConfig config, Path saida) {
        Objects.requireNonNull(saida, "saida is required");
        byte[] pdf = gerarPdf(nfseXml, producao, config);
        try {
            Path parent = saida.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(saida, pdf);
        } catch (IOException exception) {
            throw new DanfseException("Nao foi possivel gravar o PDF do DANFSe em " + saida, exception);
        }
        return pdf;
    }

    /**
     * Le um arquivo de imagem (PNG/JPG/GIF/SVG) e devolve uma data URI pronta para o DANFSe
     * (ex.: logo do emitente). Util para CLI/MCP que recebem um caminho de arquivo.
     */
    public static String dataUriImagem(Path arquivo) {
        Objects.requireNonNull(arquivo, "arquivo is required");
        return ImagemDataUri.de(arquivo);
    }

    /** Logo oficial da NFS-e (CC BY-ND), embutido como data URI. Carregado uma vez do classpath. */
    private static String logoOficial() {
        String cached = logoDataUri;
        if (cached != null) {
            return cached;
        }
        try (InputStream in = DanfseGenerator.class.getResourceAsStream("/danfse/nfse-logo.png")) {
            if (in == null) {
                return null;
            }
            String uri = "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
            logoDataUri = uri;
            return uri;
        } catch (IOException exception) {
            return null;
        }
    }
}
