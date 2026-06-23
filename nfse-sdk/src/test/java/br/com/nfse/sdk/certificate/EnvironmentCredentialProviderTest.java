package br.com.nfse.sdk.certificate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentCredentialProviderTest {

    @Test
    void shouldResolvePasswordFromLookup() {
        EnvironmentCredentialProvider provider = EnvironmentCredentialProvider.fromLookup(
            "NFSE_CERT_PASSWORD",
            key -> "senha-secreta"
        );

        assertArrayEquals("senha-secreta".toCharArray(), provider.resolvePassword());
    }

    @Test
    void shouldFailWhenVariableIsMissing() {
        EnvironmentCredentialProvider provider = EnvironmentCredentialProvider.fromLookup(
            "NFSE_CERT_PASSWORD",
            key -> null
        );

        assertThrows(IllegalStateException.class, provider::resolvePassword);
    }
}
