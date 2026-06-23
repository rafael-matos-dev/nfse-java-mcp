package br.com.nfse.sdk;

import br.com.nfse.sdk.http.EndpointResolver;
import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfseContextTest {

    @Test
    void shouldBuildContextWithEnvironment() {
        NfseContext context = NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .certificatePath("/tmp/cert.pfx")
            .build();

        assertEquals(Ambiente.HOMOLOGACAO, context.ambiente());
        assertEquals("/tmp/cert.pfx", context.certificatePath());
        assertTrue(context.certificado().isEmpty());
    }

    @Test
    void shouldUseDefaultEndpointResolverWhenNoneIsProvided() {
        NfseContext context = NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .build();

        assertEquals(
            URI.create("https://sefin.producaorestrita.nfse.gov.br/SefinNacional"),
            context.endpointResolver().sefin(Ambiente.HOMOLOGACAO)
        );
    }

    @Test
    void shouldAcceptCustomEndpointResolver() {
        EndpointResolver resolver = EndpointResolver.withSefinEndpoints(
            URI.create("https://homologacao.example.test"),
            URI.create("https://producao.example.test")
        );

        NfseContext context = NfseContext.builder()
            .ambiente(Ambiente.HOMOLOGACAO)
            .endpointResolver(resolver)
            .build();

        assertEquals(
            URI.create("https://homologacao.example.test"),
            context.endpointResolver().sefin(Ambiente.HOMOLOGACAO)
        );
    }
}
