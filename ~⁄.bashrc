codex-find() {
    if [ -z "$1" ]; then
      echo "usage: codex-find <keyword>"
      return 1
    fi

    rg -l -i --glob '*.jsonl' "$1" ~/.codex/sessions \
      | while IFS= read -r f; do
          ts=$(stat -c '%y' "$f" | cut -d'.' -f1)
          id=$(basename "$f" .jsonl | grep -oE '[0-9a-f-]{36}$')
          printf '%s  %s  %s\n' "$ts" "$id" "$f"
        done \
      | sort -r \
      | head -n 20
  }
