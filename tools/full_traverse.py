#!/usr/bin/env python3
"""Full functional traversal test for the in-app keyboard demo (ADB + UIAutomator).

Background:
Some devices/ROMs do NOT expose custom keyboard Button children in `uiautomator dump`.
So this script primarily uses a **grid-tap** strategy inside the keyboard container bounds,
then validates by checking the target EditText text value changes.

What it checks (best-effort):
- Tabs (Text/Number/Password/Phone) are clickable.
- When a field is active, tapping inside the keyboard container should insert characters.
- Number / Phone: inserted chars should include digits.
- Password: tapping should change the (masked) text value (or at least not crash).

Usage:
  cd repo && python3 tools/full_traverse.py

Env overrides:
  ADB, SERIAL, PKG, ACT, UI_DUMP
"""

import os
import re
import time
import subprocess
import xml.etree.ElementTree as ET

def _default_adb() -> str:
    # Prefer explicit env override.
    adb = os.environ.get("ADB")
    if adb:
        return adb

    # If ANDROID_HOME is set.
    android_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if android_home:
        cand = os.path.join(android_home, "platform-tools", "adb")
        if os.path.exists(cand):
            return cand

    # Workspace default (common in this repo environment).
    cand = "/home/ubuntu/clawd/android-sdk/platform-tools/adb"
    if os.path.exists(cand):
        return cand

    # Fallback to PATH.
    return "adb"


ADB = _default_adb()
SERIAL = os.environ.get("SERIAL")  # optional: specific device id for ADB

PKG = os.environ.get("PKG", "com.carbit.inappkeyboard")
ACT = os.environ.get("ACT", "com.carbit.inappkeyboard/.MainActivity")
DUMP_PATH = os.environ.get("UI_DUMP", "/sdcard/window_dump.xml")

BTN_TEXT = f"{PKG}:id/btn_text"
BTN_NUMBER = f"{PKG}:id/btn_number"
BTN_PASSWORD = f"{PKG}:id/btn_password"
BTN_PHONE = f"{PKG}:id/btn_phone"

ET_TEXT = f"{PKG}:id/et_text"
ET_NUMBER = f"{PKG}:id/et_number"
ET_PASSWORD = f"{PKG}:id/et_password"
ET_PHONE = f"{PKG}:id/et_phone"

KBD_CONTAINER = f"{PKG}:id/main_keyboard_container"


def adb_devices() -> list[str]:
    out = subprocess.check_output([ADB, "devices"], timeout=15).decode("utf-8", "ignore")
    devs: list[str] = []
    for line in out.splitlines():
        line = line.strip()
        if not line or line.startswith("List of devices"):
            continue
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devs.append(parts[0])
    return devs


def _resolved_serial() -> str | None:
    if SERIAL:
        return SERIAL
    devs = adb_devices()
    if len(devs) == 1:
        return devs[0]
    return None


def _adb_base_cmd():
    cmd = [ADB]
    serial = _resolved_serial()
    if serial:
        cmd += ["-s", serial]
    return cmd


def sh(*args, timeout=30) -> str:
    return subprocess.check_output([*_adb_base_cmd(), "shell", *args], timeout=timeout).decode("utf-8", "ignore")


def run(*args, timeout=30):
    subprocess.run([*_adb_base_cmd(), "shell", *args], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout, check=False)


def dump(tag: str) -> str:
    # uiautomator dump can be flaky; retry quickly.
    for _ in range(6):
        run("uiautomator", "dump", DUMP_PATH, timeout=30)
        time.sleep(0.06)
        try:
            xml = sh("cat", DUMP_PATH, timeout=30)
            if "<hierarchy" in xml:
                return xml
        except Exception:
            time.sleep(0.06)
    return ""


def parse(xml: str):
    return ET.fromstring(xml)


def center(bounds: str):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2, (x1, y1, x2, y2)


def find_by_res(root, res_id: str):
    for n in root.iter("node"):
        rid = n.attrib.get("resource-id")
        if rid and rid.strip() == res_id:
            return n
    return None


def tap(x: int, y: int):
    run("input", "tap", str(x), str(y), timeout=30)


def _screen_size(root):
    # hierarchy -> first node has bounds like [0,0][1080,2400]
    n = next(root.iter("node"), None)
    if not n:
        return 1080, 2400
    b = n.attrib.get("bounds") or ""
    c = center(b)
    if not c:
        return 1080, 2400
    x1, y1, x2, y2 = c[2]
    return max(1, x2 - x1), max(1, y2 - y1)


def _fallback_tap_tab(res_id: str, root) -> bool:
    # Some ROMs intermittently omit nodes from dump; fall back to tapping known tab locations.
    w, h = _screen_size(root)
    y = int(h * 0.14)  # near the tab row
    x_map = {
        BTN_TEXT: 0.14,
        BTN_NUMBER: 0.34,
        BTN_PASSWORD: 0.54,
        BTN_PHONE: 0.74,
    }
    frac = x_map.get(res_id)
    if frac is None:
        return False
    x = int(w * frac)
    tap(x, y)
    return True


