package br.com.nfse.sdk.service;

import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.http.NfseHttpResponse;
import br.com.nfse.sdk.http.SefinClient;
import br.com.nfse.sdk.xml.XmlSigner;
import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsXmlBuilder;
import br.com.nfse.sdk.xml.evento.CancelamentoNfse;
import br.com.nfse.sdk.xml.evento.PedidoRegistroEventoXmlBuilder;

public final class ContribuinteService {
    private final NfseContext context;
    private final SefinClient sefinClient;
    private final DpsXmlBuilder dpsXmlBuilder;
    private final PedidoRegistroEventoXmlBuilder eventoXmlBuilder;

    public ContribuinteService(NfseContext context) {
        this.context = context;
        this.sefinClient = new SefinClient(context);
        this.dpsXmlBuilder = new DpsXmlBuilder();
        this.eventoXmlBuilder = new PedidoRegistroEventoXmlBuilder();
    }

    public String consultar(String chaveAcesso) {
        return consultarNfse(chaveAcesso).body();
    }

    public NfseHttpResponse emitir(Dps dps) {
        return emitirDetalhado(dps).response();
    }

    public EmissaoNfseResult emitirDetalhado(Dps dps) {
        Dps dpsNoAmbiente = comTipoAmbienteDoContexto(dps);
        String xml = dpsXmlBuilder.build(dpsNoAmbiente);
        String signedXml = XmlSigner.signInfDps(
            xml,
            context.certificado()
                .orElseThrow(() -> new ContribuinteServiceException("Certificado A1 e obrigatorio para emitir DPS."))
        );
        NfseHttpResponse response = sefinClient.emitirNfseXml(signedXml);
        return new EmissaoNfseResult(response, xml, signedXml);
    }

    // O tpAmb da DPS sempre segue o ambiente do contexto (= o endpoint usado). Sem isso,
    // um tpAmb divergente do endpoint faz a SEFIN rejeitar com E0006.
    private Dps comTipoAmbienteDoContexto(Dps dps) {
        int tpAmb = context.ambiente().tipoAmbiente();
        if (dps.infDps().tipoAmbiente() == tpAmb) {
            return dps;
        }
        return new Dps(dps.versao(), dps.infDps().withTipoAmbiente(tpAmb));
    }

    public NfseHttpResponse emitirXml(String dpsXml) {
        return sefinClient.emitirNfseXml(dpsXml);
    }

    public NfseHttpResponse cancelar(CancelamentoNfse cancelamento) {
        String xml = eventoXmlBuilder.buildCancelamento(context.ambiente().tipoAmbiente(), cancelamento);
        String signedXml = XmlSigner.signElement(
            xml,
            "infPedReg",
            context.certificado()
                .orElseThrow(() -> new ContribuinteServiceException("Certificado A1 e obrigatorio para cancelar NFS-e."))
        );
        return sefinClient.registrarEventoXml(cancelamento.chaveAcesso(), signedXml);
    }

    public NfseHttpResponse consultarNfse(String chaveAcesso) {
        return sefinClient.consultarNfse(chaveAcesso);
    }

    public NfseHttpResponse consultarDps(String idDps) {
        return sefinClient.consultarDps(idDps);
    }

    public boolean verificarDps(String idDps) {
        return sefinClient.verificarDps(idDps);
    }

    public NfseHttpResponse registrarEventoXml(String chaveAcesso, String eventoXml) {
        return sefinClient.registrarEventoXml(chaveAcesso, eventoXml);
    }
}
