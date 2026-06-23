package br.com.nfse.sdk.xml.dps;

public class DpsXmlException extends RuntimeException {

    public DpsXmlException(String message) {
        super(message);
    }

    public DpsXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
