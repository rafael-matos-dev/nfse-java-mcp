package br.com.nfse.sdk.api;

import br.com.nfse.sdk.Ambiente;
import br.com.nfse.sdk.Nfse;
import br.com.nfse.sdk.NfseContext;
import br.com.nfse.sdk.certificate.CertificadoA1;
import br.com.nfse.sdk.http.NfseBinaryResponse;
import br.com.nfse.sdk.http.NfseHttpResponse;
import br.com.nfse.sdk.service.EmissaoNfseResult;
import br.com.nfse.sdk.service.SuccessfulXmlLogger;
import br.com.nfse.sdk.xml.dps.Dps;
import br.com.nfse.sdk.xml.dps.DpsReemissao;
import br.com.nfse.sdk.xml.dps.DpsXmlReader;
import br.com.nfse.sdk.xml.evento.CancelamentoNfse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Facade de alto nivel da SDK: uma chamada por operacao de NFS-e, compartilhada por CLI e MCP.
 * Centraliza a trava de producao: operacoes de escrita em PRODUCAO exigem confirmacao explicita.
 * Sem dependencias externas (mantem a SDK zero-dep).
 */
public final class NfseRunner {

    private NfseRunner() {
    }

    public record EmissaoResult(int statusHttp, boolean sucesso, String ambiente,
                                String chaveAcesso, String corpo, String xmlLogPath) {
    }

    public record RespostaSimples(int statusHttp, boolean sucesso, String ambiente, String corpo) {
    }

    public record PdfResult(int statusHttp, boolean sucesso, String ambiente,
                            String caminho, int bytes, String corpo) {
    }

    public record CertInfo(String alias, String cpfCnpj, String subject, String issuer,
                           String validoDe, String validoAte, boolean expirado) {
    }

    public static CertInfo cert(CertificadoA1 cert) {
        return new CertInfo(
            cert.alias(),
            cert.cpfCnpj().orElse(null),
            cert.subject(),
            cert.issuer(),
            cert.validFrom().toString(),
            cert.validUntil().toString(),
            cert.isExpired(Clock.systemUTC())
        );
    }

    public static EmissaoResult emitir(EmitirNfseRequest request, Ambiente ambiente,
                                       CertificadoA1 cert, boolean confirmarProducao) {
        exigirConfirmacaoProducao(ambiente, confirmarProducao);
        validarCertificado(cert);
        // Quando o prestador nao informa documento, herdamos o do certificado — que precisa existir.
        String cpfCnpjPrestador = cert.cpfCnpj().orElseThrow(() -> new IllegalStateException(
            "Nao foi possivel extrair CPF/CNPJ do certificado; informe o documento do prestador no payload."));
        Dps dps = request.toDps(cpfCnpjPrestador);
        return emitirDps(dps, ambiente, cert, "emitir");
    }

    public static EmissaoResult emitirDeExemplo(String xmlExemplo, DpsReemissao.Overrides overrides,
                                                Ambiente ambiente, CertificadoA1 cert, boolean confirmarProducao) {
        exigirConfirmacaoProducao(ambiente, confirmarProducao);
        validarCertificado(cert);
        Dps exemplo = DpsXmlReader.read(xmlExemplo);
        Dps dps = DpsReemissao.reemitir(exemplo, overrides);
        return emitirDps(dps, ambiente, cert, "emitir-de-exemplo");
    }

    private static EmissaoResult emitirDps(Dps dps, Ambiente ambiente, CertificadoA1 cert, String operacao) {
        Nfse nfse = nfse(ambiente, cert);
        EmissaoNfseResult result = nfse.contribuinte().emitirDetalhado(dps);
        Optional<Path> xmlLog = SuccessfulXmlLogger.saveIfSuccessful(
            ambienteLabel(ambiente) + "-" + operacao, result.response(), result.signedXml());
        NfseHttpResponse response = result.response();
        return new EmissaoResult(
            response.statusCode(),
            response.isSuccessful(),
            ambienteLabel(ambiente),
            campo(response.body(), "chaveAcesso"),
            response.body(),
            xmlLog.map(p -> p.toAbsolutePath().normalize().toString()).orElse(null)
        );
    }

