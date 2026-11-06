#!/usr/bin/env bash
# Hwan'Story device-flow sign-in for AI agents and scripts. The token is written to a 0600 file and never printed.
#
#   bash login.sh start ["client name"] ["posts social"]   ask for a code; prints the approval URL + code, then exits
#   bash login.sh wait                                     wait for the approval (up to the code's lifetime), save the token
#   bash login.sh status                                   show who the saved token belongs to (GET /api/v1/tokens/current)
#   bash login.sh logout                                   revoke the saved token on the server and delete it
#
# Files in ${HWANSTORY_CONFIG:-$HOME/.config/hwanstory}:
#   auth-header   "Authorization: Bearer hsp_..."  → use with: curl -H @"$HOME/.config/hwanstory/auth-header" ...
#   device        the pending device_code (deleted after wait)
# Server: ${HWANSTORY_API_URL:-{{API_URL}}}
set -eu
API="${HWANSTORY_API_URL:-{{API_URL}}}"
DIR="${HWANSTORY_CONFIG:-$HOME/.config/hwanstory}"
HEADER="$DIR/auth-header"
PENDING="$DIR/device"
SELF="$0"
mkdir -p "$DIR"
chmod 700 "$DIR" 2>/dev/null || true

# value of a top-level field in the server's flat, single-line JSON
field() { sed -nE 's/.*"'"$1"'"[[:space:]]*:[[:space:]]*"?([^",}]*)"?.*/\1/p' | head -1; }

cmd_start() {
  name="${1:-AI agent on $(hostname 2>/dev/null || echo unknown)}"
  scope="${2:-posts social}"
  # the name goes through stdin: on Windows, native curl.exe gets argv in the ANSI code page and would mangle non-ASCII names
  r=$(printf '%s' "$name" | curl -fsS -X POST "$API/api/v1/authentication/device/code" --data-urlencode "client_name@-" --data-urlencode "scope=$scope")
  dc=$(printf %s "$r" | field device_code)
  uc=$(printf %s "$r" | field user_code)
  url=$(printf %s "$r" | field verification_uri_complete)
  exp=$(printf %s "$r" | field expires_in)
  [ -n "$dc" ] && [ -n "$uc" ] || { echo "unexpected response from $API" >&2; exit 1; }
  (umask 077; printf '%s\n%s\n' "$dc" "$(( $(date +%s) + ${exp:-600} ))" > "$PENDING")
  echo "Ask the user to open:  $url"
  echo "and approve the code:  $uc   (valid for $(( ${exp:-600} / 60 )) minutes)"
  echo "Then run:              bash \"$SELF\" wait"
}

cmd_wait() {
  [ -s "$PENDING" ] || { echo "no pending sign-in - run: bash \"$SELF\" start" >&2; exit 1; }
  dc=$(sed -n 1p "$PENDING")
  deadline=$(sed -n 2p "$PENDING")
  interval=5
  while [ "$(date +%s)" -lt "$deadline" ]; do
    r=$(curl -sS -X POST "$API/api/v1/authentication/device/token" --data-urlencode "device_code=$dc" 2>/dev/null) || { sleep "$interval"; continue; }
    case "$r" in
      *'"access_token"'*)
        tok=$(printf %s "$r" | field access_token)
        (umask 077; printf 'Authorization: Bearer %s\n' "$tok" > "$HEADER")
        chmod 600 "$HEADER" 2>/dev/null || true
        rm -f "$PENDING"
        echo "Signed in. Token ${tok:0:21}... saved to $HEADER"
        echo "  scope: $(printf %s "$r" | field scope), expires: $(printf %s "$r" | field expires_at)"
        echo "  use:   curl -H @\"$HEADER\" $API/api/v1/tokens/current"
        return 0 ;;
      *authorization_pending*) ;;
      *slow_down*) interval=$((interval + 5)) ;;
      *)
        echo "sign-in failed: $(printf %s "$r" | field error) - $(printf %s "$r" | field error_description)" >&2
        rm -f "$PENDING"
        exit 1 ;;
    esac
    sleep "$interval"
  done
  rm -f "$PENDING"
  echo "the code expired before it was approved - run: bash \"$SELF\" start" >&2
  exit 1
}

cmd_status() {
  [ -s "$HEADER" ] || { echo "not signed in" >&2; exit 1; }
  curl -fsS -H @"$HEADER" "$API/api/v1/tokens/current"
  echo
}

cmd_logout() {
  if [ -s "$HEADER" ]; then
    curl -sS -o /dev/null -w "revoke -> %{http_code}\n" -X DELETE -H @"$HEADER" "$API/api/v1/tokens/current" || true
    rm -f "$HEADER"
  fi
  echo "signed out"
}

case "${1:-start}" in
  start)  shift || true; cmd_start "$@" ;;
  wait)   cmd_wait ;;
  status) cmd_status ;;
  logout) cmd_logout ;;
  *) sed -n '2,12p' "$SELF"; exit 2 ;;
esac
