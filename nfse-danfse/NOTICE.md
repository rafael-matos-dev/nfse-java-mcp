# Avisos de terceiros — nfse-danfse

## Logo da NFS-e

O arquivo `src/main/resources/danfse/nfse-logo.png` é o logotipo oficial da **NFS-e**
(Nota Fiscal de Serviço eletrônica), obtido do portal nacional:
https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/logos-da-nfs-e

Licença: **Creative Commons Atribuição-SemDerivações 3.0 (CC BY-ND 3.0)**.
O arquivo é redistribuído **sem modificações**, para uso no cabeçalho do DANFSe conforme a
Nota Técnica SE/CGNFS-e nº 008/2026.

## API do IBGE

Para resolver o nome de municípios que não vêm nomeados no XML, este módulo consulta a API pública
de localidades do IBGE (`servicodados.ibge.gov.br`) em tempo de geração, com cache em memória e
fallback gracioso ao código. Desative com `-Dnfse.danfse.ibge=false` (geração 100% offline).

## Dependências de runtime

- OpenHTMLtoPDF — LGPL-2.1
- Apache PDFBox / FontBox / XmpBox — Apache-2.0
- ZXing (core) — Apache-2.0

O código deste módulo é licenciado sob MIT (ver `LICENSE` na raiz do projeto).
