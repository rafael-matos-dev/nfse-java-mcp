# nfse-java-mcp

[![Maven Central](https://img.shields.io/maven-central/v/io.github.rafael-matos-dev/nfse-sdk?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.rafael-matos-dev/nfse-sdk)
[![CI](https://github.com/rafael-matos-dev/nfse-java-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/rafael-matos-dev/nfse-java-mcp/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

SDK Java **sem dependências de runtime** e **servidor MCP** para emitir **NFS-e Nacional** (padrão nacional brasileiro da Nota Fiscal de Serviço eletrônica). Feito para devs — e, via MCP, usável por qualquer pessoa através de um agente de IA: ela só aponta o certificado, passa os dados do tomador (ou uma nota de exemplo), a descrição e o valor, e o agente emite.

> ⚠️ **Documento fiscal real.** O padrão é **homologação** (produção restrita, ambiente de teste). Emitir em **produção** cria um documento fiscal com efeito tributário real e exige confirmação explícita (`confirmarProducao=true` / `--confirmar-producao`).

## Por que existe

Já existem servidores MCP de NFS-e Nacional em outras linguagens — o conceito não é inédito (veja [Créditos](#créditos-e-prior-art)). O que faltava era um **motor Java limpo e embarcável**: as bibliotecas Java existentes são municipais ou provas de conceito. Este projeto preenche essa lacuna e, por cima do SDK, oferece um servidor MCP em Java (1 runtime só).

## Três formas de usar

| Forma | Para quem | Como |
|------|-----------|------|
| **Servidor MCP** | agente de IA (Claude Desktop, etc.) | registra o jar e conversa |
| **CLI** | humanos e agentes que rodam shell | `java -jar nfse-cli.jar <comando>` |
| **SDK** | integração em sistemas Java | `NfseRunner` / `Nfse` |

## Requisitos

- Java 21+
- Maven 3.9+ (só para buildar a partir do código)
- Certificado digital **A1** (`.pfx`/`.p12`) do prestador

## Instalação

A forma mais rápida: baixe os jars prontos da [última release](https://github.com/rafael-matos-dev/nfse-java-mcp/releases/latest) — `nfse-mcp-<versao>.jar` (servidor MCP) e `nfse-cli-<versao>.jar` (CLI). Não precisa compilar.

Ou compile do código:

```bash
mvn clean package
```

Gera os jars executáveis:
- `nfse-mcp/target/nfse-mcp.jar` — servidor MCP
- `nfse-cli/target/nfse-cli.jar` — CLI

## Uso via MCP (destaque)

Registre o servidor no seu cliente MCP. Exemplo de `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "nfse": {
      "command": "java",
      "args": ["-jar", "/caminho/para/nfse-mcp.jar"],
      "env": {
        "NFSE_CERT_PATH": "/caminho/para/seu-certificado.pfx",
        "NFSE_CERT_PASSWORD": "sua-senha"
      }
    }
  }
}
```

Depois, é só conversar com o agente: _"Emita uma NFS-e de R$ 100 para o CPF 111.444.777-35, descrição 'consultoria', em homologação."_ — o agente chama a ferramenta `emitir_nfse`.

### Ferramentas expostas

| Ferramenta | O que faz |
|-----------|-----------|
| `validar_certificado` | Mostra CNPJ/CPF, emissor e validade do certificado A1. |
| `emitir_nfse` | Emite uma NFS-e a partir dos dados informados. |
| `emitir_de_exemplo` | Reaproveita uma nota anterior (XML), trocando só tomador/descrição/valor. |
| `consultar_nfse` | Consulta uma NFS-e pela chave de acesso. |
| `cancelar_nfse` | Cancela uma NFS-e (evento 101101). |
| `baixar_danfse` | Baixa o DANFSe/PDF. |

Todas aceitam `ambiente` (`homologacao` por padrão) e, nas operações de escrita, `confirmarProducao` (obrigatório `true` para produção). O certificado vem das envs `NFSE_CERT_PATH`/`NFSE_CERT_PASSWORD` ou dos parâmetros da ferramenta.

## Uso via CLI

```bash
JAR=nfse-cli/target/nfse-cli.jar
export NFSE_CERT_PATH=/caminho/cert.pfx NFSE_CERT_PASSWORD=senha

java -jar $JAR cert --json
java -jar $JAR emitir --arquivo docs/exemplos/emitir.request.json --json
java -jar $JAR emitir-de-exemplo --exemplo nota-antiga.xml --numero 123 \
  --tomador-cpf 11144477735 --tomador-nome "Fulano" --descricao "Consultoria" --valor 250.00
java -jar $JAR consultar --chave CHAVE_DA_NFSE --json
java -jar $JAR pdf --chave CHAVE_DA_NFSE --saida danfse.pdf
# Producao (documento fiscal REAL) exige a flag:
java -jar $JAR emitir --arquivo nota.json --ambiente producao --confirmar-producao
```

## Uso como SDK

Disponível no Maven Central:

```xml
<dependency>
  <groupId>io.github.rafael-matos-dev</groupId>
  <artifactId>nfse-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

```java
var cert = NfseRunner.carregarCertificado("/caminho/cert.pfx", "senha");
var resultado = NfseRunner.emitir(
    request,                 // EmitirNfseRequest
    Ambiente.HOMOLOGACAO,
    cert,
    false);                  // confirmarProducao
System.out.println(resultado.chaveAcesso());
```

O `nfse-sdk` não tem dependências de runtime (HTTP via `java.net.http`, assinatura via `javax.xml.crypto.dsig`, mTLS via `SSLContext` do A1).

## Emitir a partir de uma nota de exemplo

O fluxo mais simples para quem já emite: aponte uma nota anterior (XML de DPS ou NFS-e) e troque só o que muda. O `DpsXmlReader` lê o exemplo, o `DpsReemissao` aplica os overrides (novo número, tomador, descrição, valor) e regenera o `Id` da DPS.

## Ambientes e segurança

- **Homologação é o padrão.** Use para testar à vontade.
- **Produção cria documento fiscal real** e exige confirmação explícita.
- **Nunca** comite certificados (`.pfx`/`.p12`), `.env` com senha, nem os XMLs de notas emitidas (contêm dados reais de tomador/prestador). O `.gitignore` já bloqueia esses arquivos.

## Limitações

- A API oficial de download do DANFSe está prevista para ser desligada em **2026-07-01**; depois disso o PDF precisará ser gerado localmente (candidato a uma próxima versão).
- Geração de classes JAXB a partir dos XSDs oficiais e endurecimento de validações: próximos passos.

## Créditos e prior art

A ideia de um MCP para a NFS-e Nacional já existe e merece crédito:
- [`saviski/nfse-nacional-mcp`](https://github.com/saviski/nfse-nacional-mcp) — emissão via MCP em Python.
- [`SamuelMoraesF/mcp-nfse-nacional`](https://github.com/SamuelMoraesF/mcp-nfse-nacional) — consulta via MCP.

Este projeto é uma implementação independente em **Java**, com foco em um SDK limpo e embarcável.

## Licença

[MIT](LICENSE) © Rafael Matos
