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

BACKEND_AUTHORITY="${BACKEND_ORIGIN#*://}"
BACKEND_AUTHORITY="${BACKEND_AUTHORITY%%/*}"
BACKEND_TLS_NAME="${BACKEND_AUTHORITY%%:*}"
NGINX_RESOLVER="$(awk '/^nameserver[[:space:]]+/ { print $2; exit }' /etc/resolv.conf)"

if [ -z "${NGINX_RESOLVER}" ]; then
  echo "Could not determine the container DNS resolver" >&2
  exit 1
fi

NGINX_ENVSUBST_FILTER='^(PORT|BACKEND_ORIGIN|BACKEND_AUTHORITY|BACKEND_TLS_NAME|NGINX_RESOLVER)$'

export BACKEND_ORIGIN BACKEND_AUTHORITY BACKEND_TLS_NAME NGINX_RESOLVER NGINX_ENVSUBST_FILTER

exec /docker-entrypoint.sh "$@"
