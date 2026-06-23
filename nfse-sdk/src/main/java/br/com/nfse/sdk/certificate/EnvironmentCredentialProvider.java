package br.com.nfse.sdk.certificate;

import java.util.Objects;
import java.util.function.Function;

public final class EnvironmentCredentialProvider implements CredentialProvider {
    private final String variableName;
    private final Function<String, String> lookup;

    private EnvironmentCredentialProvider(String variableName, Function<String, String> lookup) {
        this.variableName = Objects.requireNonNull(variableName, "variableName is required");
        this.lookup = Objects.requireNonNull(lookup, "lookup is required");
    }

    public static EnvironmentCredentialProvider fromEnv(String variableName) {
        return new EnvironmentCredentialProvider(variableName, System::getenv);
    }

    static EnvironmentCredentialProvider fromLookup(String variableName, Function<String, String> lookup) {
        return new EnvironmentCredentialProvider(variableName, lookup);
    }

    @Override
    public char[] resolvePassword() {
        String value = lookup.apply(variableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variavel de ambiente " + variableName + " nao configurada.");
        }
        return value.toCharArray();
    }
}
