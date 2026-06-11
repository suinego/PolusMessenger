#!/usr/bin/env bash
# =============================================================================
# run_experiments.sh
# Запуск всех 10 экспериментальных сценариев по порядку.
# Использование: ./run_experiments.sh
# Запускать из корня проекта (рядом с gradlew).
# =============================================================================

set -euo pipefail

# ── Настройки ────────────────────────────────────────────────────────────────
PACKAGE="com.example.polusmessenger"
BASE_CLASS="${PACKAGE}.experiment.scenarios"
CSV_ON_DEVICE="/sdcard/Android/data/${PACKAGE}/files/experiment_results.csv"
CSV_LOCAL="experiment_results.csv

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

# ── Цвета для вывода ─────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[EXP]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK] ${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[ERR]${NC} $*"; }

# ── Проверки перед запуском ───────────────────────────────────────────────────
if [[ ! -f "./gradlew" ]]; then
    fail "Запускайте скрипт из корня проекта (рядом с gradlew)."
    exit 1
fi

if ! adb get-state &>/dev/null; then
    fail "Устройство не найдено. Подключите устройство или запустите эмулятор."
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
if [[ "$DEVICE_COUNT" -gt 1 ]]; then
    warn "Подключено несколько устройств. Используется первое по списку."
fi

# ── Шаг 1: Очистка старого CSV ───────────────────────────────────────────────
log "Очистка предыдущих результатов на устройстве..."
adb shell "rm -f ${CSV_ON_DEVICE}" 2>/dev/null || \
warn "Не удалось очистить CSV — возможно, файл ещё не существует."

# ── Шаг 2: Сборка APK (один раз) ─────────────────────────────────────────────
log "Сборка debug + test APK..."
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q
ok "Сборка завершена."

# ── Шаг 3: Установка APK ─────────────────────────────────────────────────────
log "Установка APK на устройство..."
./gradlew :app:installDebug :app:installDebugAndroidTest -q
ok "APK установлены."

# ── Шаг 4: Прогон сценариев ──────────────────────────────────────────────────
TOTAL=${#SCENARIOS[@]}
PASSED=0
FAILED=()
START_ALL=$(date +%s)

echo ""
log "Начинаем эксперимент: ${TOTAL} сценариев × 3 стратегии × 5 прогонов"
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
        --no-build-cache -q 2>&1; then

        END_SC=$(date +%s)
        ELAPSED=$(( END_SC - START_SC ))
        ok "${SCENARIO} — ${ELAPSED}с"
        PASSED=$((PASSED + 1))
    else
        END_SC=$(date +%s)
        ELAPSED=$(( END_SC - START_SC ))
        warn "${SCENARIO} завершился с ошибкой (${ELAPSED}с) — продолжаем..."
        FAILED+=("${SCENARIO}")
    fi

    # Пауза между сценариями, чтобы устройство остыло
    if [[ "$NUM" -lt "$TOTAL" ]]; then
        log "Пауза 5с перед следующим сценарием..."
        sleep 5
    fi
    echo ""
done

# ── Шаг 5: Выгрузка CSV ──────────────────────────────────────────────────────
END_ALL=$(date +%s)
TOTAL_MIN=$(( (END_ALL - START_ALL) / 60 ))

echo "════════════════════════════════════════════════════"
log "Итог: ${PASSED}/${TOTAL} сценариев прошли успешно (${TOTAL_MIN} мин)"

if [[ ${#FAILED[@]} -gt 0 ]]; then
    warn "Не прошли: ${FAILED[*]}"
fi

log "Выгрузка CSV с устройства..."

# Пробуем все возможные пути на устройстве
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

# Резервный вариант — извлечь из Logcat
if [[ "$PULLED" == false ]]; then
    warn "Файл не найден — извлекаем из Logcat..."
    echo "${HEADER_LINE:-"strategy,scenario,run,avgFrameMs,p95FrameMs,jankCount,totalFrames,dataKB,captureCount,durationMs"}" > "${CSV_LOCAL}"
    adb logcat -d -s "EXP_CSV" 2>/dev/null \
        | grep -oP '(?<=EXP_CSV: ).*' \
        | grep -v "^strategy," \
        >> "${CSV_LOCAL}" || true

    ROWS=$(wc -l < "${CSV_LOCAL}" || echo "0")
    if [[ "$ROWS" -gt 1 ]]; then
        ok "Извлечено из Logcat: ${CSV_LOCAL}  (${ROWS} строк)"
    else
        fail "Данных нет ни в файле, ни в Logcat. Возможно, все тесты упали до сохранения."
        echo "  Проверьте Logcat вручную:"
        echo "    adb logcat -d -s EXP_CSV"
    fi
fi

echo ""
echo "  Структура CSV:"
head -2 "${CSV_LOCAL}" 2>/dev/null || true
echo "════════════════════════════════════════════════════"
