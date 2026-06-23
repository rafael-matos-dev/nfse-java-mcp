package br.com.nfse.sdk.certificate;

import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.TrustManagerFactory;

public final class SSLContextFactory {

    private SSLContextFactory() {
    }

    public static SSLContext create(CertificadoA1 certificado) {
        Objects.requireNonNull(certificado, "certificado is required");

        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            );
            keyManagerFactory.init(certificado.keyStore(), CertificadoA1.keyPassword());
            X509ExtendedKeyManager keyManager = fixedAliasKeyManager(keyManagerFactory, certificado.alias());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init((KeyStore) null);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                new X509ExtendedKeyManager[] {keyManager},
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
            );
            return sslContext;
        } catch (Exception exception) {
            throw new CertificadoA1Exception("Nao foi possivel criar SSLContext para o certificado A1.", exception);
        }
    }

    private static X509ExtendedKeyManager fixedAliasKeyManager(
        KeyManagerFactory keyManagerFactory,
        String alias
    ) {
        for (var keyManager : keyManagerFactory.getKeyManagers()) {
            if (keyManager instanceof X509ExtendedKeyManager extendedKeyManager) {
                return new FixedAliasKeyManager(extendedKeyManager, alias);
            }
            if (keyManager instanceof X509KeyManager x509KeyManager) {
                return new FixedAliasKeyManager(new X509KeyManagerAdapter(x509KeyManager), alias);
            }
        }
        throw new CertificadoA1Exception("Nao foi possivel localizar KeyManager X509 para o certificado A1.");
    }

    private static final class FixedAliasKeyManager extends X509ExtendedKeyManager {
        private final X509ExtendedKeyManager delegate;
        private final String alias;

        private FixedAliasKeyManager(X509ExtendedKeyManager delegate, String alias) {
            this.delegate = Objects.requireNonNull(delegate, "delegate is required");
            this.alias = Objects.requireNonNull(alias, "alias is required");
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, java.net.Socket socket) {
            return alias;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return alias;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, java.net.Socket socket) {
            return delegate.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return delegate.chooseEngineServerAlias(keyType, issuers, engine);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return delegate.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return delegate.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return delegate.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return delegate.getServerAliases(keyType, issuers);
        }
    }

    private static final class X509KeyManagerAdapter extends X509ExtendedKeyManager {
        private final X509KeyManager delegate;

        private X509KeyManagerAdapter(X509KeyManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, java.net.Socket socket) {
            return delegate.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, java.net.Socket socket) {
            return delegate.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return delegate.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return delegate.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return delegate.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return delegate.getServerAliases(keyType, issuers);
        }
    }
}
