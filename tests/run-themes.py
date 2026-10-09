"""Exercise shared RedMst GUI themes in an isolated sclang process."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
sclang = Path(os.environ.get("SCLANG", "/Applications/SuperCollider.app/Contents/MacOS/sclang"))
library = sclang.parent.parent / "Resources/SCClassLibrary"
classes = Path(os.environ.get("REDMST_CLASSES", str(root)))

with tempfile.TemporaryDirectory(prefix="redmst-themes-") as directory:
    runtime = Path(directory)
    overrides = runtime / "SystemOverwrites"
    overrides.mkdir()
    (overrides / "NoPersonalStartup.sc").write_text("+ OSXPlatform { startupFiles { ^[] } }\n")
    shutil.copyfile(
        root / "tests/RedMstThemeMockPlayer.sc.template",
        runtime / "RedMstThemeMockPlayer.sc",
    )
    config = runtime / "sclang.yaml"
    config.write_text(
        "excludeDefaultPaths: true\nincludePaths:\n"
        + "".join("  - " + json.dumps(str(path)) + "\n" for path in (library, classes, runtime))
    )
    environment = {
        **os.environ,
        "QT_QPA_PLATFORM": "cocoa" if sys.platform == "darwin" else "offscreen",
    }
    image_directory = environment.get("REDMST_THEME_IMAGES")
    if image_directory:
        Path(image_directory).mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        [str(sclang), "-D", "-l", str(config), str(root / "tests/themes.scd")],
        env=environment,
        capture_output=True,
        text=True,
        timeout=90,
    )
    output = result.stdout + result.stderr
    print(output)
    if result.returncode or "PASS: shared GUI themes" not in output or "ERROR:" in output:
        raise SystemExit(result.returncode or 1)
    if image_directory:
        expected_images = {
            f"{class_name}-{theme}.png"
            for class_name in (
                "RedMstGUI",
                "RedMstGUI2",
                "RedMstGUI3",
                "RedMstSessionGUI",
                "RedMstArrangementGUI",
            )
            for theme in ("dark", "light")
        }
        missing_images = sorted(
            name
            for name in expected_images
            if not (Path(image_directory) / name).is_file()
            or (Path(image_directory) / name).stat().st_size == 0
        )
        if missing_images:
            raise SystemExit("Missing theme screenshots: " + ", ".join(missing_images))
