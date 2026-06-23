package br.com.nfse.sdk.certificate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CertificadoA1 {
    private static final Pattern CPF_CNPJ_PATTERN = Pattern.compile("(?<!\\d)(\\d{14}|\\d{11})(?!\\d)");

    private final Path path;
    private final String alias;
    private final X509Certificate certificate;
    private final KeyStore keyStore;

    private CertificadoA1(Path path, String alias, X509Certificate certificate, KeyStore keyStore) {
        this.path = path;
        this.alias = alias;
        this.certificate = certificate;
        this.keyStore = keyStore;
    }

    public static CertificadoA1 fromFile(Path path, char[] password) {
        Objects.requireNonNull(path, "path is required");
        Objects.requireNonNull(password, "password is required");

        char[] passwordCopy = password.clone();
        try (InputStream input = Files.newInputStream(path)) {
            KeyStore source = KeyStore.getInstance("PKCS12");
            source.load(input, passwordCopy);

            String alias = findPrivateKeyAlias(source, passwordCopy)
                .orElseThrow(() -> new CertificadoA1Exception("Certificado A1 nao contem chave privada."));

            Key key = source.getKey(alias, passwordCopy);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new CertificadoA1Exception("Certificado A1 nao contem chave privada valida.");
            }

            Certificate[] chain = source.getCertificateChain(alias);
            if (chain == null || chain.length == 0 || !(chain[0] instanceof X509Certificate x509Certificate)) {
                throw new CertificadoA1Exception("Certificado A1 nao contem certificado X509 valido.");
            }

            KeyStore inMemory = KeyStore.getInstance("PKCS12");
            inMemory.load(null, null);
            inMemory.setKeyEntry(alias, privateKey, keyPassword(), chain);

            return new CertificadoA1(path.toAbsolutePath().normalize(), alias, x509Certificate, inMemory);
        } catch (CertificadoA1Exception exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CertificadoA1Exception(
                "Nao foi possivel carregar o certificado A1. Verifique o arquivo e a senha.",
                exception
            );
        } finally {
            Arrays.fill(passwordCopy, '\0');
        }
    }

    public Path path() {
        return path;
    }

    public String alias() {
        return alias;
    }

    public String subject() {
        return certificate.getSubjectX500Principal().getName();
    }

    public String issuer() {
        return certificate.getIssuerX500Principal().getName();
    }

    public Instant validFrom() {
        return certificate.getNotBefore().toInstant();
    }

    public Instant validUntil() {
        return certificate.getNotAfter().toInstant();
    }

    public boolean isExpired(Clock clock) {
        Instant now = clock.instant();
        return now.isBefore(validFrom()) || now.isAfter(validUntil());
    }

    public Optional<String> cpfCnpj() {
        Matcher matcher = CPF_CNPJ_PATTERN.matcher(subject());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public PrivateKey privateKey() {
        try {
            Key key = keyStore.getKey(alias, keyPassword());
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new CertificadoA1Exception("Certificado A1 nao contem chave privada valida.");
        } catch (CertificadoA1Exception exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CertificadoA1Exception("Nao foi possivel acessar a chave privada do certificado A1.", exception);
        }
    }

    KeyStore keyStore() {
        return keyStore;
    }

    static char[] keyPassword() {
        return new char[0];
    }

    private static Optional<String> findPrivateKeyAlias(KeyStore keyStore, char[] password) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String candidate = aliases.nextElement();
            if (keyStore.isKeyEntry(candidate) && keyStore.getKey(candidate, password) instanceof PrivateKey) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
