#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose=(docker compose -p crackcs-local -f "$project_root/compose.local.yaml")
case "${1:-}" in
  start)
    "${compose[@]}" up -d --wait
    ;;
  stop)
    "${compose[@]}" stop
    ;;
  backup)
    archive="${2:?Usage: backup /path/to/new.dump}"
    if [[ -e "$archive" ]]; then
      echo 'Refusing to overwrite an existing backup' >&2
      exit 1
    fi
    # noclobber also prevents a race with another backup targeting the same path.
    (set -o noclobber; "${compose[@]}" exec -T postgres pg_dump -U crackcs_local -d crackcs_local --format=custom --no-owner --no-privileges > "$archive")
    "${compose[@]}" exec -T postgres pg_restore --list < "$archive" > /dev/null
    shasum -a 256 "$archive"
    ;;
  restore)
    archive="${2:?Usage: restore /path/to/backup.dump crackcs_restore_unique_name}"
    restore_database="${3:?A NEW restore database name is required}"
    if [[ ! "$restore_database" =~ ^crackcs_restore_[a-z0-9_]+$ || ${#restore_database} -gt 63 ]]; then
      echo 'Restore target must start with crackcs_restore_, contain only a-z/0-9/_, and fit 63 characters' >&2
      exit 1
    fi
    # createdb fails if the target exists: never restore over existing data.
    "${compose[@]}" exec -T postgres pg_restore --list < "$archive" > /dev/null
    "${compose[@]}" exec -T postgres createdb -U crackcs_local "$restore_database"
    "${compose[@]}" exec -T postgres pg_restore -U crackcs_local --dbname="$restore_database" --no-owner --no-privileges --exit-on-error --single-transaction < "$archive"
    echo "Restored to $restore_database; original crackcs_local is unchanged"
    ;;
  *)
    echo 'Usage: bash scripts/local-postgres.sh start|stop|backup <new-file>|restore <file> <new-db>' >&2
    exit 2
    ;;
esac
