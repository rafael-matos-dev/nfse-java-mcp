package br.com.nfse.sdk.http;

public class NfseHttpException extends RuntimeException {

    public NfseHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
