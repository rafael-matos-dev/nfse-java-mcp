# Changelog

Todos os releases relevantes deste projeto. O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o versionamento segue [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [0.2.0] - 2026-06-23

### Adicionado
- Workflow de release (GitHub Actions): ao publicar uma tag `v*`, anexa `nfse-mcp.jar` e
  `nfse-cli.jar` a um GitHub Release.
- `CHANGELOG.md` e badges (Maven Central, CI, licença) no README.
- Testes para `EmitirNfseRequest`, `DpsReemissao` e `NfseRunner`.

### Alterado
- Bloqueia emissão/cancelamento com **certificado A1 expirado** (falha cedo, com mensagem clara).
- `NfseRunner.emitir` exige CPF/CNPJ extraível do certificado quando o prestador não informa um.
- `EmitirNfseRequest` valida `numero > 0`.
- Servidor MCP: mensagens de erro por campo ao converter argumentos numéricos/data.

## [0.1.0] - 2026-06-23

### Adicionado
- `nfse-sdk` — motor Java sem dependências de runtime: emissão de DPS v1.01 (assinatura
  enveloped RSA-SHA256), consulta, cancelamento (evento 101101) e download de DANFSe; mTLS via
  certificado A1. Publicado no Maven Central como `io.github.rafael-matos-dev:nfse-sdk`.
- `DpsXmlReader` + `DpsReemissao` — emitir a partir de uma nota de exemplo, trocando só o que muda.
- `nfse-mcp` — servidor MCP (stdio) com as ferramentas `validar_certificado`, `emitir_nfse`,
  `emitir_de_exemplo`, `consultar_nfse`, `cancelar_nfse`, `baixar_danfse`.
- `nfse-cli` — CLI para humanos e agentes que rodam shell.
- Homologação por padrão; produção exige confirmação explícita.

[Não lançado]: https://github.com/rafael-matos-dev/nfse-java-mcp/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.2.0
[0.1.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.1.0
