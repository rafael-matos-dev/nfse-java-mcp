package br.com.nfse.danfse;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Gera o QR Code do DANFSe como PNG embutido em data URI (para o template HTML).
 *
 * <p>O QR aponta para a consulta publica da NFS-e pela chave de acesso. A URL varia por ambiente
 * (producao x producao restrita). A NT 008 e a fonte normativa do conteudo exato; aqui usamos a
 * URL de consulta publica conhecida, que pode ser sobrescrita.
 */
public final class QrCodeGenerator {

    public static final String CONSULTA_PRODUCAO =
        "https://www.nfse.gov.br/consultapublica";
    public static final String CONSULTA_HOMOLOGACAO =
        "https://www.producaorestrita.nfse.gov.br/consultapublica";

    private QrCodeGenerator() {
    }

    /** URL de consulta publica (sem parametros) para a chave informada, por ambiente. */
    public static String consultaUrl(String chaveAcesso, boolean producao) {
        String base = producao ? CONSULTA_PRODUCAO : CONSULTA_HOMOLOGACAO;
        return base + "?tpc=1&chave=" + chaveAcesso;
    }

    /** Retorna o QR como data URI ({@code data:image/png;base64,...}) pronto para um <img src>. */
    public static String dataUri(String conteudo, int tamanhoPx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1,
                EncodeHintType.CHARACTER_SET, "UTF-8"
            );
            BitMatrix matrix = writer.encode(conteudo, BarcodeFormat.QR_CODE, tamanhoPx, tamanhoPx, hints);
            BufferedImage image = new BufferedImage(tamanhoPx, tamanhoPx, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < tamanhoPx; x++) {
                for (int y = 0; y < tamanhoPx; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception exception) {
            throw new DanfseException("Nao foi possivel gerar o QR Code do DANFSe.", exception);
        }
    }
}
