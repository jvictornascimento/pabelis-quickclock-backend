# Pabelis QuickClock Backend

Backend Spring planejado para o QuickClock. Este projeto vai concentrar API,
persistencia, regras de negocio compartilhadas e suporte aos relatorios da PWA.

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

## Regras principais

O app nao controla horario exato. Cada dia pode marcar:

- antes do almoco
- depois do almoco

Relatorios devem considerar somente dias marcados, servicos adicionais e
orcamentos aprovados. Valores devem ser armazenados em centavos.

## Contexto compartilhado

As regras de produto ficam em `PROJECT_CONTEXT.md`. Quando ele mudar aqui,
tambem deve ser atualizado em:

- `/mnt/c/projetos/ponto-eletronico`
- `/mnt/z/react/QuickClock`

## Proximo passo

Inicializar o projeto Spring Boot neste diretorio e manter a API alinhada ao
contexto compartilhado.
