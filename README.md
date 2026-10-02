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

O backend sera publicado em uma VPS usando container. Configuracoes de producao
devem ser feitas por variaveis de ambiente.

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

## Contexto compartilhado

As regras de produto ficam em `PROJECT_CONTEXT.md`. Quando ele mudar aqui,
tambem deve ser atualizado em:

- `/mnt/c/projetos/ponto-eletronico`
- `/mnt/z/react/QuickClock`

## Status

A base inicial Spring Boot ja possui health endpoint em `/api/health`, testes e
Dockerfile. As proximas historias devem implementar banco, empresas,
configuracoes e regras de negocio conforme as issues do repositorio.
