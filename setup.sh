#!/bin/bash
# 공통 ADR 을 프로젝트에 깐다. 프로젝트 루트에서 실행한다: bash /path/to/ClaudeHarnessKit/setup.sh
#   docs/adr/        ADR 전체와 예시(이미 있으면 건드리지 않는다)
#   CLAUDE.md        없을 때만 템플릿을 둔다. 세션이 ADR-001 부터 읽게 하는 자리다
set -e

KIT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(pwd)"
ADR_TARGET="$PROJECT_ROOT/docs/adr"
CLAUDE_MD="$PROJECT_ROOT/CLAUDE.md"

echo "프로젝트 루트: $PROJECT_ROOT"
read -p "여기에 공통 ADR 을 깔까요? (y/N): " confirm
[[ "$confirm" == "y" || "$confirm" == "Y" ]] || { echo "취소했습니다."; exit 0; }

if [ -d "$ADR_TARGET" ]; then
    echo "  → docs/adr/ 가 이미 있어 건너뜁니다. 새 판과 맞추려면 직접 비교해 고치세요."
else
    mkdir -p "$PROJECT_ROOT/docs"
    cp -r "$KIT_DIR/templates/docs/adr" "$ADR_TARGET"
    echo "  ✅ docs/adr/"
fi

if [ -f "$CLAUDE_MD" ]; then
    echo "  → CLAUDE.md 가 이미 있어 건너뜁니다. templates/CLAUDE.md.template 의 「설계 결정(ADR)」 절을 옮겨 넣으세요."
else
    cp "$KIT_DIR/templates/CLAUDE.md.template" "$CLAUDE_MD"
    echo "  ✅ CLAUDE.md (템플릿 - 프로젝트에 맞게 채우세요)"
fi
