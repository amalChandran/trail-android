#!/usr/bin/env python3
"""Summarize observed passing test cases, not inferred test counts from source code."""
import json
from collections import Counter
from pathlib import Path
import re
import sys

kind, filename = sys.argv[1:]
output = Path(filename).with_suffix(".json")
# A failed rerun must not leave an earlier green report beside the new failed log.
output.unlink(missing_ok=True)
log = Path(filename).read_text()
if kind in ("swift", "ios"):
    summary = re.search(r"✔ Test run with (\d+) tests(?: in \d+ suites)? passed", log)
    matches = re.findall(r"✔ Test (?!run )(.+?)(?: with (\d+) test cases)? passed after", log)
    if (kind == "swift" and not summary) or (summary and len(matches) != int(summary[1])):
        raise SystemExit("Missing or inconsistent passing Swift Testing summary.")
    # Older Swift Testing logs each argument invocation instead of a per-function case total.
    invocations = Counter(re.findall(r"◇ Passing .* to ([^\r\n]+)", log))
    cases = 0
    for name, count in matches:
        observed = invocations[name]
        if count:
            if observed and observed != int(count):
                raise SystemExit("Inconsistent parameterized test case count.")
            cases += int(count)
        elif observed:
            cases += observed
        elif ":" in name:
            raise SystemExit("Missing observed cases for parameterized test: " + name)
        else:
            cases += 1
    result = {"functions": len(matches), "cases_passed": cases, "failures": 0}
    if kind == "ios":
        if "** TEST SUCCEEDED **" not in log:
            raise SystemExit("Xcode did not report a passing test run.")
        ui = len(set(re.findall(r"Test Case '(.+?)' passed", log)))
        result = {"mapkit_functions": len(matches), "mapkit_cases_passed": result["cases_passed"],
                  "ui_cases_passed": ui, "cases_passed": result["cases_passed"] + ui, "failures": 0}
elif kind == "android":
    if not re.search(r"OK \(\d+ tests?\)", log):
        raise SystemExit("Android instrumentation did not report a passing run.")
    # -r reports each test end: 0 passed, -3 ignored, -4 assumption failure.
    statuses = re.findall(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", log)
    result = {"cases_passed": statuses.count("0"), "cases_skipped": statuses.count("-3") + statuses.count("-4"), "failures": 0}
    if not result["cases_passed"]:
        raise SystemExit("No individual passing instrumentation results found.")
else:
    raise SystemExit("Expected swift, ios or android")
output.write_text(json.dumps(result, indent=2)+"\n")
print(f"{kind}: {json.dumps(result)}; report: {output}")
