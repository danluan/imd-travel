# IMDTravel — Parte 1 (BASELINE)

Projeto da disciplina de Tolerância a Falhas (DIMAp/UFRN, 2026.2). Implementamos o **IMDTravel**,
que orquestra a compra de passagens chamando AirlinesHub, Exchange e Fidelity (imagens do professor).
Na Parte 1 não há tolerância a falhas: se uma dependência falha, a compra devolve `500`.

O enunciado original está em [README-ORIENTAÇÕES.md](README-ORIENTAÇÕES.md) e em `docs/`.

## Equipe

* DANIEL LUAN LOURENCO DE LIMA
* VINICIUS DANTAS RODRIGUES 

## Linguagem e framework

- **Java 21, Spring Boot 3.4 e Maven.** O código fica em `imdtravel/`, organizado em camadas MVC.
- **Persistência:** SQLite em `/data/sales.db`, num volume Docker nomeado.
- **Observabilidade:** log JSON (`ts, svc, cid, event, ms`), `X-Correlation-Id` propagado e `/health` com verificação real.

## Como rodar

```
docker compose up -d --build --wait    # sobe os 4 serviços (só a porta 8080 fica exposta)
./testes.sh                            # verificações do professor
docker compose down -v                 # derruba tudo e apaga as vendas
```

## Testes realizados

| Teste | Resultado |
|---|---|
| Unitários (JUnit) | 44/44 |
| `./testes.sh` | 40/40 |
| `./testes.sh --com-falhas` | 43/43 |
| Roteiro de observação O1–O8 (`./tests/observar.sh`) | Resumo abaixo |

| Cenário | O que observamos |
|---|---|
| O1: 50 compras sequenciais | Assentos, vendas e bônus batem: 50 de cada |
| O2: 60 compras concorrentes no mesmo par | 20 × `201` e 40 × `409`; o estoque não fica negativo |
| O3: Fidelity parado | `500`, mas o assento já foi consumido e a venda não é registrada |
| O4: Fidelity congelado | A compra fica pendurada sem resposta; só conclui quando o Fidelity volta |
| O5 e O6: Exchange ou AirlinesHub parados | `500` imediato e nenhum assento perdido |
| O7: IMDTravel morto durante a carga | 20 assentos consumidos sem venda registrada |
| O8: AirlinesHub reiniciado | O estoque volta a 20, mas o `/sales` mantém as vendas |

Os detalhes estão em [tests/README.md](tests/README.md), as evidências em `tests/evidencias/` e a
revisão contra a spec §4 em [tests/revisao-spec4.md](tests/revisao-spec4.md).

## Catálogo de voos

Os valores vêm do `docker-compose.yml`:

| Item | Valor |
|---|---|
| Voos | IMD100, IMD200, IMD300 |
| Preço base (USD) | 350,50 · 420,50 · 510,50 |
| Acréscimo por dia | 12,25 USD |
| Datas | 2026-12-01 a 2026-12-07 (7 dias) |
| Assentos por voo e dia | 20 |
| Câmbio inicial | 5,50 (varia até ±0,3% a cada 10 s, entre 5,00 e 6,00) |
 