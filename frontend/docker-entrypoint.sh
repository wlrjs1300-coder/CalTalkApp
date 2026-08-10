#!/bin/sh
set -eu

case "${PORT:-}" in
  ''|*[!0-9]*)
    echo "PORT must be numeric" >&2
    exit 1
    ;;
esac

case "${BACKEND_ORIGIN:-}" in
  http://*|https://*) ;;
  *://*)
    echo "BACKEND_ORIGIN must use http or https" >&2
    exit 1
    ;;
  '')
    echo "BACKEND_ORIGIN is required" >&2
    exit 1
    ;;
  *.onrender.com) BACKEND_ORIGIN="https://${BACKEND_ORIGIN}" ;;
  *) BACKEND_ORIGIN="http://${BACKEND_ORIGIN}" ;;
esac
export BACKEND_ORIGIN

exec /docker-entrypoint.sh "$@"
