#!/usr/bin/env bash
# Launches the installed debug app on a running emulator, walks through the
# main screens, and saves screenshots. Fails if the app crashes or the
# catalog never loads.
set -euo pipefail

PACKAGE=com.ppeternnguyen.myamiibo
OUT=${1:-screenshots}
mkdir -p "$OUT"

dump() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
  adb exec-out cat /sdcard/ui.xml
}

# Prints a small JPEG of a screenshot into the log, so it can be viewed even
# where artifacts can't be downloaded.
print_image() {
  local file=$1
  if command -v convert > /dev/null; then
    echo "BEGIN-IMAGE $(basename "$file")"
    convert "$file" -resize 360x -quality 70 jpg:- | base64 -w 0
    echo
    echo "END-IMAGE"
  fi
}

print_diagnostics() {
  echo "--- Visible text ---"
  dump | python3 -c '
import sys, xml.etree.ElementTree as ET
try:
    root = ET.fromstring(sys.stdin.read())
except ET.ParseError as error:
    print("Could not parse UI dump:", error)
    sys.exit()
for node in root.iter("node"):
    label = node.get("text") or node.get("content-desc")
    if label:
        print(repr(label), node.get("bounds"))
' || true
  echo "--- Recent app log ---"
  adb logcat -d -v brief | grep -iE "myamiibo|okhttp|AndroidRuntime|System.err" | tail -60 || true
}

# Prints the center "x y" of the first node whose attribute matches the regex.
center_of() {
  local attr=$1 pattern=$2
  dump | python3 -c '
import re, sys, xml.etree.ElementTree as ET
attr, pattern = sys.argv[1], sys.argv[2]
for node in ET.fromstring(sys.stdin.read()).iter("node"):
    if re.search(pattern, node.get(attr, "")):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$attr" "$pattern"
}

wait_for() {
  local attr=$1 pattern=$2 tries=${3:-30}
  for _ in $(seq "$tries"); do
    if [ -n "$(center_of "$attr" "$pattern")" ]; then return 0; fi
    sleep 2
  done
  echo "Timed out waiting for $attr ~ /$pattern/" >&2
  dump > "$OUT/last-ui.xml" || true
  adb exec-out screencap -p > "$OUT/failure.png" || true
  print_diagnostics
  print_image "$OUT/failure.png"
  return 1
}

tap() {
  local point
  point=$(center_of "$1" "$2")
  [ -n "$point" ] || { echo "Nothing to tap for $1 ~ /$2/" >&2; return 1; }
  adb shell input tap $point
}

shot() {
  sleep "${2:-2}"
  adb exec-out screencap -p > "$OUT/$1.png"
  print_image "$OUT/$1.png"
}

assert_running() {
  adb shell pidof "$PACKAGE" > /dev/null || { echo "App is not running (crashed?)" >&2; adb logcat -d -s AndroidRuntime:E > "$OUT/crash.txt"; return 1; }
}

adb logcat -c
adb shell am start -W -n "$PACKAGE/.MainActivity"

# Browse: the catalog loads from AmiiboAPI and shows a result count.
wait_for text '^[0-9]+ results?$' 60
shot 1-browse
assert_running

# Detail: mark the first amiibo as owned, then open it by tapping its image.
name=$(dump | python3 -c '
import re, sys, xml.etree.ElementTree as ET
for node in ET.fromstring(sys.stdin.read()).iter("node"):
    match = re.fullmatch(r"Mark (.+) as owned", node.get("content-desc", ""))
    if match:
        print(match.group(1))
        break
')
[ -n "$name" ] || { echo "No amiibo card found" >&2; exit 1; }
echo "First amiibo: $name"
tap content-desc "^Mark .* as owned$"
sleep 1
tap content-desc "^$(python3 -c 'import re, sys; print(re.escape(sys.argv[1]))' "$name")"
wait_for text '^Release dates$' 20
shot 2-detail
# Game usage is further down the page; scroll until it shows.
for _ in $(seq 6); do
  if [ -n "$(center_of text '^(Works with [0-9]+ games?|AmiiboAPI doesn.t list any game features for this amiibo\.)$')" ]; then break; fi
  adb shell input swipe 540 1800 540 700 300
  sleep 2
done
wait_for text '^(Works with [0-9]+ games?|AmiiboAPI doesn.t list any game features for this amiibo\.)$' 10
adb shell input swipe 540 1600 540 900 300
shot 3-usage
assert_running

# Collection: the amiibo marked as owned shows up with stats.
adb shell input keyevent KEYCODE_BACK
sleep 1
tap text '^Collection$'
wait_for text '^Owned \(1\)$' 15
shot 4-collection
assert_running

# Dark mode
adb shell cmd uimode night yes
sleep 3
tap text '^Browse$'
wait_for text '^[0-9]+ results?$' 15
shot 5-browse-dark
assert_running
adb shell cmd uimode night no

echo "Smoke test passed. Screenshots in $OUT"
