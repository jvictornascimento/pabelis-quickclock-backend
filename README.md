# Pabelis QuickClock Backend

Backend Spring do QuickClock. Este projeto concentra API, persistencia, regras
de negocio compartilhadas e suporte aos relatorios da PWA.

## Por que este projeto existe?

O QuickClock comecou como um app Flutter, mas a versao definitiva precisa rodar
bem no iPhone sem depender da App Store. A conta Apple Developer custa US$ 99
por ano, e este e um app pessoal, nao comercial. Para esse uso, uma PWA e mais
pratica e evita pagar uma assinatura anual so para instalar o proprio app.

A divisao fica assim:

- `pabelis-quickclock-flutter`: app Flutter atual e referencia funcional.
- `pabelis-quickclock-front`: frontend React PWA.
- `pabelis-quickclock-backend`: backend Spring deste projeto.

Nao sao tres projetos por bagunca; cada um tem uma responsabilidade clara.

## Responsabilidade do backend

- Gerenciar empresas.
- Salvar pontos por periodo trabalhado.
- Salvar configuracoes por empresa.
- Controlar servicos adicionais.
- Controlar orcamentos, historico e aprovacao.
- Registrar entradas quando valores forem marcados como pagos.
- Registrar despesas, categorias, forma de pagamento e parcelas.
- Expor dados para dashboard financeiro mensal.
- Gerar ou apoiar relatorios mensais por empresa.

## Arquitetura

O backend segue arquitetura hexagonal:

```text
src/main/java/br/com/pabelis/quickclock/
  domain/       Regras e modelos sem dependencia de framework
  application/  Casos de uso e portas
  adapters/     HTTP, persistencia e integracoes
```

Controllers HTTP devem apenas receber a requisicao, chamar casos de uso e
devolver resposta. Regras de negocio ficam em `domain/` e `application/`.

## Regras principais

O app nao controla horario exato. Cada dia pode marcar:

- antes do almoco
- depois do almoco

Relatorios devem considerar somente dias marcados, servicos adicionais e
orcamentos aprovados. Valores devem ser armazenados em centavos.

## Desenvolvimento

Requisitos:

- Java 21
- Maven
- Docker
- PostgreSQL para execucao local completa

Rodar testes:

```bash
mvn test
```

Rodar a API localmente:

```bash
mvn spring-boot:run
```

Verificar saude da API:

```bash
curl http://localhost:8080/api/health
```

## Container

Gerar imagem:

```bash
docker build -t quickclock-backend .
```

Rodar container:

```bash
docker run --rm -p 8080:8080 quickclock-backend
```

O backend roda localmente no PC em container. O Compose fica no repositorio
frontend `/mnt/z/react/QuickClock/compose.yaml` e sobe API, PostgreSQL e proxy web.
Configure o .env do frontend e execute `docker compose up --build --wait` la.
A API e o banco ficam nas redes Docker, sem portas publicadas no host.
O acesso pessoal e HTTP pelo IP privado do ZeroTier, sem login ou DNS amigavel.

A imagem usa usuario sem privilegios e curl para o healthcheck HTTP. O endpoint
`/api/health` indica disponibilidade HTTP; nao e uma verificacao continua do banco.
Flyway e Hibernate validam o schema na inicializacao. O teste de CRUD e persistencia
precisa ser realizado contra PostgreSQL para confirmar o funcionamento completo.

## Banco de dados

O runtime usa PostgreSQL. As migrations ficam em
`src/main/resources/db/migration/` e sao executadas pelo Flyway.

Variaveis principais:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/quickclock
DATABASE_USERNAME=quickclock
DATABASE_PASSWORD=quickclock
```

Nos testes, o projeto usa H2 em memoria no modo PostgreSQL para validar que as
migrations sobem a partir de um banco limpo.

As migrations incrementais V2-V4 adicionam precos historicos por periodo,
historico de orcamentos e financeiro. V2 preenche pontos antigos com o valor
da configuracao existente (zero se ausente); nao recupera precos historicos
que nunca foram armazenados. Antes de migrar dados pessoais, fazer backup e
revisar essa aproximacao com o proprietario. Somente bancos de QA foram migrados.

## Fluxos implementados

- Empresas, expediente e valor por meio periodo.
- Ponto limitado ao dia atual em America/Sao_Paulo, com snapshot de preco.
- Servicos e orcamentos com historico, cancelamento e aprovacao idempotente.
- Relatorio mensal consolidado por empresa; PDF gerado pelo frontend.
- Recebimento integral por origem, sem duplicidade, e estorno sem historico.
- Despesas, cartoes, parcelas exatas e edicao de uma ou todas as nao pagas.
- Dashboard mensal com entradas/saidas realizadas e previstas.

`WORK_REPORT` identifica um dia de trabalho, nao um segundo credito agregado
do relatorio. Servicos e orcamentos aprovados tambem possuem origem unica.
Editar ou excluir uma origem recebida exige estornar o recebimento antes.
Relatorios separam recebido/pendente das suas origens, mesmo que o pagamento
ocorra em outro mes; o financeiro realizado usa a data efetiva do pagamento.

## Contexto compartilhado

As regras de produto ficam em `PROJECT_CONTEXT.md`. Quando ele mudar aqui,
tambem deve ser atualizado em:

- `/mnt/c/projetos/ponto-eletronico`
- `/mnt/z/react/QuickClock`

## Status

Os fluxos acima possuem testes automatizados e verificacao integrada em
PostgreSQL 17 isolado. Docker e frontend foram exercitados juntos em QA.
A validacao no Safari/ZeroTier real, backup/restauracao e migracao do SQLite
Flutter continuam pendentes na spec central. Nao ha login nem escrita offline.
