"""Exercise the GUI class in an isolated sclang process without audio."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
sclang = Path(os.environ.get("SCLANG", "/Applications/SuperCollider.app/Contents/MacOS/sclang"))
library = sclang.parent.parent / "Resources/SCClassLibrary"
classes = Path(os.environ.get("REDMST_CLASSES", str(root)))
with tempfile.TemporaryDirectory(prefix="redmst-session-bar-") as directory:
    runtime = Path(directory)
    overrides = runtime / "SystemOverwrites"
    overrides.mkdir()
    (overrides / "NoPersonalStartup.sc").write_text("+ OSXPlatform { startupFiles { ^[] } }\n")
    config = runtime / "sclang.yaml"
    config.write_text("excludeDefaultPaths: true\nincludePaths:\n" + "".join(
        "  - " + json.dumps(str(path)) + "\n" for path in (library, classes, runtime)
    ))
    result = subprocess.run(
        [str(sclang), "-D", "-l", str(config), str(root / "tests/session-bar.scd")],
        env={**os.environ, "QT_QPA_PLATFORM": "cocoa" if sys.platform == "darwin" else "offscreen"},
        capture_output=True, text=True, timeout=30,
    )
    output = result.stdout + result.stderr
    print(output)
    if result.returncode or "PASS: session-bar" not in output or "ERROR:" in output:
        raise SystemExit(result.returncode or 1)
