package br.com.nfse.danfse;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Converte um arquivo de imagem em uma data URI pronta para embutir no DANFSe (ex.: logo do
 * emitente). Imagens raster (PNG/JPG/GIF/etc.) são <b>redimensionadas automaticamente</b> para um
 * tamanho adequado ao documento — assim um logo grande (vários MB / milhares de px) não infla o PDF
 * nem deixa a geração lenta. SVG é embutido como está (vetorial). Sem dependências externas.
 */
final class ImagemDataUri {

    /** Maior dimensão (px) do logo embutido. Acima disso, a imagem é reduzida proporcionalmente. */
    static final int MAX_LADO_PX = 600;

    private ImagemDataUri() {
    }

    static String de(Path arquivo) {
        try {
            String nome = arquivo.getFileName().toString().toLowerCase();
            if (nome.endsWith(".svg")) {
                return "data:image/svg+xml;base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(arquivo));
            }
            BufferedImage original = ImageIO.read(arquivo.toFile());
            if (original == null) {
                // Nao foi possivel decodificar como raster: embute o arquivo como esta.
                return "data:" + mimePorExtensao(nome) + ";base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(arquivo));
            }
            BufferedImage ajustada = redimensionarSeNecessario(original);
            // PNG preserva transparencia e e bom para logos.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(ajustada, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException exception) {
            throw new DanfseException("Nao foi possivel ler a imagem: " + arquivo, exception);
        }
    }

    private static BufferedImage redimensionarSeNecessario(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int maior = Math.max(w, h);
        if (maior <= MAX_LADO_PX) {
            return img;
        }
        double escala = (double) MAX_LADO_PX / maior;
        int nw = Math.max(1, (int) Math.round(w * escala));
        int nh = Math.max(1, (int) Math.round(h * escala));
        BufferedImage destino = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return destino;
    }

    private static String mimePorExtensao(String nome) {
        if (nome.endsWith(".jpg") || nome.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (nome.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/png";
    }
}
