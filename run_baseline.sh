#!/usr/bin/env bash
# =============================================================================
# run_baseline.sh
# Прогоняет все 10 сценариев в режиме Baseline (библиотека полностью остановлена).
# Результаты сохраняются в baseline_results.csv (формат тот же, что experiment_results.csv).
#
# Использование: ./run_baseline.sh
# Запускать из корня проекта (рядом с gradlew).
# =============================================================================

set -euo pipefail

PACKAGE="com.example.polusmessenger"
BASE_CLASS="${PACKAGE}.experiment.scenarios"
CSV_ON_DEVICE="/sdcard/Android/data/${PACKAGE}/files/experiment_results.csv"
CSV_LOCAL="baseline_results.csv"

SCENARIOS=(
    "SC01_FastScrollChatList"
    "SC02_SlowScrollChatList"
    "SC03_TapChatItems"
    "SC04_TypeInSearch"
    "SC05_SendMessages"
    "SC06_NavigateTabs"
    "SC07_RapidTaps"
    "SC08_LongPressOnList"
    "SC09_MixedSession"
    "SC10_TypeAndDeleteInChat"
)

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[BASE]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]  ${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[ERR] ${NC} $*"; }

# ── Проверки ─────────────────────────────────────────────────────────────────
if [[ ! -f "./gradlew" ]]; then
    fail "Запускайте из корня проекта (рядом с gradlew)."
    exit 1
fi
if ! adb get-state &>/dev/null; then
    fail "Устройство не найдено. Подключите устройство или запустите эмулятор."
    exit 1
fi

# ── Очистка предыдущего CSV на устройстве ────────────────────────────────────
log "Очистка предыдущих результатов на устройстве..."
adb shell "rm -f ${CSV_ON_DEVICE}" 2>/dev/null || true

# ── Сборка и установка (один раз) ────────────────────────────────────────────
log "Сборка debug + test APK..."
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q
ok "Сборка завершена."

log "Установка APK на устройство..."
./gradlew :app:installDebug :app:installDebugAndroidTest -q
ok "APK установлены."

# ── Очистка Logcat ────────────────────────────────────────────────────────────
log "Очистка Logcat..."
adb logcat -c
sleep 1

# ── Прогон сценариев ──────────────────────────────────────────────────────────
TOTAL=${#SCENARIOS[@]}
PASSED=0
FAILED=()
START_ALL=$(date +%s)

echo ""
echo "════════════════════════════════════════════════════"
log "Baseline: ${TOTAL} сценариев × 5 прогонов (библиотека выключена)"
echo "════════════════════════════════════════════════════"
echo ""

for i in "${!SCENARIOS[@]}"; do
    SCENARIO="${SCENARIOS[$i]}"
    NUM=$((i + 1))
    START_SC=$(date +%s)

    echo "────────────────────────────────────────────────────"
    log "[${NUM}/${TOTAL}] ${SCENARIO}"
    echo "────────────────────────────────────────────────────"

    if ./gradlew :app:connectedDebugAndroidTest \
        "-Pandroid.testInstrumentationRunnerArguments.class=${BASE_CLASS}.${SCENARIO}" \
        "-Pandroid.testInstrumentationRunnerArguments.strategies=Baseline" \
        --no-build-cache -q 2>&1; then

        END_SC=$(date +%s)
        ok "${SCENARIO} — $(( END_SC - START_SC ))с"
        PASSED=$((PASSED + 1))
    else
        END_SC=$(date +%s)
        warn "${SCENARIO} завершился с ошибкой ($(( END_SC - START_SC ))с) — продолжаем..."
        FAILED+=("${SCENARIO}")
    fi

    if [[ "$NUM" -lt "$TOTAL" ]]; then
        log "Пауза 5с перед следующим сценарием..."
        sleep 5
    fi
    echo ""
done

# ── Выгрузка CSV ──────────────────────────────────────────────────────────────
END_ALL=$(date +%s)
TOTAL_MIN=$(( (END_ALL - START_ALL) / 60 ))

echo "════════════════════════════════════════════════════"
log "Итог: ${PASSED}/${TOTAL} сценариев (${TOTAL_MIN} мин)"
if [[ ${#FAILED[@]} -gt 0 ]]; then
    warn "Не прошли: ${FAILED[*]}"
fi

log "Выгрузка CSV с устройства..."
PULLED=false
for DEVICE_PATH in \
    "/sdcard/experiment_results.csv" \
    "${CSV_ON_DEVICE}" \
    "/sdcard/Download/experiment_results.csv"; do

    if adb pull "${DEVICE_PATH}" "${CSV_LOCAL}" 2>/dev/null; then
        ROWS=$(wc -l < "${CSV_LOCAL}" || echo "?")
        ok "Сохранено: ${CSV_LOCAL}  (${ROWS} строк)"
        ok "Путь на устройстве: ${DEVICE_PATH}"
        PULLED=true
        break
    fi
done

# Резервный вариант — Logcat
if [[ "$PULLED" == false ]]; then
    warn "Файл не найден — извлекаем из Logcat..."
    echo "strategy,scenario,run,avgFrameMs,p95FrameMs,jankCount,totalFrames,dataKB,captureCount,durationMs" > "${CSV_LOCAL}"
    adb logcat -d -s "EXP_CSV" 2>/dev/null \
        | grep -oP '(?<=EXP_CSV: ).*' \
        | grep -v "^strategy," \
        >> "${CSV_LOCAL}" || true

    ROWS=$(wc -l < "${CSV_LOCAL}" || echo "0")
    if [[ "$ROWS" -gt 1 ]]; then
        ok "Извлечено из Logcat: ${CSV_LOCAL}  (${ROWS} строк)"
    else
        fail "Данных нет ни в файле, ни в Logcat. Возможно, все тесты упали."
        echo "  Проверь Logcat вручную:"
        echo "    adb logcat -d -s EXP_CSV"
    fi
fi

echo ""
echo "  Предпросмотр:"
head -5 "${CSV_LOCAL}" 2>/dev/null || true
echo "════════════════════════════════════════════════════"
