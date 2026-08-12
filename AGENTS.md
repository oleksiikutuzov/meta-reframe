# Repository Guidelines

## Project Structure & Module Organization

This repository is an independent Yocto/OpenEmbedded layer. Its current layout is:

- `conf/layer.conf` — layer registration, dependencies, and Wrynose compatibility.
- `recipes-core/images/` — image recipes, beginning with the hardware bring-up target.
- `kas/` — pinned repositories and canonical build configuration.
- `docs/` — dependency mapping and future hardware/boot documentation.
- `recipes-*/<component>/files/` — patches, services, and configuration owned by a recipe.

Keep files close to the recipe that owns them. Add shared BitBake classes under `classes/` and Python helpers under `lib/` only when reuse justifies them. Update `README.md` whenever supported hardware or build configuration changes.

## Build, Test, and Development Commands

Use the pinned kas workflow documented in `README.md`:

```sh
KAS_WORK_DIR="$PWD" kas checkout meta-reframe/kas/reframe.yml
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml -c 'bitbake -p reframe-image-minimal'
KAS_WORK_DIR="$PWD" kas build meta-reframe/kas/reframe.yml
```

Run these commands from the parent `reframe-yocto/` project directory so kas
checks out all layers beside `meta-reframe/`. The first two commands validate
repository resolution and metadata parsing. The final command performs the
approximately 66 GB full Pi Zero 2 W image build and must run locally, not on a
standard GitHub-hosted runner. Build every affected recipe for each supported
machine before submitting changes.

## Coding Style & Naming Conventions

Use four spaces in BitBake and Python files; do not use tabs. Name recipes `<component>_<version>.bb`, use `<component>_%.bbappend` only when intentionally version-independent, and prefix patches descriptively, such as `0001-fix-startup-order.patch`. Keep assignments readable, prefer `PACKAGECONFIG` for optional features, and use machine overrides only for genuine hardware differences.

## Testing Guidelines

No automated test suite or coverage target exists yet. The baseline is a clean metadata parse and a successful affected recipe or image build. For hardware changes, document target-side checks and tested board revisions. Current bring-up checks include `/dev/i2c*`, `/dev/spidev*`, camera/media devices, relevant `dmesg` output, and successful systemd startup.

## Commit & Pull Request Guidelines

Use concise, imperative, scoped subjects, for example `images: add camera diagnostics`. Keep each commit limited to one logical milestone. Commits substantially created with an AI agent must identify that agent with a Git trailer, for example `Assisted-by: Codex`. Pull requests should explain motivation, list exact layer revisions, tested machines, and commands, link related issues, and call out licensing or compatibility changes. Include logs for failures and screenshots only for visible UI changes.

## Integration Constraints

Do not fork upstream reFrame or install software with target-side `pip`. Carry minimal integration patches in this layer, pin source revisions, keep `/usr/lib/reframe` immutable, and store persistent application data under `/var/lib/reframe`. Establish camera support independently before packaging the application; keep PiSugar integration separate from camera bring-up.
