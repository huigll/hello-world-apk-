#!/usr/bin/env python3
"""Full functional traversal test for the in-app keyboard demo.

What it checks (best-effort, UIAutomator based):
- TEXT field: cycle EN/ZH/FR/AR, tap all visible keys, enter symbols and return.
- ZH: type 'n''i' and verify candidate buttons appear.
- NUMBER/PHONE: verify digits 0-9 exist (numeric keypad).
- PASSWORD: verify stays on EN (lang key cannot cycle), and no candidate buttons appear.

Usage:
  python3 tools/full_traverse.py

Requirements:
- adb in PATH OR set ADB env var.
- A device connected (adb devices).
"""

import os
import re
import time
import subprocess
import xml.etree.ElementTree as ET

ADB = os.environ.get("ADB", "/home/larry/android-dev/sdk/platform-tools/adb")
PKG = os.environ.get("PKG", "com.carbit.inappkeyboard")
ACT = os.environ.get("ACT", "com.carbit.inappkeyboard/.MainActivity")

ET_TEXT = f"{PKG}:id/et_text"
ET_NUMBER = f"{PKG}:id/et_number"
ET_PASSWORD = f"{PKG}:id/et_password"
ET_PHONE = f"{PKG}:id/et_phone"
KBD_CONTAINER = f"{PKG}:id/main_keyboard_container"

DUMP_PATH = os.environ.get("UI_DUMP", "/sdcard/window_dump.xml")


def _run_shell(*args, timeout=30):
    subprocess.run([ADB, "shell", *args], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout, check=False)


def _out_shell(*args, timeout=30) -> str:
    return subprocess.check_output([ADB, "shell", *args], timeout=timeout).decode("utf-8", "ignore")


def dump(tag: str) -> str:
    # uiautomator dump is sometimes async; retry a few times.
    for _ in range(5):
        _run_shell("uiautomator", "dump", DUMP_PATH, timeout=30)
        time.sleep(0.05)
        try:
            xml = _out_shell("cat", DUMP_PATH, timeout=30)
            if "<hierarchy" in xml:
                return xml
        except Exception:
            time.sleep(0.05)
    return ""


def parse(xml: str):
    return ET.fromstring(xml)


def center(bounds: str):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2, (x1, y1, x2, y2)


def tap(x: int, y: int):
    _run_shell("input", "tap", str(x), str(y))


