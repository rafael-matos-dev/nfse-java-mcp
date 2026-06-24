package br.com.nfse.danfse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve o nome ("Cidade - UF") de um código IBGE de município consultando a API pública do IBGE,
 * para os municípios que o XML da NFS-e não traz por nome (ex.: tomador em outra cidade).
 *
 * <p>Robusto por design: resultado em cache em memória, timeout curto e <b>fallback gracioso</b> —
 * se o IBGE estiver indisponível/offline, retorna vazio e o DANFSe mostra o próprio código. Nunca
 * lança exceção que quebre a geração do PDF.
 */
public final class MunicipioResolver {

    private static final String API = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios/";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final Pattern NOME = Pattern.compile("\"nome\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIGLA = Pattern.compile("\"sigla\"\\s*:\\s*\"([^\"]+)\"");

    // Cache compartilhado entre gerações. "" marca código consultado sem sucesso (evita repetir).
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private MunicipioResolver() {
    }

    /**
     * Retorna "Cidade - UF" para o código IBGE, ou vazio se não resolver (offline/desconhecido).
     * Desative a consulta ao IBGE com {@code -Dnfse.danfse.ibge=false} (modo offline).
     */
    public static Optional<String> resolver(String codigoMunicipio) {
        if (codigoMunicipio == null || !codigoMunicipio.matches("\\d{7}")) {
            return Optional.empty();
        }
        if ("false".equalsIgnoreCase(System.getProperty("nfse.danfse.ibge"))) {
            return Optional.empty();
        }
        String cached = CACHE.get(codigoMunicipio);
        if (cached != null) {
            return cached.isEmpty() ? Optional.empty() : Optional.of(cached);
        }
        String resolvido = consultarIbge(codigoMunicipio);
        CACHE.put(codigoMunicipio, resolvido == null ? "" : resolvido);
        return Optional.ofNullable(resolvido);
    }

    private static String consultarIbge(String codigo) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API + codigo))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            // 1o "nome" = municipio; 1o "sigla" = UF (vem antes da sigla da regiao).
            Matcher mNome = NOME.matcher(resp.body());
            Matcher mSigla = SIGLA.matcher(resp.body());
            if (!mNome.find()) {
                return null;
            }
            String nome = mNome.group(1);
            return mSigla.find() ? nome + " - " + mSigla.group(1) : nome;
        } catch (Exception exception) {
            return null; // offline / timeout / erro: fallback gracioso
        }
    }
}
