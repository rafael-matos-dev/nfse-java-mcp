package br.com.nfse.danfse;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Facade do modulo: gera o PDF do DANFSe a partir do XML autorizado da NFS-e.
 *
 * <p>Fluxo: {@link NfseXmlReader} extrai o modelo, {@link QrCodeGenerator} produz o QR,
 * {@link DanfseHtmlRenderer} monta o XHTML e o OpenHTMLtoPDF rasteriza em PDF.
 */
public final class DanfseGenerator {

    private DanfseGenerator() {
    }

    /** Gera o PDF do DANFSe a partir do XML da NFS-e. {@code producao} controla a URL do QR. */
    public static byte[] gerarPdf(String nfseXml, boolean producao) {
        Objects.requireNonNull(nfseXml, "nfseXml is required");
        Danfse danfse = NfseXmlReader.read(nfseXml);
        String qr = danfse.chaveAcesso() == null
            ? null
            : QrCodeGenerator.dataUri(QrCodeGenerator.consultaUrl(danfse.chaveAcesso(), producao), 180);
        String html = DanfseHtmlRenderer.render(danfse, qr);
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
        Objects.requireNonNull(saida, "saida is required");
        byte[] pdf = gerarPdf(nfseXml, producao);
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
}
