package br.com.nfse.sdk.certificate;

@FunctionalInterface
public interface CredentialProvider {
    char[] resolvePassword();
}
