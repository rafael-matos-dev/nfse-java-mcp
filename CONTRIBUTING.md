# Contribuindo

Contribuições são bem-vindas! Algumas diretrizes:

## Desenvolvimento

- Java 21, UTF-8, 4 espaços de indentação. Pacote raiz `br.com.nfse`.
- Mantenha o `nfse-sdk` **sem dependências de runtime** (só JDK).
- Rode os testes antes de abrir PR: `mvn test`.
- Testes não devem exigir certificado real — gere material de teste com `TestPkcs12Factory`.

## Build e teste

```bash
mvn clean install          # build + testes dos 3 modulos
mvn -pl nfse-sdk test      # so o SDK
```

## Segurança — leia antes de commitar

- **Nunca** inclua certificados reais (`.pfx`/`.p12`), `.env` com senha, ou XMLs de notas emitidas (dados de contribuinte). O `.gitignore` bloqueia esses arquivos — confirme antes de cada commit.
- Use dados **fictícios** em exemplos e testes.
- Produção emite documento fiscal real: mudanças que afetem o fluxo de emissão devem preservar a trava de produção (confirmação explícita).

## Pull Requests

Inclua um resumo da mudança, os comandos de teste rodados e, para mudanças no fluxo de emissão, descreva como validou em homologação.
