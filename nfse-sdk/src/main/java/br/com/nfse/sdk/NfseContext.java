package br.com.nfse.sdk;

import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.http.EndpointResolver;
import java.util.Objects;
import java.util.Optional;

public final class NfseContext {
    private final Ambiente ambiente;
    private final CertificadoA1 certificado;
    private final String certificatePath;
    private final EndpointResolver endpointResolver;

    private NfseContext(Builder builder) {
        this.ambiente = Objects.requireNonNull(builder.ambiente, "ambiente is required");
        this.certificado = builder.certificado;
        this.certificatePath = builder.certificatePath;
        this.endpointResolver = Objects.requireNonNullElseGet(
            builder.endpointResolver,
            EndpointResolver::defaultResolver
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public Ambiente ambiente() {
        return ambiente;
    }

    public Optional<CertificadoA1> certificado() {
        return Optional.ofNullable(certificado);
    }

    public String certificatePath() {
        return certificatePath;
    }

    public EndpointResolver endpointResolver() {
        return endpointResolver;
    }

    public static final class Builder {
        private Ambiente ambiente;
        private CertificadoA1 certificado;
        private String certificatePath;
        private EndpointResolver endpointResolver;

        private Builder() {
        }

        public Builder ambiente(Ambiente ambiente) {
            this.ambiente = ambiente;
            return this;
        }

        public Builder certificado(CertificadoA1 certificado) {
            this.certificado = certificado;
            return this;
        }

        public Builder certificatePath(String certificatePath) {
            this.certificatePath = certificatePath;
            return this;
        }

        public Builder endpointResolver(EndpointResolver endpointResolver) {
            this.endpointResolver = endpointResolver;
            return this;
        }

        public NfseContext build() {
            return new NfseContext(this);
        }
    }
}