def tap_res(res_id: str, tries: int = 18, delay_s: float = 0.25) -> bool:
    for _ in range(tries):
        xml = dump("tap_res")
        if not xml:
            time.sleep(delay_s)
            continue
        root = parse(xml)
        n = find_by_res(root, res_id)
        if not n:
            # Fallback for tabs
            if res_id.startswith(f"{PKG}:id/btn_"):
                if _fallback_tap_tab(res_id, root):
                    time.sleep(delay_s)
                    return True
            time.sleep(delay_s)
            continue
        c = center(n.attrib.get("bounds") or "")
        if not c:
            time.sleep(delay_s)
            continue
        x, y = c[0], c[1]
        tap(x, y)
        time.sleep(delay_s)
        return True
    return False


def get_text(res_id: str) -> str:
    xml = dump("get_text")
    if not xml:
        return ""
    root = parse(xml)
    n = find_by_res(root, res_id)
    return (n.attrib.get("text") or "") if n else ""


def get_bounds(res_id: str):
    xml = dump("bounds")
    if not xml:
        return None
    root = parse(xml)
    n = find_by_res(root, res_id)
    if not n:
        return None
    c = center(n.attrib.get("bounds") or "")
    return c[2] if c else None


def grid_points(bounds, rows: int, cols: int, pad: int = 12):
    x1, y1, x2, y2 = bounds
    x1 += pad
    y1 += pad
    x2 -= pad
    y2 -= pad
    if x2 <= x1 or y2 <= y1:
        return []
    pts = []
    for r in range(rows):
        for c in range(cols):
            x = int(x1 + (x2 - x1) * (c + 0.5) / cols)
            y = int(y1 + (y2 - y1) * (r + 0.5) / rows)
            pts.append((x, y))
    return pts


INPUT_CONTAINER = f"{PKG}:id/input_container"


def test_field(name: str, tab_res: str, et_res: str, expect_digits: bool = False):
    anoms = []

    if not tap_res(tab_res):
        anoms.append((name, "tab", "failed to click tab"))
        return anoms

    # give UI a moment to swap the visible EditText
    time.sleep(0.25)

    if not tap_res(et_res):
        # fallback: tap the input container center
        ic = get_bounds(INPUT_CONTAINER)
        if ic:
            x1, y1, x2, y2 = ic
            tap((x1 + x2) // 2, (y1 + y2) // 2)
            time.sleep(0.25)
        # re-check: if still cannot read text node, mark fail
        txt = get_text(et_res)
        if txt == "":
            anoms.append((name, "focus", "failed to click EditText"))
            return anoms

    kb = get_bounds(KBD_CONTAINER)
    if not kb:
        anoms.append((name, "keyboard", "missing keyboard container"))
        return anoms

    before = get_text(et_res)

    # Sample a small grid; we only need to see some input happen.
    pts = grid_points(kb, rows=5, cols=10)
    inserted = ""
    last = before

    for i, (x, y) in enumerate(pts[:30]):
        tap(x, y)
        time.sleep(0.08)
        cur = get_text(et_res)
        if cur != last:
            # Best-effort: assume append; record delta.
            if cur.startswith(last):
                inserted += cur[len(last):]
            last = cur

    if last == before:
        anoms.append((name, "typing", "no text change after tapping keyboard grid"))
        return anoms

    if expect_digits:
        digits = "".join(ch for ch in inserted if ch.isdigit())
        if len(set(digits)) < 3:  # very loose; just confirm numeric-ish
            anoms.append((name, "typing", f"expected digits, got inserted='{inserted[:30]}'"))

    return anoms


def main():
    all_anoms = []

    # Preflight ADB
    try:
        subprocess.check_call([ADB, "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=10)
    except Exception as e:
        print(f"ERROR: adb not runnable: {ADB} ({e})")
        raise SystemExit(2)

    devs = adb_devices()
    serial = _resolved_serial()
    if not serial:
        print(f"ERROR: no unique device selected. Set SERIAL=<deviceId>. Detected devices={devs}")
        raise SystemExit(3)

    if serial not in devs:
        print(f"ERROR: selected SERIAL not found/unauthorized: {serial}. Detected devices={devs}")
        raise SystemExit(4)

    run("am", "force-stop", PKG)
    run("pm", "clear", PKG)
    run("am", "start", "-n", ACT)
    time.sleep(1.3)

    all_anoms += test_field("TEXT", BTN_TEXT, ET_TEXT, expect_digits=False)
    all_anoms += test_field("NUMBER", BTN_NUMBER, ET_NUMBER, expect_digits=True)
    all_anoms += test_field("PHONE", BTN_PHONE, ET_PHONE, expect_digits=True)
    all_anoms += test_field("PASSWORD", BTN_PASSWORD, ET_PASSWORD, expect_digits=False)

    print("ANOMS", len(all_anoms), flush=True)
    for a in all_anoms:
        print(a, flush=True)


if __name__ == "__main__":
    main()
