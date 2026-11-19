## Godot Usage Rules (proot Debian)

- NEVER launch the Godot editor or run the game with a window. No `godot`, `godot -e`, or `godot --path .` without headless mode. The proot environment has no display and the GUI will hang or crash.
- ALWAYS pass `--headless` for every Godot command.
- Allowed commands:
  - Import/reimport resources: `godot --headless --path . --import`
  - Run a script: `godot --headless --path . --script res://tools/check.gd`
  - Validate scripts (parse check): `godot --headless --path . --check-only --script res://path/to/file.gd`
  - Export: `godot --headless --path . --export-release "Android" build/game.apk`
- Always add a timeout so nothing hangs: `timeout 120 godot --headless ...`
- Do not try to test visuals, input, or gameplay. Verify by reading code, running headless scripts, and checking logs for errors.
- If something needs the editor or a real device (UI, Android install, ads), stop and tell me what to test manually instead of attempting it.
- Do not install X11, VNC, Xvfb, or any display tooling to work around this.
