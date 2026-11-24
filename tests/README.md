# Testes e evidências da Parte 1

Tudo aqui foi gerado de verdade, rodando o IMDTravel da equipe (imagem construída de
`imdtravel/`) com AirlinesHub, Exchange e Fidelity nas imagens `baseline` do Docker Hub, em
2026-11-24. O sistema não tem nenhum mecanismo de tolerância a falhas (spec §4), e o que está
abaixo é o que acontece quando as dependências falham.

| Arquivo | O que é |
|---|---|
| `observar.sh` | O roteiro de observação O1 a O8. `./tests/observar.sh` roda todos, `./tests/observar.sh O3 O7` roda só os citados. Requer docker compose, curl e jq. |
| `evidencias/O1` … `O8/` | Evidência de cada cenário (ver abaixo). |
| `evidencias/testes-sh.txt` | Saída de `./testes.sh --reiniciar`: **40 passaram, 0 falharam**. |
| `evidencias/testes-sh-com-falhas.txt` | Saída de `./testes.sh --com-falhas`: **43 passaram, 0 falharam** (a seção 7 levou 72 s até o 500). |
| `evidencias/testes-unitarios.txt` | Saída de `./mvnw test` (em container): **44 testes, 0 falhas**. |
| `revisao-spec4.md` | Revisão do código contra a §4: nenhuma violação. |

Cada pasta `evidencias/Ox/` tem: `terminal.txt` (saída do terminal), `respostas*.tsv` (uma linha por
requisição: cid, instante, status HTTP, tempo em segundos, código de saída do curl, corpo),
`logs-imdtravel.jsonl` (log JSON do IMDTravel) e `logs-todos.txt` (os quatro serviços), `sales.json`
(retrato do `/sales`), `auditoria.txt` e `resumo.txt`.

**Como ler a auditoria.** Para cada par (voo, dia), compara três contagens: assentos consumidos
(`20 − seatsAvailable`, lido no AirlinesHub), linhas no `/sales` do IMDTravel e entradas/soma de
pontos no `/points` do Fidelity (um usuário por par, `<cenário>-<voo>-<dia>`). Tudo consistente quando
as três batem. A coluna "esperados" é a soma de `floor(valueUSD)` das vendas registradas.

**Sobre o limite de espera.** O IMDTravel não tem timeout. Quando um cenário diz "o cliente
desistiu", é o `curl -m` do script de observação, nunca o sistema.

## Resultado dos cenários

| # | Cenário | Resultado observado | Consistente? |
|---|---|---|---|
| O1 | 50 compras sequenciais, 5 pares | 50 × 201. Tempo: p50 0,62 s, p95 0,89 s, máx 0,99 s. Assentos consumidos 50 = vendas 50 = 50 bônus | Sim |
| O2 | 60 compras, `xargs -P 25`, mesmo par | 20 × 201 e 40 × 409. Estoque terminou em 0 (nunca negativo). 20 consumidos = 20 vendas = 20 bônus | Sim |
| O3 | `stop fidelity`, 3 compras | 3 × 500 em 0,7 a 1,0 s. 3 assentos consumidos, 0 vendas, 0 bônus. Com conexão já aberta, o 500 levou **71,6 s** | **Não** (assentos perdidos) |
| O4 | `pause fidelity` | As duas compras travaram. O cliente esperou 180 s sem 500 nem timeout. Depois do `unpause`, as duas terminaram em 201 | Sim no fim; 180 s com assento retido e sem venda |
| O5 | `stop exchange`, 3 compras | 3 × 500 em 0,006 a 0,1 s. 0 assentos consumidos | Sim (nada perdido) |
| O6 | `stop airlineshub`, 3 compras | 3 × 500 em 0,003 a 0,07 s. 0 assentos consumidos | Sim (nada perdido) |
| O7 | `kill imdtravel` no meio de 60 compras (20 em paralelo, após 2 s) | 28 × 201 e 32 clientes com conexão perdida (curl 52 ou 56). 48 assentos consumidos, 28 vendas, 28 bônus | **Não**: 20 assentos sem venda |
| O8 | 6 compras, `restart airlineshub` | Antes: 6 = 6 = 6. Depois: estoque de volta a 20 (consumidos 0) e `/sales` ainda com 6 | **Não** (estoque × `/sales`) |

