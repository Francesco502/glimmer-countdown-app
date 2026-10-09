#!/usr/bin/env python3
"""One host-driven draft recovery smoke; requires an installed Direct APK.

This terminates a real background process through ActivityManager while retaining
its task. It does not claim natural low-memory-killer or physical-device coverage.
"""

import argparse
from datetime import datetime, timezone
import json
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
            # Otherwise require a labelled container with exactly one EditText.
            container = node
            for _ in range(4):
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

    def action(self, labels=(), title=None):
        matches = []
        event_matches = []
        for node in self.nodes:
            description = node.get("content-desc", "")
            exact = node.get("text") in labels or description in labels
            event = title is not None and (node.get("text") == title or description.startswith(title + ", "))
            if not (exact or event):
                continue
            target = node
            while target is not None and target.get("clickable") != "true":
                target = self.parents.get(target)
            if target is not None and target.get("enabled") != "false" and bounds(target):
                matches.append(target)
                if title is not None and description.startswith(title + ", "):
                    event_matches.append(target)
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
                    raise SmokeFailure("No scroll container while looking for " + description)
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

    def enter(self, key, value):
        self.hide_keyboard()
        node = self.seek(key, lambda tree: tree.field(LABELS[key]), scroll=True, upward=key == "title")
        if node.get("text", ""):
            raise SmokeFailure("New draft " + key + " was not empty")
        self.tap(node)
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

    def diagnostics(self):
        self.stage = "diagnostics"
        for arguments in (("shell", "dumpsys", "activity", "activities"),
                          ("shell", "dumpsys", "package", PACKAGE),
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
    smoke = Smoke(parser.parse_args())
    try:
        smoke.execute()
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
