# CLAUDE.md — Lumi English

> Leia também `docs/PROJETO.md` e a fase atual em `docs/ROADMAP.md` antes de qualquer tarefa.

## O que é

Projeto de **estudo e portfólio**: um tutor de inglês com IA para iniciantes (qualquer idade), introdutório e
didático. Roda **localmente** com Docker Compose, para **um único usuário** (sem login, sem contas).
O objetivo é demonstrar com clareza: **RAG**, **agente com tool calling**, **Spring Boot** e **React + TS**.

**Mantenha simples.** Prefira a solução mais direta que demonstre o conceito. Não adicione autenticação,
microsserviços, Redis, Kubernetes, filas ou observabilidade avançada, a menos que a fase peça.

## Princípio central

**O modelo gera a conversa; o backend controla os dados.** O LLM pode pedir ferramentas, mas o Java valida
os argumentos antes de executar e é o único que grava no banco.

## Stack

- Java 21, Spring Boot 4.x, Spring AI 2.x (OpenAI + PgVectorStore)
- PostgreSQL 17 + pgvector, Flyway
- React + TypeScript + Vite + Tailwind
- Testes: JUnit 5, AssertJ, Mockito, Testcontainers (Postgres)
- Docker Compose

Confira as versões estáveis mais recentes compatíveis antes de criar o projeto.

## Estrutura

```
/backend   Spring Boot — pacotes por funcionalidade em dev.lumi:
             settings/    perfil do aprendiz (idade, nível) — linha única no banco
             curriculum/  ingestão das lições e busca vetorial (RAG)
             tutor/       chat, agente, ferramentas, prompts
             progress/    erros, palavras aprendidas, revisão
             speech/      transcrição e voz (Fase 3)
             common/      config, tratamento de erro, log de uso da IA
/frontend  React + TS
/curriculum  lições em Markdown (fonte do RAG)
/eval      perguntas de avaliação e relatórios (Fase 4)
docker-compose.yml
```

## Convenções

- Código, commits, README e ADRs em **inglês**. Docs de planejamento podem ficar em português.
- Records para DTOs. Controllers finos, lógica em serviços.
- Nomes de modelos, top-k e limites em `application.yml` (lidos de variáveis de ambiente), nunca no código.
- Prompts em `backend/src/main/resources/prompts/*.st`.
- A resposta do tutor é **JSON estruturado** mapeado para um record e validado.
- Registre cada chamada à OpenAI na tabela `ai_call_log` (operação, modelo, tokens, latência).
- Commits pequenos no padrão Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`).

## Testes

- Testes para a lógica que importa: chunking, montagem do prompt, validação das ferramentas, regras de progresso.
- **Testes nunca chamam a OpenAI real**: mocke o `ChatModel`/`EmbeddingModel` ou use respostas fixas.
- Um teste de integração com Testcontainers cobrindo ingestão + busca no pgvector.

## Comandos

> Mantenha atualizado.

```bash
cp .env.example .env                                   # 1ª vez; o backend lê o mesmo .env
docker compose up -d                                   # postgres + pgvector em localhost:5433 (5432 é do Postgres local do Windows)
cd backend && ./mvnw verify                            # build + testes (Testcontainers, precisa do Docker)
cd backend && ./mvnw spring-boot:run                   # API em :8080 (health: /actuator/health)
cd frontend && npm install && npm run dev              # UI em :5173 (proxy /api → :8080)
cd frontend && npm run build && npm run lint           # checagem de tipos + build + oxlint
```

## Como trabalhar

1. Faça **só a fase pedida**. Apresente um plano curto antes de codar.
2. Ao fim da fase: `./mvnw verify` passando, README atualizado, e um resumo explicando as decisões para o usuário.
3. Nunca commite segredos: use `.env` (no `.gitignore`) e mantenha `.env.example`.
