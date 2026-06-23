package br.com.nfse.sdk.service;

import br.com.nfse.sdk.http.NfseHttpResponse;

public record EmissaoNfseResult(NfseHttpResponse response, String xml, String signedXml) {
}
