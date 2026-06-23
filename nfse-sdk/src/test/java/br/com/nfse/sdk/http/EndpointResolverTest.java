package br.com.nfse.sdk.http;

import br.com.nfse.sdk.Ambiente;
import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndpointResolverTest {

    @Test
    void shouldResolveOfficialSefinEndpoints() {
        EndpointResolver resolver = EndpointResolver.defaultResolver();

        assertEquals(
            URI.create("https://sefin.producaorestrita.nfse.gov.br/SefinNacional"),
            resolver.sefin(Ambiente.HOMOLOGACAO)
        );
        assertEquals(
            URI.create("https://sefin.nfse.gov.br/SefinNacional"),
            resolver.sefin(Ambiente.PRODUCAO)
        );
    }

    @Test
    void shouldAllowCustomSefinEndpointsForTests() {
        EndpointResolver resolver = EndpointResolver.withSefinEndpoints(
            URI.create("https://homologacao.example.test"),
            URI.create("https://producao.example.test")
        );

        assertEquals(URI.create("https://homologacao.example.test"), resolver.sefin(Ambiente.HOMOLOGACAO));
        assertEquals(URI.create("https://producao.example.test"), resolver.sefin(Ambiente.PRODUCAO));
    }
}
