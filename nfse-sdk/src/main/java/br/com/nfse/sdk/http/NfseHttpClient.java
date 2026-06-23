package br.com.nfse.sdk.http;

import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.certificate.SSLContextFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class NfseHttpClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    private NfseHttpClient(HttpClient httpClient, Duration requestTimeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout is required");
    }

    public static NfseHttpClient create(NfseContext context) {
        Objects.requireNonNull(context, "context is required");

        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER);

        context.certificado()
            .map(SSLContextFactory::create)
            .ifPresent(builder::sslContext);

        return new NfseHttpClient(builder.build(), DEFAULT_TIMEOUT);
    }

    static NfseHttpClient withHttpClient(HttpClient httpClient, Duration requestTimeout) {
        return new NfseHttpClient(httpClient, requestTimeout);
    }

    public NfseHttpResponse get(URI uri) {
        return get(uri, Map.of());
    }

    public NfseHttpResponse get(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = baseRequest(uri, headers).GET();
        return send(builder.build());
    }

    public NfseBinaryResponse getBytes(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = baseRequest(uri, headers).GET();
        return sendBytes(builder.build());
    }

    public NfseHttpResponse post(URI uri, String body, String contentType) {
        return post(uri, body, contentType, Map.of());
    }

    public NfseHttpResponse post(URI uri, String body, String contentType, Map<String, String> headers) {
        Objects.requireNonNull(body, "body is required");
        Objects.requireNonNull(contentType, "contentType is required");

        HttpRequest.Builder builder = baseRequest(uri, headers)
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body));
        return send(builder.build());
    }

    public NfseHttpResponse head(URI uri) {
        return head(uri, Map.of());
    }

    public NfseHttpResponse head(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = baseRequest(uri, headers)
            .method("HEAD", HttpRequest.BodyPublishers.noBody());
        return send(builder.build());
    }

    private HttpRequest.Builder baseRequest(URI uri, Map<String, String> headers) {
        Objects.requireNonNull(uri, "uri is required");
        Objects.requireNonNull(headers, "headers is required");

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(requestTimeout);
        headers.forEach(builder::header);
        return builder;
    }

    private NfseHttpResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new NfseHttpResponse(
                response.statusCode(),
                response.body(),
                response.headers().map()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NfseHttpException("Requisicao NFS-e interrompida.", exception);
        } catch (Exception exception) {
            throw new NfseHttpException("Nao foi possivel executar requisicao NFS-e.", exception);
        }
    }

    private NfseBinaryResponse sendBytes(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new NfseBinaryResponse(
                response.statusCode(),
                response.body(),
                response.headers().map()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NfseHttpException("Requisicao NFS-e interrompida.", exception);
        } catch (Exception exception) {
            throw new NfseHttpException("Nao foi possivel executar requisicao NFS-e.", exception);
        }
    }
}
