#!/usr/bin/env python3
"""Host-driven draft recovery, or 4.0-to-4.1 upgrade with --upgrade-apk.

This terminates a real background process through ActivityManager while retaining
its task. It does not claim natural low-memory-killer or physical-device coverage.
"""

import argparse
from datetime import datetime, timezone
import json
import hashlib
from pathlib import Path
import re
import subprocess
import sys
import time
import traceback
import uuid
import xml.etree.ElementTree as ET


PACKAGE = "com.example.timeapk"
COMPONENT = PACKAGE + "/.MainActivity"
LABELS = {
    "add": ("Add event", "Record your first event", "添加事件", "记录第一个日期"),
    "new": ("New event", "新建事件"),
    "title": ("Title", "标题"),
    "note": ("Note", "备注"),
    "save": ("Commit", "落笔"),
    "back": ("Back", "返回"),
    "discard": ("Discard changes?", "放弃修改？"),
    "stay": ("Stay", "留在此页"),
    "edit": ("Edit", "编辑"),
    "editing": ("Edit event", "编辑事件"),
}


class SmokeFailure(RuntimeError):
    pass


def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if match:
        left, top, right, bottom = map(int, match.groups())
        if right > left and bottom > top:
            return left, top, right, bottom
    return None


def editable(node):
    return node.get("class") == "android.widget.EditText" or node.get("editable") == "true"


def package_identity(state):
    values = {}
    for key, pattern in {
        "versionName": r"^\s*versionName=([^\s]+)",
        "versionCode": r"^\s*versionCode=(\d+)\b",
        "firstInstallTime": r"^\s*firstInstallTime=([^\r\n]+)",
        "userId": r"^\s*(?:userId|appId)=(\d+)\b",
    }.items():
        matches = set(re.findall(pattern, state, re.MULTILINE))
        if len(matches) != 1:
            raise SmokeFailure("Expected one package " + key + ", found " + repr(matches))
        values[key] = next(iter(matches)).strip()
    return values


class UiTree:
    def __init__(self, data):
        self.root = ET.fromstring(data)
        self.parents = {child: parent for parent in self.root.iter() for child in parent}
        self.nodes = [node for node in self.root.iter("node") if node.get("package") == PACKAGE and bounds(node)]

    def contains(self, labels):
        return any(node.get("text") in labels or node.get("content-desc") in labels for node in self.nodes)

    def field(self, labels):
        matches = []
        for node in self.nodes:
            if not editable(node):
                continue
            # Compose can expose the description on the EditText or its wrapper.
            # Otherwise require a local labelled container with exactly one EditText.
            container = node
            for _ in range(4):
                # A form may show another field's label while its EditText is below
                # the viewport. The whole scroll container cannot bind that label.
                if container.get("scrollable") == "true":
                    break
                children = list(container.iter("node"))
                edits = [child for child in children if editable(child)]
                labelled = container.get("content-desc") in labels or any(
                    not editable(child) and (child.get("text") in labels or child.get("content-desc") in labels)
                    for child in children
                )
                if len(edits) == 1 and labelled:
                    matches.append(node)
                    break
                container = self.parents.get(container)
                if container is None:
                    break
        return self.unique(matches, "EditText labelled " + repr(labels))

    def action(self, labels=(), title=None, allow_selected=False):
        matches = []
        event_matches = []
        for node in self.nodes:
            description = node.get("content-desc", "")
            exact = node.get("text") in labels or description in labels
            event = title is not None and (node.get("text") == title or description.startswith(title + ", "))
            if not (exact or event):
                continue
            target = node
            # Compose omits the accessibility click action on selected choices,
            # although their native touch target still accepts taps.
            while target is not None and target.get("clickable") != "true" and not (
                allow_selected and (target.get("selected") == "true" or target.get("checked") == "true")
            ):
                target = self.parents.get(target)
            if target is not None and target.get("enabled") != "false" and bounds(target):
                # A partially visible event can expose an unclipped clickable
                # parent; tap its visible title instead of that parent's center.
                hit_target = node if event else target
                if hit_target.get("enabled") == "false":
                    continue
                matches.append(hit_target)
                if title is not None and description.startswith(title + ", "):
                    event_matches.append(hit_target)
        # The home timeline can repeat a card title; its bucket is not the card.
        return self.unique(list(dict.fromkeys(event_matches or matches)), "action " + repr(title or labels))

    @staticmethod
    def unique(matches, description):
        if len(matches) > 1:
            raise SmokeFailure("Ambiguous " + description + ": " + repr([node.attrib for node in matches]))
        return matches[0] if matches else None


