package br.com.nfse.danfse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImagemDataUriTest {

    @Test
    void redimensionaImagemGrandeParaCaberNoDocumento(@TempDir Path dir) throws Exception {
        // Imagem 2000x2000 (maior que MAX_LADO_PX) -> deve ser reduzida.
        BufferedImage grande = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        Path arquivo = dir.resolve("logo-grande.png");
        ImageIO.write(grande, "png", arquivo.toFile());

        String dataUri = ImagemDataUri.de(arquivo);

        assertTrue(dataUri.startsWith("data:image/png;base64,"), "deve ser PNG data URI");
        byte[] bytes = java.util.Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        BufferedImage resultado = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        assertTrue(Math.max(resultado.getWidth(), resultado.getHeight()) <= ImagemDataUri.MAX_LADO_PX,
            "maior lado deve respeitar o limite (recebido: "
                + resultado.getWidth() + "x" + resultado.getHeight() + ")");
        // proporcao preservada (quadrado continua quadrado)
        assertTrue(resultado.getWidth() == resultado.getHeight(), "proporcao preservada");
    }

    @Test
    void mantemImagemPequenaSemRedimensionar(@TempDir Path dir) throws Exception {
        BufferedImage pequena = new BufferedImage(120, 48, BufferedImage.TYPE_INT_ARGB);
        Path arquivo = dir.resolve("logo-pequeno.png");
        ImageIO.write(pequena, "png", arquivo.toFile());

        String dataUri = ImagemDataUri.de(arquivo);
        byte[] bytes = java.util.Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        BufferedImage resultado = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        assertTrue(resultado.getWidth() == 120 && resultado.getHeight() == 48, "nao deve alterar");
    }

    @Test
    void svgEhEmbutidoComoVetor(@TempDir Path dir) throws Exception {
        Path svg = dir.resolve("logo.svg");
        java.nio.file.Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
        String dataUri = ImagemDataUri.de(svg);
        assertTrue(dataUri.startsWith("data:image/svg+xml;base64,"), "SVG mantem vetor");
    }
}
