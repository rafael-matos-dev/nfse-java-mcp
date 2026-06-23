package br.com.nfse.sdk.http;

import br.com.nfse.sdk.NfseContext;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class DanfseClient {
    private static final String PDF = "application/pdf";
    private static final Map<String, String> PDF_HEADERS = Map.of("Accept", PDF);

    private final NfseHttpClient httpClient;
    private final URI baseUri;

    public DanfseClient(NfseContext context) {
        this(
            NfseHttpClient.create(context),
            context.endpointResolver().danfse(context.ambiente())
        );
    }

    DanfseClient(NfseHttpClient httpClient, URI baseUri) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.baseUri = ensureTrailingSlash(Objects.requireNonNull(baseUri, "baseUri is required"));
    }

    public NfseBinaryResponse baixarPdf(String chaveAcesso) {
        return httpClient.getBytes(resolve(pathSegment(chaveAcesso)), PDF_HEADERS);
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
}
