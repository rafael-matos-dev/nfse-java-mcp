package br.com.nfse.sdk.service;

import br.com.nfse.sdk.http.NfseHttpResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SuccessfulXmlLogger {
    private static final Pattern CHAVE_ACESSO = Pattern.compile("\"chaveAcesso\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID_DPS = Pattern.compile("\"idDps\"\\s*:\\s*\"([^\"]+)\"");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SuccessfulXmlLogger() {
    }

    public static Optional<Path> saveIfSuccessful(String operation, NfseHttpResponse response, String signedXml) {
        if (!response.isSuccessful()) {
            return Optional.empty();
        }

        try {
            Path directory = Path.of(envOr("NFSE_XML_SUCCESS_LOG_DIR", "target/nfse-success-xml"));
            Files.createDirectories(directory);
            Path path = directory.resolve(fileName(operation, response.body()));
            Files.writeString(path, signedXml);
            return Optional.of(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel gravar XML de envio com sucesso.", exception);
        }
    }

    private static String fileName(String operation, String body) {
        String reference = extract(CHAVE_ACESSO, body)
            .or(() -> extract(ID_DPS, body))
            .orElse("sem-chave");
        String timestamp = FILE_TIMESTAMP.format(OffsetDateTime.now());
        return timestamp + "-" + sanitize(operation) + "-" + sanitize(reference) + ".xml";
    }

    private static Optional<String> extract(Pattern pattern, String body) {
        if (body == null) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String envOr(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
