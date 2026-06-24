# Changelog

Todos os releases relevantes deste projeto. O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o versionamento segue [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [0.4.0] - 2026-06-23

### Adicionado
- DANFSe muito mais próximo do oficial: **logo oficial da NFS-e** no cabeçalho (CC BY-ND, ver
  `nfse-danfse/NOTICE.md`), aviso **"NFS-e SEM VALIDADE JURÍDICA"** em vermelho quando a nota é de
  homologação (lê `ambGer`/`tpAmb` do XML), cabeçalho em 3 colunas e layout mais compacto.
- `DanfseConfig` — identificação opcional do município (brasão + contato da prefeitura), já que esses
  dados não vêm no XML nem há API pública que os forneça. `DanfseGenerator.gerarPdf(xml, producao, config[, saida])`.

### Corrigido
- Cancelamento (evento 101101): o `nPedRegEvento` foi **removido** do Id do pedido de evento conforme
  o padrão `TSIdPedRegEvt` (`PRE[0-9]{56}` = `PRE` + chave de 50 + tipo de evento 6). Anexar o número
  do pedido gerava rejeição no esquema XML da SEFIN. O `numeroPedido` segue no modelo, mas não compõe o Id.

## [0.3.0] - 2026-06-23

### Adicionado
- **Módulo `nfse-danfse`**: geração **local** do PDF do DANFSe a partir do XML autorizado da NFS-e,
  sem depender da API oficial (desligada em 1º/07/2026 — NT SE/CGNFS-e nº 008/2026). Render HTML/CSS
  → PDF (OpenHTMLtoPDF) + QR Code (ZXing); layout NT 008 e seção **IBS/CBS** (NT 009) condicional.
  Publicado no Maven Central como `io.github.rafael-matos-dev:nfse-danfse`.
- MCP: ferramenta **`gerar_danfse`** (aceita o XML, um arquivo ou o `nfseXmlGZipB64`).
- CLI: subcomando **`danfse --xml nota.xml --saida danfse.pdf`**.

### Alterado
- `baixar_danfse` (MCP) marcada como **legado** (válida até 2026-07-01); prefira `gerar_danfse`.

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

[Não lançado]: https://github.com/rafael-matos-dev/nfse-java-mcp/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.4.0
[0.3.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.3.0
[0.2.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.2.0
[0.1.0]: https://github.com/rafael-matos-dev/nfse-java-mcp/releases/tag/v0.1.0
