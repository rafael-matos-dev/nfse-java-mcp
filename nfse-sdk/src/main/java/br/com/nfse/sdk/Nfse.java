package br.com.nfse.sdk;

import br.com.nfse.sdk.service.ContribuinteService;
import br.com.nfse.sdk.service.DanfseService;

public final class Nfse {
    private final ContribuinteService contribuinteService;
    private final DanfseService danfseService;

    public Nfse(NfseContext context) {
        this.contribuinteService = new ContribuinteService(context);
        this.danfseService = new DanfseService(context);
    }

    public ContribuinteService contribuinte() {
        return contribuinteService;
    }

    public DanfseService danfse() {
        return danfseService;
    }
}
