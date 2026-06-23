package br.com.nfse.sdk.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class XmlPayloadCodec {

    private XmlPayloadCodec() {
    }

    public static String gzipBase64(String xml) {
        Objects.requireNonNull(xml, "xml is required");

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(xml.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new XmlPayloadException("Nao foi possivel compactar XML em GZip/Base64.", exception);
        }
    }

    public static String ungzipBase64(String payload) {
        Objects.requireNonNull(payload, "payload is required");

        try {
            byte[] compressed = Base64.getDecoder().decode(payload);
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            throw new XmlPayloadException("Nao foi possivel descompactar payload GZip/Base64.", exception);
        }
    }
}
