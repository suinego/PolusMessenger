#!/usr/bin/env bash
# =============================================================================
# run_cpu_experiment.sh
# Запускает CpuExperimentTest (Baseline / S1 / S2 / S3) и сохраняет результаты.
#
# Использование: ./run_cpu_experiment.sh
# Запускать из корня проекта (рядом с gradlew).
# =============================================================================

set -euo pipefail

PACKAGE="com.example.polusmessenger"
TEST_CLASS="${PACKAGE}.experiment.CpuExperimentTest"

# Выходные файлы (в корне проекта)
CSV_RESULTS="cpu_results.csv"
CSV_TIMELINE="cpu_timeline.csv"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[CPU]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK] ${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[ERR]${NC} $*"; }

# ── Проверки ─────────────────────────────────────────────────────────────────
if [[ ! -f "./gradlew" ]]; then
    fail "Запускайте из корня проекта (рядом с gradlew)."
    exit 1
fi
if ! adb get-state &>/dev/null; then
    fail "Устройство не найдено."
    exit 1
fi

# ── Сборка и установка ────────────────────────────────────────────────────────
log "Сборка APK..."
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q
ok "Сборка завершена."

log "Установка APK..."
./gradlew :app:installDebug :app:installDebugAndroidTest -q
ok "APK установлены."

# ── Очистка старого Logcat чтобы не смешивались старые данные ────────────────
log "Очистка Logcat..."
adb logcat -c
sleep 1

# ── Запуск теста ──────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════"
log "Запуск CpuExperimentTest (4 стратегии × 4 прогона ≈ 12-15 мин)"
echo "════════════════════════════════════════════════════"
echo ""

START=$(date +%s)

./gradlew :app:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=${TEST_CLASS}" \
    --no-build-cache -q 2>&1 || warn "Тест завершился с ошибкой — всё равно извлекаем данные."

END=$(date +%s)
ok "Тест завершён за $(( (END - START) / 60 )) мин."

# ── Извлечение CPU_RESULT → csv_results.csv ───────────────────────────────────
log "Извлечение результатов из Logcat..."

echo "strategy,run,avgCpuPct,maxCpuPct,sampleCount" > "$CSV_RESULTS"
adb logcat -d -s "CPU_RESULT" 2>/dev/null \
    | grep -oP '(?<=CPU_RESULT: ).*' \
    | grep -v "^strategy," \
    >> "$CSV_RESULTS" || true

ROWS=$(wc -l < "$CSV_RESULTS")
if [[ "$ROWS" -gt 1 ]]; then
    ok "Сводные результаты: $CSV_RESULTS  (${ROWS} строк)"
else
    warn "CPU_RESULT не найден. Возможно тест упал до сохранения."
fi

# ── Извлечение CPU_TIMELINE → cpu_timeline.csv ────────────────────────────────
log "Извлечение временного ряда CPU..."

echo "strategy,run,sampleIdx,cpuPct" > "$CSV_TIMELINE"
adb logcat -d -s "CPU_TIMELINE" 2>/dev/null \
    | grep -oP '(?<=CPU_TIMELINE: ).*' \
    >> "$CSV_TIMELINE" || true

TROWS=$(wc -l < "$CSV_TIMELINE")
if [[ "$TROWS" -gt 1 ]]; then
    ok "Временной ряд: $CSV_TIMELINE  (${TROWS} точек)"
else
    warn "CPU_TIMELINE данных нет."
fi

# ── Итог ─────────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════"
ok "Готово. Файлы в корне проекта:"
echo "  $CSV_RESULTS   — avg/max CPU по прогонам"
echo "  $CSV_TIMELINE  — CPU каждые 500 мс (временной ряд)"
echo ""
echo "  Вставь данные в Colab:"
echo "    cat $CSV_RESULTS"
echo "    cat $CSV_TIMELINE"
echo "════════════════════════════════════════════════════"

# ── Предпросмотр результатов ──────────────────────────────────────────────────
echo ""
echo "── Предпросмотр cpu_results.csv ──"
cat "$CSV_RESULTS"