## Por cenário

### O1 · Linha de base
**O que foi feito.** 50 compras sequenciais em 5 pares (10 por par), com tudo saudável.
**Resultado.** 50 × 201; p50 0,62 s, p95 0,89 s, máx 0,99 s. A latência é dominada pelo `/sell`
(300 a 900 ms, fixo pelo AirlinesHub). Nos logs (`O1/logs-imdtravel.jsonl`) cada compra tem as
quatro chamadas na ordem `/flight`, `/convert`, `/sell`, `/bonus`, todas com o mesmo `cid`.
**Interpretação.** É o ponto de referência: sem falha, as três contagens batem. Serve de
comparação com a Parte 2.

### O2 · Carga concorrente
**O que foi feito.** 60 compras no mesmo par IMD300/2026-12-01, 25 em paralelo.
**Resultado.** 20 × 201 e 40 × 409, estoque final 0, nenhum 500 e nenhum estoque negativo.
Tempo p95 de 1,26 s. As três contagens batem (20).
**Interpretação.** A atomicidade do estoque é do AirlinesHub, e o IMDTravel não limita a entrada
(spec §4). Sem falha de dependência, a concorrência por si só não cria inconsistência.

### O3 · Fidelity parado
**O que foi feito.** `docker compose stop fidelity`, depois 3 compras em paralelo no mesmo par. Em
seguida, `start` e tentativas de compra a cada 5 s. Por fim, uma segunda fase: com uma conexão já
aberta com o Fidelity (aberta pela compra que deu certo), parar o Fidelity de novo e comprar.
**Resultado.**
- Fase 1 (sem conexão aberta): 3 × 500 em 0,77 a 1,02 s (`ConnectException`, no log). Os 3 assentos
  foram consumidos; `/sales` ficou com 0 linhas.
- Depois do `start`, a primeira tentativa (1 s depois) ainda deu 500 e a segunda (7 s depois) deu 201.
  Cada tentativa que falha também consome um assento. Não confirmamos a causa. A hipótese é um
  cache de endereço ou de conexão do cliente HTTP do JDK, já que o container volta com outro IP.
- Fase 2 (com conexão aberta): o 500 levou **71,6 s**, a mesma ordem de grandeza dos 72 s do
  `testes.sh --com-falhas`. O assento foi consumido e `/sales` não ganhou linha.
- Auditoria final: 6 assentos consumidos, 1 venda. O Fidelity guarda tudo em memória e perde os
  pontos quando é parado e iniciado de novo, então a coluna de bônus ali (0) não é comparável.
**Interpretação.** Falha de parada do Fidelity, com efeito de omissão. O defeito de projeto é o
Fidelity estar no caminho crítico **depois** do passo irreversível (`/sell`): o cliente recebe 500,
mas o assento foi vendido. A inconsistência está entre o AirlinesHub (estoque) e o IMDTravel
(`/sales`). Sem timeout, o tempo do 500 depende do estado da conexão TCP, e pode ir de menos
de 1 s a mais de um minuto.

### O4 · Fidelity congelado
**O que foi feito.** `docker compose pause fidelity` (o processo para, mas a conexão TCP continua
aceita). Compra A com cliente desistindo em 180 s, compra B com limite de 300 s, ambas no mesmo
par. Aos 10 s, conferiu-se que o resto do IMDTravel responde. Depois que A desistiu, `unpause`.
**Resultado.**
- Nenhuma resposta em 180 s: nem 500, nem timeout. O IMDTravel não tem limite, e o log mostra o
  `/bonus` pendurado por 179 426 ms.
- Aos 10 s: `/health` 200 em 2 ms e `/sales` 200 em 59 ms. `seatsAvailable` já estava em 18
  (2 assentos consumidos) com 0 linhas em `/sales`. Isso é a janela irreversível durante a pausa.
- A: o cliente desistiu aos 180 s (`000`). B recebeu 201 em 180,3 s, logo depois do `unpause`.
- Depois do `unpause`, **a compra A também terminou do lado do servidor** (o log tem `resposta.enviada`
  com 201 em 180 210 ms), mesmo sem cliente para receber. Auditoria final: 2 consumidos, 2 vendas, 2 bônus.