class Smoke:
    def __init__(self, args):
        self.adb = [args.adb, "-s", args.serial]
        self.output = Path(args.output).resolve()
        if (self.output / "commands.jsonl").exists() or (self.output / "result.json").exists():
            raise SmokeFailure("Use a fresh evidence output directory: " + str(self.output))
        self.output.mkdir(parents=True, exist_ok=True)
        self.commands = self.output / "commands"
        self.commands.mkdir(exist_ok=True)
        self.sequence = 0
        self.snapshots = 0
        self.stage = "setup"
        self.last_xml = None
        self.evidence = {"scenario": "retained-task-background-process-draft-recovery", "serial": args.serial}

    def run(self, *arguments, check=True, timeout=15, destination=None):
        self.sequence += 1
        prefix = self.commands / ("%04d" % self.sequence)
        stdout_path = destination or prefix.with_suffix(".stdout")
        stderr_path = prefix.with_suffix(".stderr")
        command = self.adb + list(arguments)
        started = time.monotonic()
        expired = False
        try:
            result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
            stdout, stderr, returncode = result.stdout, result.stderr, result.returncode
        except subprocess.TimeoutExpired as error:
            stdout, stderr, returncode = error.stdout or b"", error.stderr or b"", None
            expired = True
        stdout_path.write_bytes(stdout)
        stderr_path.write_bytes(stderr)
        record = {
            "sequence": self.sequence, "stage": self.stage, "argv": command,
            "returncode": returncode, "timeout": expired,
            "seconds": round(time.monotonic() - started, 3),
            "stdout": str(stdout_path.relative_to(self.output)),
            "stderr": str(stderr_path.relative_to(self.output)),
        }
        with (self.output / "commands.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, ensure_ascii=False) + "\n")
        if expired or (check and returncode != 0):
            raise SmokeFailure("Command failed at %s (see command %04d): %s" % (
                self.stage, self.sequence, stderr.decode("utf-8", errors="replace")[-400:]))
        return stdout

    def text(self, *arguments, **options):
        return self.run(*arguments, **options).decode("utf-8", errors="replace")

    def screenshot(self, name):
        path = self.output / (name + ".png")
        data = self.run("exec-out", "screencap", "-p", destination=path)
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            raise SmokeFailure("Invalid screenshot PNG: " + str(path))

    def tree(self):
        self.snapshots += 1
        name = "%02d-%s" % (self.snapshots, self.stage)
        path = self.output / (name + ".xml")
        errors = []
        for attempt in range(3):
            try:
                self.run("shell", "uiautomator", "dump", "/sdcard/glimmer-runtime-smoke.xml", timeout=12)
                data = self.run("exec-out", "cat", "/sdcard/glimmer-runtime-smoke.xml", destination=path)
                tree = UiTree(data)
                if not tree.nodes:
                    raise SmokeFailure("UI dump has no visible target-package nodes")
                self.last_xml = str(path)
                return tree
            except (SmokeFailure, ET.ParseError) as error:
                errors.append(str(error))
                if path.exists():
                    path.rename(self.output / (name + "-attempt%d.xml" % attempt))
                time.sleep(0.4)
        raise SmokeFailure("UI dump failed after three attempts: " + "; ".join(errors))

    def checkpoint(self, stage):
        self.stage = stage
        tree = self.tree()
        self.screenshot(stage)
        return tree

    def seek(self, description, find, scroll=False, upward=False):
        for attempt in range(7 if scroll else 5):
            tree = self.tree()
            node = find(tree)
            if node is not None:
                return node
            if scroll:
                containers = [node for node in tree.nodes if node.get("scrollable") == "true" and not editable(node)]
                if not containers:
                    # Navigation can briefly expose the previous, non-scrollable page.
                    time.sleep(0.4)
                    continue
                container = max(containers, key=lambda node: (bounds(node)[2] - bounds(node)[0]) * (bounds(node)[3] - bounds(node)[1]))
                left, top, right, bottom = bounds(container)
                x = (left + right) // 2
                upper, lower = top + (bottom - top) // 5, top + (bottom - top) * 4 // 5
                start, end = (upper, lower) if upward else (lower, upper)
                self.run("shell", "input", "swipe", str(x), str(start), str(x), str(end), "350")
            time.sleep(0.4)
        raise SmokeFailure("Cannot find " + description + "; last XML: " + str(self.last_xml))

    def tap(self, node):
        left, top, right, bottom = bounds(node)
        self.run("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))

    def tap_action(self, key):
        self.tap(self.seek(key, lambda tree: tree.action(LABELS[key])))

    def hide_keyboard(self):
        state = self.text("shell", "dumpsys", "input_method")
        if re.search(r"\bmInputShown=true\b", state):
            self.run("shell", "input", "keyevent", "BACK")
            time.sleep(0.4)

    def field_text(self, key, expected, scroll=True):
        node = self.seek(key, lambda tree: tree.field(LABELS[key]), scroll=scroll, upward=key == "title")
        if node.get("text") != expected:
            raise SmokeFailure("%s value differs: expected %r, got %r" % (key, expected, node.get("text")))

    def enter(self, key, value, confirm_each_character=False):
        # BACK hides 4.0's IME without clearing field focus. Keep its pending
        # or shown input view so the focused-field path can become ready.
        if not confirm_each_character:
            self.hide_keyboard()
        node = self.seek(key, lambda tree: tree.field(LABELS[key]), scroll=True, upward=key == "title")
        if node.get("text", ""):
            raise SmokeFailure("New draft " + key + " was not empty")
        # 4.0 autofocus can show its IME after the XML dump; retapping an
        # already focused field can hit the keyboard at the old coordinates.
        if not confirm_each_character or node.get("focused") != "true":
            self.tap(node)
        if confirm_each_character:
            # PRETAG API36 sent its first U before the cold IME input view
            # started; the log records a 2500 ms IME event timeout and UU.
            def input_ready():
                state = self.text("shell", "dumpsys", "input_method")
                return all(re.search(r"\b" + flag + r"=true\b", state) for flag in (
                    "mInputShown", "mInputViewStarted", "mIsInputViewShown"))

            self.wait("4.0 IME input view ready", input_ready, timeout=20)
            node = self.seek(key, lambda tree: tree.field(LABELS[key]), scroll=False)
            if node.get("focused") != "true" or node.get("text", ""):
                raise SmokeFailure("4.0 " + key + " input was not focused and empty before typing")
            # The immutable 4.0 baseline has the old delayed text-echo race. Only
            # its upgrade fixture waits for each exact prefix through the real UI.
            for index, character in enumerate(value, 1):
                self.run("shell", "input", "text", character)
                self.field_text(key, value[:index], scroll=False)
        else:
            self.run("shell", "input", "text", value)
        self.field_text(key, value, scroll=False)
        self.hide_keyboard()

    def pid(self):
        return self.text("shell", "pidof", PACKAGE, check=False).strip()

    def task(self, stopped=False):
        state = self.text("shell", "dumpsys", "activity", "activities")
        records = list(re.finditer(r"ActivityRecord\{[^\n]*" + re.escape(COMPONENT) + r"\s+t(\d+)\b[^\n]*", state))
        task_ids = {match.group(1) for match in records}
        if len(task_ids) != 1:
            raise SmokeFailure("Expected one retained MainActivity task, found " + repr(task_ids))
        if stopped:
            stopped_records = []
            for match in records:
                end = state.find("ActivityRecord{", match.end())
                block = state[match.end():end if end != -1 else len(state)]
                stopped_records.append(bool(re.search(r"\b(?:mState|state)=STOPPED\b", block)))
            if not any(stopped_records):
                return None
        return next(iter(task_ids))

    def wait(self, description, read, timeout=15):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            value = read()
            if value:
                return value
            time.sleep(0.5)
        raise SmokeFailure("Timed out waiting for " + description)

    def launch(self):
        # Never use -S, force-stop, CLEAR_TASK, or remove the retained task here.
        self.run("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c",
                 "android.intent.category.LAUNCHER", "-n", COMPONENT, timeout=30)

    def execute(self):
        self.run("get-state")
        self.run("shell", "getprop", destination=self.output / "device-properties.txt")
        self.run("shell", "dumpsys", "package", PACKAGE, destination=self.output / "installed-package.txt")
        nonce = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8]
        title, note = "OSDraft-" + nonce, "OSNote-" + nonce
        self.evidence.update(title=title, note=note)

        self.launch()
        self.checkpoint("01-home")
        self.tap_action("add")
        self.stage = "02-enter-draft"
        self.enter("title", title)
        self.enter("note", note)
        self.checkpoint("03-unsaved-draft")
        old_pid = self.pid()
        if not re.fullmatch(r"\d+", old_pid):
            raise SmokeFailure("Expected exactly one application PID, got " + repr(old_pid))
        task_id = self.task()
        self.evidence.update(old_pid=old_pid, task_id=task_id)

        self.stage = "04-background"
        self.run("shell", "input", "keyevent", "HOME")
        stopped_task = self.wait("MainActivity STOPPED", lambda: self.task(stopped=True))
        if stopped_task != task_id:
            raise SmokeFailure("Task changed before background termination")
        self.screenshot(self.stage)
        self.stage = "05-process-kill"
        self.run("shell", "am", "kill", "--user", "0", PACKAGE)
        self.wait("application PID to disappear", lambda: not self.pid())
        if self.task() != task_id:
            raise SmokeFailure("Task was removed during background termination")
        package_state = self.text("shell", "dumpsys", "package", PACKAGE)
        if not re.search(r"User 0:[^\n]*\bstopped=false\b", package_state):
            raise SmokeFailure("Package is force-stopped or user-0 stopped state is unavailable")
        self.evidence["package_stopped_after_kill"] = False

        self.stage = "06-restore-launch"
        self.launch()
        new_pid = self.wait("replacement process", self.pid)
        if not re.fullmatch(r"\d+", new_pid) or new_pid == old_pid:
            raise SmokeFailure("Application did not receive a distinct replacement PID")
        if self.task() != task_id:
            raise SmokeFailure("Restoration created a different task")
        self.evidence["new_pid"] = new_pid
        tree = self.checkpoint("07-restored-draft")
        if not tree.contains(LABELS["new"]):
            raise SmokeFailure("The retained task did not restore the new-event editor")
        self.field_text("title", title)
        self.field_text("note", note)
        self.hide_keyboard()
        self.tap_action("back")
        self.stage = "08-unsaved-confirmation"
        self.seek("discard confirmation", lambda tree: tree.root if tree.contains(LABELS["discard"]) else None)
        self.checkpoint(self.stage)
        self.tap_action("stay")
        self.stage = "09-stayed-draft"
        self.field_text("note", note)
        self.checkpoint(self.stage)

        self.tap_action("save")
        self.stage = "10-saved-home"
        self.seek("home Add event", lambda tree: tree.action(LABELS["add"]))
        self.checkpoint(self.stage)
        event = self.seek("saved event", lambda tree: tree.action(title=title), scroll=True)
        self.tap(event)
        self.stage = "11-reopen-event"
        self.tap_action("edit")
        self.seek("edit-event page", lambda tree: tree.root if tree.contains(LABELS["editing"]) else None)
        self.field_text("title", title)
        self.field_text("note", note)
        self.checkpoint("12-persisted-note")
        self.tap_action("back")
        self.evidence["passed"] = True
        self.evidence["scope"] = "One small new draft; actual AMS background-process termination with retained task; no natural LMK, large-draft OS, save-in-flight, or physical-device claim."

    def date_text(self):
        labels = ("Date", "日期")
        row = self.seek("labelled date row", lambda tree: tree.action(labels), scroll=True, upward=True)
        values = {node.get("text", "").strip() for node in row.iter("node")
                  if re.search(r"\d", node.get("text", ""))}
        if len(values) != 1:
            raise SmokeFailure("Date row did not expose one stable date value: " + repr(values))
        return next(iter(values))

    def upgrade(self, apk):
        self.evidence["scenario"] = "saved-event-4.0-to-4.1-in-place-upgrade"
        apk = Path(apk).resolve()
        if not apk.is_file():
            raise SmokeFailure("Upgrade APK does not exist: " + str(apk))
        digest = hashlib.sha256()
        with apk.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        self.evidence.update(upgrade_apk=str(apk), upgrade_apk_sha256=digest.hexdigest(),
                             upgrade_apk_size=apk.stat().st_size)
        self.run("get-state")
        self.run("shell", "getprop", destination=self.output / "device-properties.txt")
        self.stage = "upgrade-01-baseline-package"
        before = package_identity(self.text("shell", "dumpsys", "package", PACKAGE,
                                            destination=self.output / "before-package.txt"))
        self.evidence["package_before"] = before
        if (before["versionName"], before["versionCode"]) != ("4.0", "23"):
            raise SmokeFailure("Upgrade requires installed baseline 4.0/23, got " + repr(before))

        nonce = uuid.uuid4().hex[:12]
        title, note = "UpgradeEvent-" + nonce, "UpgradeNote-" + nonce
        self.evidence.update(title=title, note=note,
                             baseline_input="4.0 only: each ASCII character confirmed against its complete UI prefix",
                             baseline_input_character_count=len(title) + len(note))
        self.launch()
        self.checkpoint("upgrade-02-baseline-home")
        self.tap_action("add")
        self.stage = "upgrade-03-create-fixture"
        self.enter("title", title, confirm_each_character=True)
        self.enter("note", note, confirm_each_character=True)
        self.checkpoint(self.stage)
        self.tap_action("save")
        self.stage = "upgrade-04-saved-baseline"
        self.seek("baseline home", lambda tree: tree.action(LABELS["add"]))
        self.checkpoint(self.stage)
        self.tap(self.seek("baseline event", lambda tree: tree.action(title=title), scroll=True))
        self.tap_action("edit")
        self.stage = "upgrade-05-baseline-fields"
        self.seek("baseline editor", lambda tree: tree.root if tree.contains(LABELS["editing"]) else None)
        self.field_text("title", title)
        self.field_text("note", note)
        baseline_date = self.date_text()
        self.evidence["date_before"] = baseline_date
        self.checkpoint(self.stage)
        self.hide_keyboard()
        self.tap_action("back")
        self.seek("baseline home before install", lambda tree: tree.action(LABELS["add"]))

        self.stage = "upgrade-06-install"
        response = self.text("install", "-r", "--user", "0", str(apk), timeout=120)
        if not re.search(r"^Success\s*$", response, re.MULTILINE):
            raise SmokeFailure("Upgrade install did not report Success")
        after = package_identity(self.text("shell", "dumpsys", "package", PACKAGE,
                                           destination=self.output / "after-package.txt"))
        self.evidence["package_after"] = after
        if (after["versionName"], after["versionCode"]) != ("4.1", "24"):
            raise SmokeFailure("Upgrade did not install 4.1/24: " + repr(after))
        for key in ("firstInstallTime", "userId"):
            if after[key] != before[key]:
                raise SmokeFailure("In-place upgrade changed " + key)

        self.stage = "upgrade-07-launch"
        self.launch()
        self.seek("upgraded home", lambda tree: tree.action(LABELS["add"]))
        self.checkpoint(self.stage)
        self.tap(self.seek("preserved event", lambda tree: tree.action(title=title), scroll=True))
        self.tap_action("edit")
        self.stage = "upgrade-08-preserved-fields"
        self.seek("upgraded editor", lambda tree: tree.root if tree.contains(LABELS["editing"]) else None)
        self.field_text("title", title)
        self.field_text("note", note)
        upgraded_date = self.date_text()
        self.evidence["date_after"] = upgraded_date
        if upgraded_date != baseline_date:
            raise SmokeFailure("Upgrade changed the saved event date")
        self.checkpoint(self.stage)
        self.hide_keyboard()
        self.tap_action("back")
        self.evidence["passed"] = True
        self.evidence["scope"] = "One saved synthetic event upgraded from 4.0/23 to 4.1/24 with unchanged firstInstallTime/userId and title/note/displayed date; caller verifies APK signature separately; no 4.0 process-recovery or physical-device claim."

    def capture_pages(self, title):
        self.stage = "pages-00-package"
        identity = package_identity(self.text("shell", "dumpsys", "package", PACKAGE,
                                               destination=self.output / "pages-installed-package.txt"))
        if (identity["versionName"], identity["versionCode"]) != ("4.1", "24"):
            raise SmokeFailure("Page capture requires installed 4.1/24: " + repr(identity))
        self.evidence.update(pages_package=identity, pages_title=title, captured_pages=[],
                             pages_scope="Five actual MainActivity pages: home cards, calendar, the unique saved event detail, settings root, and app-internal widget default configuration preview; no Launcher widget binding claim.")

        def selected(tree, labels):
            node = tree.action(labels, allow_selected=True)
            return node is not None and (node.get("selected") == "true" or node.get("checked") == "true")

        def capture(stage, visible):
            self.stage = stage
            self.seek(stage + " markers", lambda tree: tree.root if visible(tree) else None)
            tree = self.checkpoint(stage)
            if not visible(tree):
                raise SmokeFailure("Page markers changed before capture: " + stage)
            activity_file = self.output / (stage + "-activity.txt")
            activity = self.text("shell", "dumpsys", "activity", "activities", destination=activity_file)
            if not re.search(r"(?:mResumedActivity|topResumedActivity|ResumedActivity)[^\n]*" +
                             re.escape(COMPONENT) + r"\b", activity):
                raise SmokeFailure("Captured page is not resumed MainActivity: " + stage)
            self.evidence["captured_pages"].append({
                "page": stage, "png": stage + ".png",
                "xml": str(Path(self.last_xml).relative_to(self.output)),
                "activity": activity_file.name,
            })

        cards = ("Cards", "卡片")
        calendar = ("Calendar", "月历")
        settings = ("Settings", "设置")
        widget = ("Widget settings", "小组件设置")
        defaults = ("Default configuration", "默认配置")
        preview = ("Preview", "预览")
        self.stage = "pages-01-home-cards"
        self.tap(self.seek("Cards tab", lambda tree: tree.action(cards, allow_selected=True)))

        tools = ("Filters and sorting", "筛选与排序")
        by_days = ("By days left", "按剩余天数")
        custom = ("Custom order", "自定义排序")
        self.evidence["sort_menu_touch"] = {
            "passed": False, "selections": [],
            "scope": "Actual MainActivity menu touch targets, reopened selected-state XML, and native Back dismissal; an initially selected ByDays is not evidence of a state change.",
        }
        self.stage = "pages-sort-00-open"
        self.tap(self.seek("Filters and sorting", lambda tree: tree.action(tools)))
        for name, labels in (("by-days", by_days), ("custom", custom)):
            self.stage = "pages-sort-" + name + "-tap"
            self.tap(self.seek(name + " sort option", lambda tree: tree.action(labels, allow_selected=True), scroll=True))
            # A physical option tap must dismiss the popup before reopening it.
            self.tap(self.seek("reopen Filters and sorting", lambda tree: tree.action(tools)))
            self.stage = "pages-sort-" + name + "-selected"
            self.seek(name + " selected after reopening", lambda tree: tree.root if selected(tree, labels) else None, scroll=True)
            tree = self.checkpoint(self.stage)
            if not selected(tree, labels):
                raise SmokeFailure("Sort selection changed before capture: " + name)
            node = tree.action(labels, allow_selected=True)
            self.evidence["sort_menu_touch"]["selections"].append({
                "sort": name, "checked": node.get("checked"), "selected": node.get("selected"),
                "xml": str(Path(self.last_xml).relative_to(self.output)), "png": self.stage + ".png",
            })
        self.run("shell", "input", "keyevent", "BACK")
        self.stage = "pages-01-home-cards"
        capture(self.stage, lambda tree: selected(tree, cards) and tree.action(title=title) is not None and
                not tree.contains(by_days + custom))
        self.evidence["sort_menu_touch"].update(
            passed=True, closed_by_native_back=True,
            closed_xml=str(Path(self.last_xml).relative_to(self.output)), closed_png=self.stage + ".png"
        )

        self.stage = "pages-02-calendar"
        self.tap(self.seek("Calendar tab", lambda tree: tree.action(calendar, allow_selected=True)))
        capture(self.stage, lambda tree: selected(tree, calendar) and
                tree.contains(("Previous month", "上个月")) and tree.contains(("Next month", "下个月")))
        self.tap(self.seek("unique saved event in calendar", lambda tree: tree.action(title=title), scroll=True))
        capture("pages-03-event-detail", lambda tree: tree.contains(("Event detail", "事件详情")) and
                tree.contains((title,)) and tree.action(LABELS["edit"]) is not None)
        self.check_share(title)

        self.tap_action("back")
        self.stage = "pages-04-settings-root"
        self.tap(self.seek("Settings entry", lambda tree: tree.action(settings)))
        capture(self.stage, lambda tree: tree.contains(settings) and tree.action(widget) is not None and
                tree.action(("Theme", "主题")) is not None and tree.action(("Export / Import", "导出 / 导入")) is not None)
        self.tap(self.seek("Widget settings entry", lambda tree: tree.action(widget)))
        self.stage = "pages-05-widget-default-preview"
        tree = self.seek("widget default configuration", lambda tree: tree if
                         tree.contains(widget) and tree.action(defaults) is not None else None)
        if not tree.contains(preview):
            self.tap(tree.action(defaults))
        capture(self.stage, lambda tree: tree.contains(widget) and tree.contains(defaults) and tree.contains(preview))

    def check_share(self, title):
        share = ("Share", "分享")
        dialog = ("Share card", "分享笺")
        save = ("Save image", "保存图片")
        send = ("Share", "唤起分享")
        saved = ("Image saved", "图片已入相册")
        failed = ("Could not prepare the image", "成图未成，稍候再试")
        safe_title = re.sub(r"[^A-Za-z0-9\u4e00-\u9fa5_-]+", "-", title).strip("-")[:28] or "event"
        name = "timeapk-" + safe_title + ".png"
        media = "content://media/external/images/media"
        self.evidence["share_check"] = {
            "passed": False, "title": title, "display_name": name,
            "scope": "Actual MainActivity save to MediaStore and raw PNG verification; system chooser opened and cancelled; no recipient selection, URI-read or delivery claim.",
        }

        def media_rows():
            # Filename characters are restricted above; retain SQL string quotes
            # across adb shell's argument joining without accepting arbitrary SQL.
            result = self.text("shell", "content", "query", "--uri", media,
                               "--projection", "_id:_display_name:mime_type:relative_path:is_pending",
                               "--where", '"_display_name=\'' + name + '\'"')
            if result.strip() == "No result found.":
                return []
            rows = [dict(field.strip().split("=", 1) for field in line.split(", "))
                    for line in re.findall(r"^Row:\s*\d+\s+(.+)$", result, re.MULTILINE)]
            if not rows or any(not re.fullmatch(r"\d+", row.get("_id", "")) or
                               row.get("_display_name") != name for row in rows):
                raise SmokeFailure("Unexpected MediaStore query result: " + result[-500:])
            return rows

        def dialog_ready(tree):
            if tree.contains(failed):
                raise SmokeFailure("Actual share UI reported image preparation failure")
            return tree if tree.contains(dialog) and tree.action(save) is not None and any(
                title in node.get("content-desc", "") for node in tree.nodes) else None

        def save_feedback(tree):
            if tree.contains(failed):
                raise SmokeFailure("Actual share UI reported image save failure")
            return str(Path(self.last_xml).relative_to(self.output)) if tree.contains(saved) else None

        self.stage = "share-00-media-before"
        before_ids = {row["_id"] for row in media_rows()}
        self.stage = "share-01-preview"
        self.tap(self.seek("detail Share", lambda tree: tree.action(share)))
        self.seek("rendered share dialog", dialog_ready)
        self.checkpoint(self.stage)
        self.stage = "share-02-save"
        self.tap(self.seek("Save image", lambda tree: tree.action(save)))

        def published_image():
            rows = [row for row in media_rows() if row["_id"] not in before_ids]
            if len(rows) > 1:
                raise SmokeFailure("One save action created multiple MediaStore images")
            if rows and rows[0].get("is_pending") == "0":
                row = rows[0]
                if row.get("mime_type") != "image/png" or row.get("relative_path", "").strip("/") != "Pictures/TimeAPK":
                    raise SmokeFailure("Saved image MIME or location differs: " + repr(row))
                return row
            return None

        tree = self.seek("completed save controls", lambda tree: tree if
                         tree.action(("Cancel", "取消")) is not None else None)
        feedback_xml = save_feedback(tree)
        if feedback_xml:
            self.screenshot("share-02-save-feedback")
        self.tap(tree.action(("Cancel", "取消")))
        tree = self.checkpoint("share-03-saved-detail")
        feedback_xml = save_feedback(tree) or feedback_xml
        row = self.wait("new published share image", published_image, timeout=20)
        uri = media + "/" + row["_id"]
        image_path = self.output / "share-saved-image.png"
        data = self.run("exec-out", "content", "read", "--uri", uri, destination=image_path)
        if len(data) < 33 or not data.startswith(b"\x89PNG\r\n\x1a\n") or data[12:16] != b"IHDR" or not data.endswith(b"\x00\x00\x00\x00IEND\xaeB`\x82"):
            raise SmokeFailure("Saved MediaStore image is not a complete PNG")
        dimensions = (int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big"))
        if dimensions != (1080, 1350) or not image_path.is_file():
            raise SmokeFailure("Saved share image dimensions differ: " + repr(dimensions))
        self.evidence["share_check"].update(
            source="MediaStore row created by this UI save action", saved_uri=uri, media_row=row,
            saved_png=image_path.name, bytes=len(data), sha256=hashlib.sha256(data).hexdigest(),
            width=dimensions[0], height=dimensions[1], feedback_observed=feedback_xml is not None,
            feedback_xml=feedback_xml,
            feedback_scope="Transient success feedback is recorded only if observed; new published row and raw PNG independently prove saving.",
        )

        self.stage = "share-04-chooser"
        self.tap(self.seek("reopen detail Share", lambda tree: tree.action(share)))
        tree = self.seek("rendered share dialog", dialog_ready)
        self.tap(tree.action(send))

        def chooser_activity():
            state = self.text("shell", "dumpsys", "activity", "activities")
            if re.search(r"(?:mResumedActivity|topResumedActivity|ResumedActivity)[^\n]*(?:android|com\.android\.intentresolver)/[^\s}]*ChooserActivity(?:Launcher)?\b", state) and "android.intent.action.CHOOSER" in state:
                return state
            return None

        activity = self.wait("system chooser Activity", chooser_activity, timeout=20)
        (self.output / "share-04-chooser-activity.txt").write_text(activity, encoding="utf-8")
        # The chooser is a system-package window, deliberately outside UiTree.nodes.
        chooser_xml = self.output / "share-04-chooser.xml"
        self.run("shell", "uiautomator", "dump", "/sdcard/glimmer-runtime-smoke.xml", timeout=12)
        raw = self.run("exec-out", "cat", "/sdcard/glimmer-runtime-smoke.xml", destination=chooser_xml)
        ET.fromstring(raw)
        self.last_xml = str(chooser_xml)
        self.screenshot(self.stage)
        self.run("shell", "input", "keyevent", "BACK")
        self.stage = "share-05-returned-detail"
        self.seek("same detail after chooser Back", lambda tree: tree.root if
                  tree.contains(("Event detail", "事件详情")) and tree.contains((title,)) and
                  tree.action(LABELS["edit"]) is not None and not tree.contains(dialog) else None)
        self.checkpoint(self.stage)
        activity_file = self.output / (self.stage + "-activity.txt")
        activity = self.text("shell", "dumpsys", "activity", "activities", destination=activity_file)
        if not re.search(r"(?:mResumedActivity|topResumedActivity|ResumedActivity)[^\n]*" + re.escape(COMPONENT) + r"\b", activity):
            raise SmokeFailure("Chooser Back did not resume MainActivity")
        self.evidence["share_check"].update(passed=True, chooser_xml=chooser_xml.name,
                                           chooser_png="share-04-chooser.png", returned_activity=activity_file.name)

    def check_update(self):
        self.stage = "update-01-open-about"
        package_file = self.output / "update-installed-package.txt"
        identity = package_identity(self.text("shell", "dumpsys", "package", PACKAGE, destination=package_file))
        if (identity["versionName"], identity["versionCode"]) != ("4.1", "24"):
            raise SmokeFailure("Published-update check requires installed 4.1/24: " + repr(identity))
        tree = self.tree()
        if tree.contains(("Widget settings", "小组件设置")) and tree.contains(("Default configuration", "默认配置")):
            self.tap_action("back")
            tree = self.tree()
        settings = tree.action(("Settings", "设置"))
        if settings is not None:
            self.tap(settings)
        about = ("About & updates", "关于与更新")
        self.tap(self.seek("About & updates entry", lambda tree: tree.action(about), scroll=True))
        update = ("Check for updates", "探寻新章")
        latest = ("Already up to date", "已是最新版本")
        self.seek("About update controls", lambda tree: tree.root if
                  tree.contains(about) and tree.action(update) is not None else None)
        tree = self.checkpoint("update-02-about-ready")
        if tree.contains(latest):
            raise SmokeFailure("Latest-version confirmation was already visible before this check")
        control = tree.action(update)
        if control is None:
            raise SmokeFailure("Manual update control disappeared before this check")
        self.tap(control)
        self.stage = "update-03-already-current"

        def read_result():
            tree = self.tree()
            if tree.contains(("Update search did not settle.", "探寻未成，稍候再试")):
                raise SmokeFailure("Actual UI update check reported failure")
            if any(node.get(key, "").startswith(("New version ", "发现新版本 "))
                   for node in tree.nodes for key in ("text", "content-desc")):
                raise SmokeFailure("Actual UI found a different latest version")
            return tree if tree.contains(about) and tree.contains(latest) else None

        self.wait("Already up to date UI confirmation", read_result, timeout=35)
        # The success snackbar is transient: its matched XML is already recorded.
        # Capture PNG immediately instead of spending another UI dump on it.
        result_xml = str(Path(self.last_xml).relative_to(self.output))
        self.screenshot(self.stage)
        activity_file = self.output / (self.stage + "-activity.txt")
        activity = self.text("shell", "dumpsys", "activity", "activities", destination=activity_file)
        if not re.search(r"(?:mResumedActivity|topResumedActivity|ResumedActivity)[^\n]*" +
                         re.escape(COMPONENT) + r"\b", activity):
            raise SmokeFailure("Update success was not observed in resumed MainActivity")
        self.evidence["update_check"] = {
            "passed": True, "result": "update_latest", "package": identity,
            "package_dump": package_file.name, "xml": result_xml,
            "png": self.stage + ".png", "activity": activity_file.name,
            "scope": "Actual Direct-app manual update UI returned Already up to date; caller independently verifies the public GitHub latest release and exact APK asset.",
        }

    def diagnostics(self):
        self.evidence["diagnostic_scope"] = "Native frame and memory snapshots only; no full performance benchmark or physical-device claim."
        self.stage = "diagnostics"
        for arguments in (("shell", "dumpsys", "activity", "activities"),
                          ("shell", "dumpsys", "package", PACKAGE),
                          ("shell", "dumpsys", "gfxinfo", PACKAGE, "framestats"),
                          ("shell", "dumpsys", "meminfo", PACKAGE),
                          ("logcat", "-d", "-t", "300", "-v", "threadtime")):
            try:
                self.run(*arguments, check=False)
            except Exception:
                pass
        try:
            self.screenshot("final-state")
            self.tree()
        except Exception:
            pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--upgrade-apk", help="Upgrade installed 4.0/23 to this caller-verified signed 4.1/24 APK")
    parser.add_argument("--capture-pages", action="store_true", help="After success, capture five MainActivity pages for manual review")
    parser.add_argument("--check-update", action="store_true", help="After success, require the published Direct-app update UI to confirm Already up to date")
    args = parser.parse_args()
    smoke = Smoke(args)
    try:
        if args.upgrade_apk:
            smoke.upgrade(args.upgrade_apk)
        else:
            smoke.execute()
        if args.capture_pages and smoke.evidence.get("passed"):
            smoke.capture_pages(smoke.evidence["title"])
        if args.check_update and smoke.evidence.get("passed"):
            smoke.check_update()
    except Exception as error:
        smoke.evidence.update(passed=False, failed_stage=smoke.stage, error=str(error), last_xml=smoke.last_xml)
        (smoke.output / "failure.txt").write_text(traceback.format_exc(), encoding="utf-8")
    finally:
        smoke.diagnostics()
        (smoke.output / "result.json").write_text(json.dumps(smoke.evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(smoke.evidence, ensure_ascii=False))
    return 0 if smoke.evidence.get("passed") else 1


if __name__ == "__main__":
    sys.exit(main())
