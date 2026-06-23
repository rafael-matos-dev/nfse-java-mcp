package br.com.nfse.sdk.http;

import br.com.nfse.sdk.Ambiente;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class EndpointResolver {
    private static final URI SEFIN_HOMOLOGACAO = URI.create(
        "https://sefin.producaorestrita.nfse.gov.br/SefinNacional"
    );
    private static final URI SEFIN_PRODUCAO = URI.create("https://sefin.nfse.gov.br/SefinNacional");
    private static final URI DANFSE_HOMOLOGACAO = URI.create("https://adn.producaorestrita.nfse.gov.br/danfse");
    private static final URI DANFSE_PRODUCAO = URI.create("https://adn.nfse.gov.br/danfse");

    private final Map<Ambiente, URI> sefinEndpoints;
    private final Map<Ambiente, URI> danfseEndpoints;

    private EndpointResolver(Map<Ambiente, URI> sefinEndpoints, Map<Ambiente, URI> danfseEndpoints) {
        this.sefinEndpoints = new EnumMap<>(sefinEndpoints);
        this.danfseEndpoints = new EnumMap<>(danfseEndpoints);
    }

    public static EndpointResolver defaultResolver() {
        EnumMap<Ambiente, URI> sefinEndpoints = new EnumMap<>(Ambiente.class);
        sefinEndpoints.put(Ambiente.HOMOLOGACAO, SEFIN_HOMOLOGACAO);
        sefinEndpoints.put(Ambiente.PRODUCAO, SEFIN_PRODUCAO);

        EnumMap<Ambiente, URI> danfseEndpoints = new EnumMap<>(Ambiente.class);
        danfseEndpoints.put(Ambiente.HOMOLOGACAO, DANFSE_HOMOLOGACAO);
        danfseEndpoints.put(Ambiente.PRODUCAO, DANFSE_PRODUCAO);

        return new EndpointResolver(sefinEndpoints, danfseEndpoints);
    }

    public static EndpointResolver withSefinEndpoints(URI homologacao, URI producao) {
        Objects.requireNonNull(homologacao, "homologacao is required");
        Objects.requireNonNull(producao, "producao is required");

        return defaultResolver().withSefin(homologacao, producao);
    }

    public static EndpointResolver withEndpoints(
        URI sefinHomologacao,
        URI sefinProducao,
        URI danfseHomologacao,
        URI danfseProducao
    ) {
        Objects.requireNonNull(sefinHomologacao, "sefinHomologacao is required");
        Objects.requireNonNull(sefinProducao, "sefinProducao is required");
        Objects.requireNonNull(danfseHomologacao, "danfseHomologacao is required");
        Objects.requireNonNull(danfseProducao, "danfseProducao is required");

        EnumMap<Ambiente, URI> sefinEndpoints = new EnumMap<>(Ambiente.class);
        sefinEndpoints.put(Ambiente.HOMOLOGACAO, sefinHomologacao);
        sefinEndpoints.put(Ambiente.PRODUCAO, sefinProducao);

        EnumMap<Ambiente, URI> danfseEndpoints = new EnumMap<>(Ambiente.class);
        danfseEndpoints.put(Ambiente.HOMOLOGACAO, danfseHomologacao);
        danfseEndpoints.put(Ambiente.PRODUCAO, danfseProducao);

        return new EndpointResolver(sefinEndpoints, danfseEndpoints);
    }

    public URI sefin(Ambiente ambiente) {
        URI endpoint = sefinEndpoints.get(Objects.requireNonNull(ambiente, "ambiente is required"));
        if (endpoint == null) {
            throw new IllegalArgumentException("Ambiente sem endpoint SEFIN configurado: " + ambiente);
        }
        return endpoint;
    }

    public URI danfse(Ambiente ambiente) {
        URI endpoint = danfseEndpoints.get(Objects.requireNonNull(ambiente, "ambiente is required"));
        if (endpoint == null) {
            throw new IllegalArgumentException("Ambiente sem endpoint DANFSe configurado: " + ambiente);
        }
        return endpoint;
    }

    private EndpointResolver withSefin(URI homologacao, URI producao) {
        EnumMap<Ambiente, URI> endpoints = new EnumMap<>(Ambiente.class);
        endpoints.put(Ambiente.HOMOLOGACAO, homologacao);
        endpoints.put(Ambiente.PRODUCAO, producao);
        return new EndpointResolver(endpoints, danfseEndpoints);
    }
}