def swipe(x1, y1, x2, y2, dur_ms=300):
    _run_shell("input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur_ms))


def find(root, pred):
    for n in root.iter("node"):
        if pred(n):
            return n
    return None


def tap_res_with_scroll(res_id: str, max_scrolls=7, tag="tap") -> bool:
    """Try to tap a view by resource-id, scrolling if needed.

    Also verifies focus for EditText-like targets: after tapping, re-dumps and checks `focused=true`.
    """
    for i in range(max_scrolls + 1):
        xml = dump(f"{tag}_{i}")
        if not xml:
            continue
        root = parse(xml)
        n = find(root, lambda n: n.attrib.get("resource-id") == res_id)
        if n is not None:
            c = center(n.attrib.get("bounds"))
            if not c:
                return False
            cx, cy, _ = c

            # Tap twice if needed (some OEMs are flaky on first tap).
            tap(cx, cy)
            time.sleep(0.15)
            tap(cx, cy)
            time.sleep(0.25)

            # Verify focus when possible
            xml2 = dump(f"{tag}_{i}_after")
            if xml2:
                r2 = parse(xml2)
                n2 = find(r2, lambda n: n.attrib.get("resource-id") == res_id)
                if n2 is not None:
                    focused = n2.attrib.get("focused")
                    # If it's focusable but didn't become focused, treat as failure.
                    if n2.attrib.get("focusable") == "true" and focused != "true":
                        # keep trying by scrolling a bit and retry
                        pass
                    else:
                        return True
            else:
                return True

        # swipe up (content moves up)
        swipe(540, 820, 540, 360, 320)
        time.sleep(0.2)
    return False


def get_edit_text(root, res_id: str) -> str:
    n = find(root, lambda n: n.attrib.get("resource-id") == res_id)
    return (n.attrib.get("text") or "") if n is not None else ""


def get_container_bounds(root):
    n = find(root, lambda n: n.attrib.get("resource-id") == KBD_CONTAINER)
    if not n:
        return None
    _, _, b = center(n.attrib["bounds"])
    return b


def buttons_set(root):
    s = set()
    for n in root.iter("node"):
        if n.attrib.get("class") == "android.widget.Button":
            t = (n.attrib.get("text") or "").strip()
            if t:
                s.add(t)
    return s


def keys_in_container(root, cb):
    x1, y1, x2, y2 = cb
    keys = []
    for n in root.iter("node"):
        if n.attrib.get("class") != "android.widget.Button":
            continue
        t = (n.attrib.get("text") or "").strip()
        b = n.attrib.get("bounds")
        if not t or not b:
            continue
        c = center(b)
        if not c:
            continue
        cx, cy, (bx1, by1, bx2, by2) = c
        if x1 <= cx <= x2 and y1 <= cy <= y2:
            keys.append((by1, bx1, t, cx, cy))

    keys.sort()
    seen = set()
    out = []
    for _, __, t, cx, cy in keys:
        if t in seen:
            continue
        seen.add(t)
        out.append((t, cx, cy))
    return out


def find_btn_xy(root, text: str):
    for n in root.iter("node"):
        if n.attrib.get("class") == "android.widget.Button":
            t = (n.attrib.get("text") or "").strip()
            if t == text:
                c = center(n.attrib.get("bounds") or "")
                if c:
                    return c[0], c[1]
    return None


def lang_btn(root):
    # Robust: bottom-row leftmost button in keyboard container.
    cb = get_container_bounds(root)
    if not cb:
        return None, None
    x1, y1, x2, y2 = cb
    btns = []
    for n in root.iter("node"):
        if n.attrib.get("class") != "android.widget.Button":
            continue
        t = (n.attrib.get("text") or "").strip()
        b = n.attrib.get("bounds")
        if not t or not b:
            continue
        c = center(b)
        if not c:
            continue
        cx, cy, (bx1, by1, bx2, by2) = c
        if not (x1 <= cx <= x2 and y1 <= cy <= y2):
            continue
        btns.append((by1, bx1, t, cx, cy))

    if not btns:
        return None, None

    bottom_by1 = max(r[0] for r in btns)
    bottom = [r for r in btns if r[0] >= bottom_by1 - 12]
    bottom.sort(key=lambda r: r[1])
    _, __, t, cx, cy = bottom[0]
    return t, (cx, cy)


def ensure_letters_page():
    for _ in range(4):
        root = parse(dump("ensure"))
        cur, xy = lang_btn(root)
        if cur == "#" and xy:
            tap(*xy)
            time.sleep(0.2)
        else:
            return


def cycle_to(target: str):
    for _ in range(18):
        root = parse(dump("cy"))
        cur, xy = lang_btn(root)
        if cur == target:
            return root, cur
        if cur == "#" and xy:
            tap(*xy)
            time.sleep(0.2)
            continue
        if xy:
            tap(*xy)
            time.sleep(0.15)
    return root, cur


def find_candidates(root):
    c = []
    for n in root.iter("node"):
        if n.attrib.get("class") == "android.widget.Button":
            t = (n.attrib.get("text") or "").strip()
            if t and any("\u4e00" <= ch <= "\u9fff" for ch in t) and t != "中":
                c.append(t)
    return c


def main():
    anoms = []

    _run_shell("pm", "clear", PKG)
    _run_shell("am", "start", "-n", ACT)
    time.sleep(1.0)

    # TEXT: full traverse
    if not tap_res_with_scroll(ET_TEXT, tag="focus_text"):
        anoms.append(("TEXT", "focus", "failed to focus et_text"))
    else:
        ensure_letters_page()
        for lang in ["EN", "中", "FR", "AR"]:
            root, cur = cycle_to(lang)
            print(f"== TEXT {cur} ==", flush=True)
            root = parse(dump("kb"))
            cb = get_container_bounds(root)
            if not cb:
                anoms.append(("TEXT", cur, "container", "missing keyboard container"))
                continue
            keys = keys_in_container(root, cb)
            print("keys", len(keys), flush=True)

            for idx, (k, x, y) in enumerate(keys, 1):
                before_txt = get_edit_text(root, ET_TEXT)
                before_btns = buttons_set(root)
                tap(x, y)
                time.sleep(0.05)
                root = parse(dump("step"))
                after_txt = get_edit_text(root, ET_TEXT)
                after_btns = buttons_set(root)

                if cur == "中":
                    if k not in {"Space", "Enter", "⌫", "⇧", "123", "中", "EN", "FR", "AR", "#"} and after_txt != before_txt:
                        anoms.append(("TEXT", cur, k, "ZH letter changed text"))
                else:
                    if k not in {"⌫", "⇧", "123", "EN", "FR", "AR", "中", "#"} and after_txt == before_txt and after_btns == before_btns:
                        anoms.append(("TEXT", cur, k, "key no change (text+ui)"))

                if idx % 10 == 0:
                    print(f"..{idx}/{len(keys)}", flush=True)

            # symbols toggle
            root = parse(dump("preS"))
            xy123 = find_btn_xy(root, "123")
            if xy123:
                before_btns = buttons_set(root)
                tap(*xy123)
                time.sleep(0.2)
                root = parse(dump("sym"))
                sym_btns = buttons_set(root)
                if sym_btns == before_btns:
                    anoms.append(("TEXT", cur, "123", "failed to enter symbols"))
                cb = get_container_bounds(root)
                if cb:
                    skeys = keys_in_container(root, cb)
                    sample = [k for k, _, _ in skeys if k not in {"ABC", "⌫", "Space", "Enter", "123"}][:30]
                    for k in sample:
                        xy = find_btn_xy(root, k)
                        if not xy:
                            continue
                        before = get_edit_text(root, ET_TEXT)
                        tap(*xy)
                        time.sleep(0.03)
                        root = parse(dump("symstep"))
                        after = get_edit_text(root, ET_TEXT)
                        if after == before:
                            anoms.append(("TEXT", cur, "SYM:" + k, "symbol no change"))
                xyabc = find_btn_xy(root, "ABC")
                if xyabc:
                    tap(*xyabc)
                    time.sleep(0.2)
                    root = parse(dump("back"))
                    after_btns = buttons_set(root)
                    if "q" not in after_btns and "a" not in after_btns:
                        anoms.append(("TEXT", cur, "ABC", "failed to return to letters"))

        # ZH candidates
        root, _ = cycle_to("中")
        root = parse(dump("zh"))
        for ch in ["n", "i"]:
            xy = find_btn_xy(root, ch)
            if xy:
                tap(*xy)
                time.sleep(0.06)
                root = parse(dump("zh2"))
        cand = find_candidates(root)
        print("TEXT ZH candidates after ni:", cand[:10], flush=True)
        if not cand:
            anoms.append(("TEXT", "中", "candidates", "no candidates after ni"))

    # NUMBER
    if not tap_res_with_scroll(ET_NUMBER, tag="focus_number"):
        anoms.append(("NUMBER", "focus", "failed to focus et_number"))
    else:
        root = parse(dump("num"))
        btns = buttons_set(root)
        print("== NUMBER ==", flush=True)
        for d in list("1234567890"):
            if d not in btns:
                anoms.append(("NUMBER", "layout", "missing digit " + d))

    # PHONE
    if not tap_res_with_scroll(ET_PHONE, tag="focus_phone"):
        anoms.append(("PHONE", "focus", "failed to focus et_phone"))
    else:
        root = parse(dump("phone"))
        btns = buttons_set(root)
        print("== PHONE ==", flush=True)
        for d in list("1234567890"):
            if d not in btns:
                anoms.append(("PHONE", "layout", "missing digit " + d))

    # PASSWORD
    if not tap_res_with_scroll(ET_PASSWORD, tag="focus_pwd"):
        anoms.append(("PASSWORD", "focus", "failed to focus et_password"))
    else:
        ensure_letters_page()
        root = parse(dump("pwd"))
        btns = buttons_set(root)
        print("== PASSWORD ==", flush=True)
        if "q" not in btns and "a" not in btns:
            anoms.append(("PASSWORD", "layout", "missing letters q/a"))

        cur, xy = lang_btn(root)
        if xy:
            for _ in range(3):
                tap(*xy)
                time.sleep(0.15)
                root = parse(dump("pwd_lang"))
                cur2, _ = lang_btn(root)
                if cur2 != "EN":
                    anoms.append(("PASSWORD", "lang", "lang changed from EN"))
                    break

        # no candidates
        for ch in ["n", "i"]:
            xy2 = find_btn_xy(root, ch)
            if xy2:
                tap(*xy2)
                time.sleep(0.06)
                root = parse(dump("pwd_type"))
        cand2 = find_candidates(root)
        if cand2:
            anoms.append(("PASSWORD", "candidates", "unexpected candidates"))

    print("ANOMS", len(anoms), flush=True)
    for a in anoms[:200]:
        print(a, flush=True)


if __name__ == "__main__":
    main()
