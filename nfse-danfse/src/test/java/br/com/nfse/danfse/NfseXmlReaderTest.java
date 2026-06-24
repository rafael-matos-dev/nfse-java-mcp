package br.com.nfse.danfse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NfseXmlReaderTest {

    static String xmlExemplo() throws IOException {
        try (var in = NfseXmlReaderTest.class.getResourceAsStream("/nfse-exemplo-ficticio.xml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void extraiChaveDeAcessoRemovendoPrefixoNFS() throws Exception {
        Danfse d = NfseXmlReader.read(xmlExemplo());
        assertEquals("35503082212345678000199000000000001234500000000000001", d.chaveAcesso());
    }

    @Test
    void extraiIdentificacaoNfseEDps() throws Exception {
        Danfse.Identificacao id = NfseXmlReader.read(xmlExemplo()).identificacao();
        assertEquals("12345", id.numeroNfse());
        assertEquals("1234", id.numeroDps());
        assertEquals("00900", id.serieDps());
        assertEquals("Sao Paulo", id.municipioEmissao());
        assertEquals(java.time.LocalDate.parse("2026-06-23"), id.competencia());
    }

    @Test
    void extraiPrestadorComEnderecoERegimeSimples() throws Exception {
        Danfse.Pessoa p = NfseXmlReader.read(xmlExemplo()).prestador();
        assertEquals("12345678000199", p.cnpj());
        assertEquals("1234567890", p.inscricaoMunicipal());
        assertEquals("EMPRESA EXEMPLO LTDA", p.nome());
        assertEquals("Centro", p.endereco().bairro());
        assertEquals("SP", p.endereco().uf());
        assertTrue(p.regimeSimplesNacional().contains("ME/EPP"));
    }

    @Test
    void extraiTomadorPorCpfComEnderecoAninhado() throws Exception {
        Danfse.Pessoa t = NfseXmlReader.read(xmlExemplo()).tomador();
        assertEquals("11144477735", t.cpf());
        assertNull(t.cnpj());
        assertEquals("Fulano de Tal", t.nome());
        assertEquals("Avenida Central", t.endereco().logradouro());
        assertEquals("3550308", t.endereco().municipio());
    }

    @Test
    void extraiServicoEValores() throws Exception {
        Danfse d = NfseXmlReader.read(xmlExemplo());
        assertEquals("010101", d.servico().codigoTributacaoNacional());
        assertEquals("Operacao Tributavel", d.servico().tributacaoIssqn());
        assertEquals("Nao Retido", d.servico().tipoRetencaoIssqn());
        assertEquals(0, d.valores().valorServico().compareTo(new BigDecimal("250.00")));
        assertEquals(0, d.valores().valorLiquido().compareTo(new BigDecimal("250.00")));
    }

    @Test
    void semIbsCbsQuandoAusente() throws Exception {
        assertNull(NfseXmlReader.read(xmlExemplo()).ibsCbs());
    }

    @Test
    void xmlSemInfNFSeFalha() {
        assertThrows(DanfseException.class, () -> NfseXmlReader.read("<NFSe></NFSe>"));
    }
}
