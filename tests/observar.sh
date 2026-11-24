#!/usr/bin/env bash
#
# observar.sh — roteiro de observação O1..O8 do IMDTravel (Parte 1, sem tolerância a falhas).
#
#   ./tests/observar.sh          roda os 8 cenários, um depois do outro
#   ./tests/observar.sh O3 O7    roda só os cenários citados
#
# Requer: docker compose, curl, jq. Compatível com bash 3.2 (macOS).
#
# Cada cenário começa de estado limpo (down -v + up --wait; só o primeiro da execução faz
# --build), grava a evidência em tests/evidencias/<cenário>/ e devolve os serviços ao normal.
# O limite de espera (curl -m) existe SÓ aqui, no cliente de observação: o IMDTravel não tem
# timeout nenhum, e é isso que o roteiro quer mostrar.
#
# Variáveis opcionais: BASE (http://localhost:8080), KILL_AFTER (s, O7), LIM_O3, LIM_O4A, LIM_O4B.

BASE="${BASE:-http://localhost:8080}"
export BASE
SELF="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
EVID="$RAIZ/tests/evidencias"
FLIGHTS="IMD100 IMD200 IMD300"
DAYS="2026-12-01 2026-12-02 2026-12-03 2026-12-04 2026-12-05 2026-12-06 2026-12-07"
SEATS=20

# ---------------------------------------------------------------------------
# Modo interno: uma compra. Usado pelo xargs -P para as rajadas.
#   observar.sh --compra <arquivo> <limite_s> <cid> <voo> <dia> <usuario>
# Linha gravada (TSV): cid  inicio_epoch  status  tempo_s  rc_curl  corpo
# ---------------------------------------------------------------------------
if [ "${1:-}" = "--compra" ]; then
    out=$2; lim=$3; cid=$4; voo=$5; dia=$6; usr=$7
    ini=$(date +%s)
    resp=$(curl -s -m "$lim" -H 'Content-Type: application/json' -H "X-Correlation-Id: $cid" \
        -d "{\"flight\":\"$voo\",\"day\":\"$dia\",\"user\":\"$usr\"}" \
        -w '\n%{http_code} %{time_total}' "$BASE/buyTicket")
    rc=$?
    ult=$(printf '%s\n' "$resp" | tail -n1)
    corpo=$(printf '%s\n' "$resp" | sed '$d' | tr '\t\n' '  ')
    st=${ult%% *}; tp=${ult#* }
    [ -n "$st" ] || { st=000; tp=0; }
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$cid" "$ini" "$st" "$tp" "$rc" "$corpo" >> "$out"
    exit 0
fi

cd "$RAIZ" || exit 1
for c in docker curl jq; do
    command -v "$c" >/dev/null 2>&1 || { echo "falta o comando: $c" >&2; exit 1; }
done

dc() { docker compose "$@"; }
# curl por dentro da rede do compose (só o imdtravel publica porta)
dexec() { docker compose exec -T imdtravel curl -sS -m "${T_EXEC:-10}" "$@"; }

reset_estado() {
    echo "== estado limpo: docker compose down -v + up ($1)"
    dc down -v >/dev/null 2>&1
    if [ "$1" = build ]; then dc up -d --build --wait >/dev/null 2>&1; else dc up -d --wait >/dev/null 2>&1; fi
    dc ps --format '{{.Service}} {{.Status}}'
}

normalizar() {
    echo "== devolvendo os serviços ao normal"
    dc unpause fidelity exchange airlineshub imdtravel >/dev/null 2>&1
    dc start fidelity exchange airlineshub imdtravel >/dev/null 2>&1
    dc up -d --wait >/dev/null 2>&1
    dc ps --format '{{.Service}} {{.Status}}'
}

# estat <arquivo.tsv>: contagem por status e tempos
estat() {
    f=$1
    echo "requisições: $(wc -l < "$f" | tr -d ' ')"
    cut -f3 "$f" | sort | uniq -c | awk '{printf "  status %s: %s\n",$2,$1}'
    cut -f4 "$f" | sort -n | awk '{v[NR]=$1} END{ if(NR){i=int(NR*0.95+0.999); if(i<1)i=1;
        printf "  tempo(s): min=%s p50=%s p95=%s max=%s\n", v[1], v[int((NR+1)/2)], v[i], v[NR]}}'
}

# rajada <arquivo.tsv> <limite_s> <paralelismo> <lista>   (lista: "cid voo dia usuario" por linha)
rajada() {
    xargs -P "$3" -L 1 bash "$SELF" --compra "$1" "$2" < "$4"
}

# sequencial <arquivo.tsv> <limite_s> <lista>
sequencial() {
    while read -r cid voo dia usr; do
        bash "$SELF" --compra "$1" "$2" "$cid" "$voo" "$dia" "$usr"
    done < "$3"
}