    public static RespostaSimples consultar(String chaveAcesso, Ambiente ambiente, CertificadoA1 cert) {
        NfseHttpResponse response = nfse(ambiente, cert).contribuinte().consultarNfse(chaveAcesso);
        return new RespostaSimples(response.statusCode(), response.isSuccessful(), ambienteLabel(ambiente), response.body());
    }

    public static RespostaSimples cancelar(String chaveAcesso, String cpfCnpjAutor, int numeroPedido,
                                           String codigoMotivo, String descricaoMotivo,
                                           Ambiente ambiente, CertificadoA1 cert, boolean confirmarProducao) {
        exigirConfirmacaoProducao(ambiente, confirmarProducao);
        validarCertificado(cert);
        String autor = (cpfCnpjAutor == null || cpfCnpjAutor.isBlank()) ? cert.cpfCnpj().orElse(null) : cpfCnpjAutor;
        CancelamentoNfse cancelamento = new CancelamentoNfse(
            chaveAcesso, autor, OffsetDateTime.now(), numeroPedido, codigoMotivo, descricaoMotivo, "nfse-java-mcp");
        NfseHttpResponse response = nfse(ambiente, cert).contribuinte().cancelar(cancelamento);
        return new RespostaSimples(response.statusCode(), response.isSuccessful(), ambienteLabel(ambiente), response.body());
    }

    public static PdfResult baixarPdf(String chaveAcesso, Path saida, Ambiente ambiente, CertificadoA1 cert) {
        NfseBinaryResponse response = nfse(ambiente, cert).danfse().baixarPdf(chaveAcesso, saida);
        return new PdfResult(
            response.statusCode(),
            response.isSuccessful(),
            ambienteLabel(ambiente),
            response.isSuccessful() ? saida.toAbsolutePath().normalize().toString() : null,
            response.body().length,
            response.isSuccessful() ? null : new String(response.body())
        );
    }

    public static CertificadoA1 carregarCertificado(String caminho, String senha) {
        return CertificadoA1.fromFile(Path.of(caminho), senha.toCharArray());
    }

    public static Ambiente ambiente(String valor) {
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("homologacao")) {
            return Ambiente.HOMOLOGACAO;
        }
        if (valor.equalsIgnoreCase("producao")) {
            return Ambiente.PRODUCAO;
        }
        throw new IllegalArgumentException("Ambiente invalido: " + valor + " (use homologacao ou producao).");
    }

    // Assinar com certificado fora da validade gera documento invalido — falha cedo, antes da rede.
    private static void validarCertificado(CertificadoA1 cert) {
        if (cert.isExpired(Clock.systemUTC())) {
            throw new IllegalStateException(
                "Certificado A1 fora da validade (valido de " + cert.validFrom() + " a " + cert.validUntil()
                    + "); nao e possivel emitir/cancelar.");
        }
    }

    private static void exigirConfirmacaoProducao(Ambiente ambiente, boolean confirmarProducao) {
        if (ambiente == Ambiente.PRODUCAO && !confirmarProducao) {
            throw new IllegalStateException(
                "Emissao em PRODUCAO cria documento fiscal REAL. Confirme explicitamente "
                    + "(CLI: --confirmar-producao; MCP: confirmarProducao=true).");
        }
    }

    private static Nfse nfse(Ambiente ambiente, CertificadoA1 cert) {
        return new Nfse(NfseContext.builder().ambiente(ambiente).certificado(cert).build());
    }

    private static String ambienteLabel(Ambiente ambiente) {
        return ambiente.name().toLowerCase();
    }

    /** Extrai um campo string simples de um corpo JSON (ex.: chaveAcesso), sem dependencia de parser. */
    private static String campo(String json, String nome) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String marca = "\"" + nome + "\"";
        int i = json.indexOf(marca);
        if (i < 0) {
            return null;
        }
        i = json.indexOf(':', i + marca.length());
        if (i < 0) {
            return null;
        }
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        int start = i + 1;
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }
}
