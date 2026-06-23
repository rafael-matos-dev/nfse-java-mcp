package br.com.nfse.sdk.http;

import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.xml.XmlPayloadCodec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class SefinClient {
    private static final String JSON = "application/json";
    private static final Map<String, String> JSON_HEADERS = Map.of("Accept", JSON);

    private final NfseHttpClient httpClient;
    private final URI baseUri;

    public SefinClient(NfseContext context) {
        this(
            NfseHttpClient.create(context),
            context.endpointResolver().sefin(context.ambiente())
        );
    }

    SefinClient(NfseHttpClient httpClient, URI baseUri) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.baseUri = ensureTrailingSlash(Objects.requireNonNull(baseUri, "baseUri is required"));
    }

    public NfseHttpResponse emitirNfseXml(String dpsXml) {
        return emitirNfsePayload(XmlPayloadCodec.gzipBase64(dpsXml));
    }

    public NfseHttpResponse emitirNfsePayload(String dpsXmlGZipB64) {
        return httpClient.post(
            resolve("nfse"),
            json("dpsXmlGZipB64", dpsXmlGZipB64),
            JSON,
            JSON_HEADERS
        );
    }

    public NfseHttpResponse consultarNfse(String chaveAcesso) {
        return httpClient.get(resolve("nfse/" + pathSegment(chaveAcesso)), JSON_HEADERS);
    }

    public NfseHttpResponse consultarDps(String idDps) {
        return httpClient.get(resolve("dps/" + pathSegment(idDps)), JSON_HEADERS);
    }

    public boolean verificarDps(String idDps) {
        NfseHttpResponse response = httpClient.head(resolve("dps/" + pathSegment(idDps)), JSON_HEADERS);
        return response.isSuccessful();
    }

    public NfseHttpResponse registrarEventoXml(String chaveAcesso, String eventoXml) {
        return registrarEventoPayload(chaveAcesso, XmlPayloadCodec.gzipBase64(eventoXml));
    }

    public NfseHttpResponse registrarEventoPayload(String chaveAcesso, String eventoXmlGZipB64) {
        return httpClient.post(
            resolve("nfse/" + pathSegment(chaveAcesso) + "/eventos"),
            json("pedidoRegistroEventoXmlGZipB64", eventoXmlGZipB64),
            JSON,
            JSON_HEADERS
        );
    }

    public NfseHttpResponse consultarEvento(String chaveAcesso, int tipoEvento, int numeroSequencialEvento) {
        return httpClient.get(
            resolve(
                "nfse/" + pathSegment(chaveAcesso)
                    + "/eventos/" + tipoEvento
                    + "/" + numeroSequencialEvento
            ),
            JSON_HEADERS
        );
    }

    public NfseHttpResponse listarEventos(String chaveAcesso) {
        return httpClient.get(resolve("nfse/" + pathSegment(chaveAcesso) + "/eventos"), JSON_HEADERS);
    }

    public NfseHttpResponse listarEventosPorTipo(String chaveAcesso, int tipoEvento) {
        return httpClient.get(resolve("nfse/" + pathSegment(chaveAcesso) + "/eventos/" + tipoEvento), JSON_HEADERS);
    }

    private URI resolve(String path) {
        return baseUri.resolve(path);
    }

    private static URI ensureTrailingSlash(URI uri) {
        String value = uri.toString();
        if (value.endsWith("/")) {
            return uri;
        }
        return URI.create(value + "/");
    }

    private static String pathSegment(String value) {
        Objects.requireNonNull(value, "path segment is required");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String json(String key, String value) {
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(value, "value is required");
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
