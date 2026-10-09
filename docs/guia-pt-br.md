# FlowPay — guia do projeto

## A ideia

Um pagamento pode ser simples no primeiro envio. A parte interessante aparece quando o cliente repete a requisição, o método muda ou uma regra de negócio impede a autorização. O FlowPay usa esse cenário para mostrar padrões de projeto em uma API pequena, com responsabilidades fáceis de localizar.

O projeto atende à alternativa do desafio DIO de criar uma implementação própria com Java e Spring. Ele não movimenta dinheiro: o AsterPay é um provedor fictício executado no mesmo processo.

## Comece por aqui

1. Instale um JDK 17 ou superior.
2. Na pasta do projeto, execute `bash ./mvnw clean verify`. No Windows, use `mvnw.cmd clean verify`.
3. Inicie a aplicação com `java -jar target/flowpay-1.0.0.jar`.
4. Consulte `http://localhost:8080/actuator/health`.
5. Use o exemplo de `curl` no [README](../README.md) ou as requisições de [requests.http](requests.http).

A primeira execução precisa baixar Maven e dependências. O JDK 17 foi a versão efetivamente utilizada na verificação. O arquivo Excel, uma interface visual e um banco de dados não fazem parte deste desafio.

## O que observar na demonstração

- Um PIX válido em BRL retorna `201` com um comprovante de autorização simulado.
- Repetir a mesma chave e o mesmo conteúdo retorna `200`, o mesmo ID e o cabeçalho `Idempotent-Replayed: true`.
- Alterar um valor válido e reutilizar a chave retorna `409`.
- Solicitar PIX em USD retorna `422`, pois o formato está correto, mas a regra de negócio não permite.
- O token fictício `tok_declined` gera uma autorização `DECLINED`; a recusa do provedor não é tratada como falha HTTP.
- Buscar um ID inexistente retorna `404`.

Não use dados pessoais, credenciais ou números de cartão nos testes.

## Por que os padrões estão aqui

| Padrão | Decisão concreta |
|---|---|
| Strategy | Cada método de pagamento implementa o mesmo contrato e escolhe seu canal de autorização. |
| Chain of Responsibility | As regras são aplicadas em ordem; a primeira rejeição interrompe a cadeia. |
| Adapter | O formato do SDK fictício, seus centavos e seus status ficam fora do domínio. |
| Observer | Logs e métricas acompanham o resultado sem ficarem dentro da regra principal. |
| Repository | O caso de uso acessa comprovantes por um contrato, não por uma coleção concreta. |

O registro de processadores é uma seleção de estratégias, não uma implementação anunciada de Factory Method. O escopo de beans do Spring também não é apresentado como Singleton escrito neste projeto.

## Como explicar o código

Leia `PaymentService` primeiro. Ele valida, reserva a chave, chama a estratégia adequada, salva o resultado e publica um evento. As classes de regra não dependem de Spring; a configuração monta a cadeia.

A idempotência usa uma reserva atômica e uma `CompletableFuture`. O teste de concorrência envia 24 solicitações em um pool de 12 threads e verifica que a operação do provedor é executada uma vez. Não existe um bloqueio global para todas as chaves.

O valor monetário usa `BigDecimal`; a conversão para centavos é exata e não arredonda silenciosamente. Um `Clock` injetado permite testes sem depender do horário real.

## Limites que fazem parte da entrega

Este protótipo guarda dados apenas na memória de um processo. Reiniciar apaga pagamentos e chaves. Os mapas não possuem limite de capacidade nem expiração. A API não tem autenticação, isolamento entre clientes, reconciliação ou integração real com instituição financeira.

Isso significa que o projeto está pronto para estudo e demonstração local, não para operar pagamentos reais. Os detalhes técnicos estão em [architecture.md](architecture.md), e os resultados efetivamente executados estão em [quality-report.md](quality-report.md). Não é necessário esconder esses limites ao apresentar o projeto: explicá-los mostra compreensão das decisões.

## Entrega na DIO

Depois de publicar, informe a URL principal do repositório público, não um link para o ZIP. Confira se `README.md`, `pom.xml`, os fontes, os testes e a pasta `docs` aparecem no GitHub. Aguarde a primeira execução do workflow; reporte o resultado real, sem presumir sucesso.

Descrição sugerida:

> FlowPay é uma API REST em Java e Spring Boot que aplica Strategy, Chain of Responsibility, Adapter, Observer e Repository a um fluxo simulado de autorização de pagamentos. O projeto inclui validação de regras de negócio, idempotência concorrente local, erros estruturados, testes automatizados, contrato OpenAPI e documentação das decisões e limitações.

## Roteiro curto para estudar antes de apresentar

1. Explique por que uma requisição repetida não deve gerar uma nova autorização.
2. Localize a troca de canal por método e mostre onde uma nova estratégia seria registrada.
3. Diferencie uma recusa de negócio de uma falha HTTP.
4. Mostre o teste concorrente e explique exatamente o que ele prova.
5. Explique por que memória local não resolve idempotência em vários servidores.

Use esse roteiro para compreender o que será publicado. Não atribua experiência de produção, resultados de clientes ou revisão humana externa a esta demonstração.