# auditoria <dir> <prefixo> <pares "voo:dia ...">
# Usuário de cada par = <prefixo>-<voo>-<dia>, para separar os bônus por par.
auditoria() {
    dir=$1; pre=$2; pares=$3
    curl -s -m 20 "$BASE/sales" > "$dir/sales.json" || echo '[]' > "$dir/sales.json"
    {
        echo "auditoria $(date -u +%FT%TZ)  (consumidos = $SEATS - seatsAvailable)"
        printf '%-22s %9s %6s %7s %7s %9s  %s\n' par consumidos sales bonusN pontos esperados situacao
        tc=0; ts=0
        for p in $pares; do
            v=${p%%:*}; d=${p#*:}
            seats=$(dexec "http://airlineshub:8080/flight?flight=$v&day=$d" | jq -r '.seatsAvailable' 2>/dev/null)
            pts=$(dexec "http://fidelity:8080/points?user=$pre-$v-$d" 2>/dev/null)
            pn=$(printf '%s' "$pts" | jq -r '.entries|length' 2>/dev/null)
            pp=$(printf '%s' "$pts" | jq -r '.totalPoints' 2>/dev/null)
            [ -n "$seats" ] || seats='?'; [ -n "$pn" ] || pn='?'; [ -n "$pp" ] || pp='?'
            ns=$(jq --arg f "$v" --arg d "$d" --arg u "$pre-$v-$d" \
                '[.[]|select(.flight==$f and .day==$d and .user==$u)]|length' "$dir/sales.json")
            esp=$(jq --arg f "$v" --arg d "$d" --arg u "$pre-$v-$d" \
                '[.[]|select(.flight==$f and .day==$d and .user==$u)|.valueUSD|floor]|add // 0' "$dir/sales.json")
            if [ "$seats" = '?' ]; then cons='?'; else cons=$((SEATS - seats)); fi
            sit=ok
            [ "$cons" = "$ns" ] && [ "$pn" = "$ns" ] || sit="INCONSISTENTE (assentos perdidos: ${cons}-${ns})"
            [ "$seats" != '?' ] && [ "$seats" -lt 0 ] && sit="ESTOQUE NEGATIVO"
            printf '%-22s %9s %6s %7s %7s %9s  %s\n' "$v $d" "$cons" "$ns" "$pn" "$pp" "$esp" "$sit"
            [ "$cons" != '?' ] && tc=$((tc + cons)); ts=$((ts + ns))
        done
        echo "total: consumidos=$tc sales=$ts  diferença (assentos sem venda registrada)=$((tc - ts))"
    } | tee "$dir/auditoria.txt"
}

salvar_logs() {
    dc logs --no-log-prefix --no-color imdtravel > "$1/logs-imdtravel.jsonl" 2>&1
    dc logs --no-color > "$1/logs-todos.txt" 2>&1
    echo "logs: $(wc -l < "$1/logs-imdtravel.jsonl" | tr -d ' ') linhas do imdtravel em logs-imdtravel.jsonl"
}

# recuperacao <dir> <cid-prefixo> <voo> <dia> <usuario>: depois do start do serviço, tenta uma compra
# a cada 5 s (até 18 vezes) e anota quanto tempo passou até a primeira que dá 201.
recuperacao() {
    d=$1; ini=$(date +%s); n=1
    while [ "$n" -le 18 ]; do
        echo "$2-$n $3 $4 $5" > "$d/entrada-pos.txt"
        sequencial "$d/respostas-pos.tsv" 60 "$d/entrada-pos.txt"
        st=$(tail -n1 "$d/respostas-pos.tsv" | cut -f3)
        echo "  tentativa $n, $(( $(date +%s) - ini ))s depois do start: status $st"
        [ "$st" = 201 ] && { echo "primeiro 201 após $(( $(date +%s) - ini ))s (tentativa $n)"; return; }
        n=$((n + 1)); sleep 5
    done
    echo "nenhum 201 em $(( $(date +%s) - ini ))s"
}

# lista <prefixo> <N> <pares>: gera N linhas "cid voo dia usuario", em rodízio pelos pares
lista() {
    pre=$1; n=$2; set -- $3; np=$#; i=0
    while [ "$i" -lt "$n" ]; do
        k=$(( i % np + 1 )); eval "p=\${$k}"
        v=${p%%:*}; d=${p#*:}
        echo "$pre-$((i + 1)) $v $d $pre-$v-$d"
        i=$((i + 1))
    done
}

# ---------------------------------------------------------------------------
# Cenários
# ---------------------------------------------------------------------------
O1() {
    D=$1
    P="IMD100:2026-12-01 IMD100:2026-12-02 IMD100:2026-12-03 IMD200:2026-12-01 IMD200:2026-12-02"
    lista O1 50 "$P" > "$D/entrada.txt"
    echo "== O1: 50 compras sequenciais em 5 pares (10 por par)"
    sequencial "$D/respostas.tsv" 120 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    auditoria "$D" O1 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O2() {
    D=$1
    P="IMD300:2026-12-01"
    lista O2 60 "$P" > "$D/entrada.txt"
    echo "== O2: 60 compras, 25 em paralelo, mesmo par IMD300/2026-12-01 ($SEATS assentos)"
    rajada "$D/respostas.tsv" 120 25 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    auditoria "$D" O2 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O3() {
    D=$1; LIM=${LIM_O3:-400}
    P="IMD100:2026-12-01"
    lista O3 3 "$P" > "$D/entrada.txt"
    echo "== O3: docker compose stop fidelity; 3 compras em paralelo (cliente espera até ${LIM}s)"
    dc stop fidelity >/dev/null 2>&1
    rajada "$D/respostas.tsv" "$LIM" 3 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    echo "== fidelity volta (start); uma compra depois, para ver a recuperação"
    dc start fidelity >/dev/null 2>&1; dc up -d --wait >/dev/null 2>&1
    echo "recuperação (cada tentativa fracassada também consome um assento):" | tee -a "$D/resumo.txt"
    recuperacao "$D" O3-pos IMD100 2026-12-01 O3-IMD100-2026-12-01 | tee -a "$D/resumo.txt"
    echo "== O3 fase 2: conexão já aberta com o fidelity (keep-alive da compra anterior); para o fidelity de novo e compra"
    echo "O3-quente-1 IMD100 2026-12-01 O3-IMD100-2026-12-01" > "$D/entrada-quente.txt"
    dc stop fidelity >/dev/null 2>&1
    sequencial "$D/respostas-quente.tsv" "$LIM" "$D/entrada-quente.txt"
    echo "fase 2 (fidelity parado com conexão já aberta):" | tee -a "$D/resumo.txt"
    estat "$D/respostas-quente.tsv" | tee -a "$D/resumo.txt"
    dc start fidelity >/dev/null 2>&1; dc up -d --wait >/dev/null 2>&1
    auditoria "$D" O3 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O4() {
    D=$1; LA=${LIM_O4A:-180}; LB=${LIM_O4B:-300}
    P="IMD100:2026-12-02"
    echo "O4-A IMD100 2026-12-02 O4-IMD100-2026-12-02" > "$D/entrada-A.txt"
    echo "O4-B IMD100 2026-12-02 O4-IMD100-2026-12-02" > "$D/entrada-B.txt"
    echo "== O4: docker compose pause fidelity; compra A (cliente desiste em ${LA}s) e B (desiste em ${LB}s)"
    dc pause fidelity >/dev/null 2>&1
    T0=$(date +%s)
    sequencial "$D/respostas-A.tsv" "$LA" "$D/entrada-A.txt" &  PA=$!
    sequencial "$D/respostas-B.tsv" "$LB" "$D/entrada-B.txt" &  PB=$!
    sleep 10
    echo "-- t=10s, fidelity pausado: o imdtravel ainda responde a outras rotas?"
    {
        echo "GET /health: $(curl -s -m 10 -o /dev/null -w '%{http_code} %{time_total}s' "$BASE/health")"
        echo "GET /sales:  $(curl -s -m 10 -o /dev/null -w '%{http_code} %{time_total}s' "$BASE/sales")"
        seats=$(dexec "http://airlineshub:8080/flight?flight=IMD100&day=2026-12-02" | jq -r .seatsAvailable)
        echo "seatsAvailable IMD100/2026-12-02 durante a pausa: $seats (consumidos: $((SEATS - seats)))"
        echo "linhas em /sales durante a pausa: $(curl -s -m 10 "$BASE/sales" | jq length)"
        echo "/points com fidelity pausado: $(T_EXEC=5 dexec 'http://fidelity:8080/points?user=x' >/dev/null 2>&1 && echo respondeu || echo 'sem resposta em 5s')"
    } | tee "$D/durante-pausa.txt"
    wait "$PA"
    echo "-- A desistiu em $(( $(date +%s) - T0 ))s (limite do cliente); a pausa continua. Agora: unpause."
    dc unpause fidelity >/dev/null 2>&1
    TU=$(( $(date +%s) - T0 ))
    echo "unpause em t=${TU}s" | tee "$D/resumo.txt"
    wait "$PB"
    echo "-- B terminou em t=$(( $(date +%s) - T0 ))s"
    sleep 5
    echo "A:" | tee -a "$D/resumo.txt"; estat "$D/respostas-A.tsv" | tee -a "$D/resumo.txt"
    echo "B:" | tee -a "$D/resumo.txt"; estat "$D/respostas-B.tsv" | tee -a "$D/resumo.txt"
    auditoria "$D" O4 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O5() {
    D=$1
    P="IMD100:2026-12-03"
    lista O5 3 "$P" > "$D/entrada.txt"
    echo "== O5: docker compose stop exchange; 3 compras"
    dc stop exchange >/dev/null 2>&1
    sequencial "$D/respostas.tsv" 400 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    cut -f1,3,4,6 "$D/respostas.tsv" | tee -a "$D/resumo.txt"
    dc start exchange >/dev/null 2>&1; dc up -d --wait >/dev/null 2>&1
    echo "recuperação:" | tee -a "$D/resumo.txt"
    recuperacao "$D" O5-pos IMD100 2026-12-03 O5-IMD100-2026-12-03 | tee -a "$D/resumo.txt"
    auditoria "$D" O5 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O6() {
    D=$1
    P="IMD100:2026-12-04"
    lista O6 3 "$P" > "$D/entrada.txt"
    echo "== O6: docker compose stop airlineshub; 3 compras"
    dc stop airlineshub >/dev/null 2>&1
    sequencial "$D/respostas.tsv" 400 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    cut -f1,3,4,6 "$D/respostas.tsv" | tee -a "$D/resumo.txt"
    dc start airlineshub >/dev/null 2>&1; dc up -d --wait >/dev/null 2>&1
    echo "recuperação:" | tee -a "$D/resumo.txt"
    recuperacao "$D" O6-pos IMD100 2026-12-04 O6-IMD100-2026-12-04 | tee -a "$D/resumo.txt"
    auditoria "$D" O6 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O7() {
    D=$1; KA=${KILL_AFTER:-2}
    P="IMD100:2026-12-05 IMD200:2026-12-05 IMD300:2026-12-05 IMD100:2026-12-06"
    lista O7 60 "$P" > "$D/entrada.txt"
    echo "== O7: rajada de 60 compras (20 em paralelo); docker compose kill imdtravel após ${KA}s"
    rajada "$D/respostas.tsv" 60 20 "$D/entrada.txt" &  PR=$!
    sleep "$KA"
    dc kill imdtravel >/dev/null 2>&1
    echo "kill em $(date -u +%T)" | tee "$D/resumo.txt"
    wait "$PR"
    echo "rajada terminou; clientes (status 000 = conexão perdida ou recusada):" | tee -a "$D/resumo.txt"
    estat "$D/respostas.tsv" | tee -a "$D/resumo.txt"
    cut -f5 "$D/respostas.tsv" | sort | uniq -c | awk '{printf "  rc do curl %s: %s\n",$2,$1}' | tee -a "$D/resumo.txt"
    echo "== subindo o imdtravel de novo (up -d --wait)"
    dc up -d --wait >/dev/null 2>&1
    dc ps --format '{{.Service}} {{.Status}}'
    auditoria "$D" O7 "$P" | tee -a "$D/resumo.txt"
    salvar_logs "$D"
}

O8() {
    D=$1
    P="IMD200:2026-12-03"
    lista O8 6 "$P" > "$D/entrada.txt"
    echo "== O8: 6 compras; depois docker compose restart airlineshub"
    sequencial "$D/respostas.tsv" 120 "$D/entrada.txt"
    estat "$D/respostas.tsv" | tee "$D/resumo.txt"
    echo "-- antes do restart:" | tee -a "$D/resumo.txt"
    auditoria "$D" O8 "$P" | tee -a "$D/resumo.txt"
    cp "$D/auditoria.txt" "$D/auditoria-antes.txt"
    dc restart airlineshub >/dev/null 2>&1; dc up -d --wait >/dev/null 2>&1
    echo "-- depois do restart do airlineshub:" | tee -a "$D/resumo.txt"
    auditoria "$D" O8 "$P" | tee -a "$D/resumo.txt"
    cp "$D/auditoria.txt" "$D/auditoria-depois.txt"
    salvar_logs "$D"
}

# ---------------------------------------------------------------------------
# Principal
# ---------------------------------------------------------------------------
ESC="$*"
[ -n "$ESC" ] || ESC="O1 O2 O3 O4 O5 O6 O7 O8"
for c in $ESC; do
    case "$c" in O[1-8]) ;; *) echo "cenário inválido: $c (use O1..O8)" >&2; exit 1 ;; esac
done

modo=build
for c in $ESC; do
    D="$EVID/$c"
    rm -rf "$D"; mkdir -p "$D"
    {
        echo "##### $c  $(date -u +%FT%TZ)"
        reset_estado "$modo"
    } 2>&1 | tee "$D/terminal.txt"
    modo=sembuild
    "$c" "$D" 2>&1 | tee -a "$D/terminal.txt"
    normalizar 2>&1 | tee -a "$D/terminal.txt"
    echo "##### $c terminou $(date -u +%FT%TZ)" | tee -a "$D/terminal.txt"
done