**Interpretação.** Falha de congelamento (não responde e não cai), a mais difícil de detectar, e é
pior que a parada. Uma thread do servidor e um assento ficam presos pelo tempo que o Fidelity
demorar. Sem timeout, nada limita isso (cada compra pendurada ocupa uma thread do Tomcat, que tem
200 por padrão). Se o cliente desiste e tenta de novo, pode comprar duas vezes, já que a primeira
continua viva no servidor e termina em 201.

### O5 · Exchange parado
**O que foi feito.** `stop exchange` e 3 compras sequenciais; depois `start` e tentativas de compra.
**Resultado.** 3 × 500 em 0,006 a 0,1 s; 0 assentos consumidos. Depois do `start`, a primeira
tentativa deu 500 e a segunda, 6 s depois, 201.
**Interpretação.** Falha de parada, mas antes do passo irreversível (a ordem é `/flight`,
`/convert`, `/sell`). Resultado limpo: erro visível ao cliente e nenhuma inconsistência.
A ordem das chamadas é o que protege aqui.

### O6 · AirlinesHub parado
**O que foi feito.** `stop airlineshub` e 3 compras sequenciais; depois `start` e tentativas.
**Resultado.** 3 × 500 em 0,003 a 0,07 s, já no `/flight`. Depois do `start`, a primeira tentativa
deu 500 e a segunda (6 s depois), 201. Nada consumido, nada vendido.
**Interpretação.** O AirlinesHub é o ponto único de falha da compra, mas falha cedo e sem
inconsistência. A resposta é imediata porque a conexão é recusada (sem timeout configurado, o
tempo vem do sistema operacional).

### O7 · IMDTravel morto no meio
**O que foi feito.** Rajada de 60 compras (20 em paralelo) em 4 pares; após 2 s,
`docker compose kill imdtravel`; depois `up -d --wait` e auditoria.
**Resultado.** 28 × 201 e 32 clientes com conexão perdida (21 com curl 52, resposta vazia, e 11 com
curl 56, reset). Assentos consumidos: 12 em cada um dos 4 pares (48), com 28 vendas e 28 bônus
(7, 8, 7 e 6 por par). **20 assentos foram vendidos sem linha em `/sales` e sem bônus.**
**Interpretação.** Falha de parada do próprio orquestrador. Não existe nenhum registro de intenção
antes do `/sell`, então o que estava entre o `/sell` e o `/sales` se perde. O cliente vê erro,
o estoque diz que vendeu, e o IMDTravel não sabe de nada. O `/sales` gravado no volume sobreviveu
ao kill (as 28 vendas confirmadas continuaram lá).

### O8 · Restart das dependências
**O que foi feito.** 6 compras em IMD200/2026-12-03, auditoria, `docker compose restart airlineshub`,
nova auditoria.
**Resultado.** Antes: 6 consumidos = 6 vendas = 6 bônus. Depois: `seatsAvailable` voltou a 20
(consumidos 0), enquanto o `/sales` do IMDTravel e o `/points` seguem com 6.
**Interpretação.** O estoque do AirlinesHub é em memória (o catálogo é recriado a cada subida, como
a spec manda). Falha de queda com perda de estado: a fonte da verdade do estoque esquece as vendas,
e o IMDTravel passa a "ter vendido" assentos que o AirlinesHub voltou a oferecer (risco de overbooking).
Nenhuma proteção da Parte 1 detecta isso.

## Resumo para a Parte 2

- O pior caso observado é o **congelamento** do Fidelity (O4): sem timeout, a compra fica presa por
  tempo ilimitado, segurando thread e assento.
- A **parada** do Fidelity (O3) deixa assentos vendidos sem registro. A causa é a posição do
  `/bonus` depois do `/sell`.
- Falhas **antes do `/sell`** (O5, O6) são limpas: 500 sem perda.
- O tempo do 500 vai de milissegundos a 72 s conforme o estado da conexão. Esse é o "timeout
  padrão" que o roteiro queria descobrir: não há timeout, e o que se vê é o do sistema operacional.
- A morte do IMDTravel (O7) perde assentos de forma silenciosa.
