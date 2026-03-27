# AGENTS Guide for `try-again`

## Project shape (what is actually wired)
- Fabric mod entrypoint is `com.minhduong.TryAgain` (`src/main/java/com/minhduong/TryAgain.java`) via `fabric.mod.json`.
- Current live feature set is authentication-only: `/login` + `/register` and join/leave auth enforcement.
- `TryAgain.onInitialize()` initializes `PlayerDataManager`, registers commands, and binds join/leave + server-start lifecycle callbacks.
- Many classes are scaffolds and are **not** registered yet (`BalanceCommand`, `BuyCommand`, `SellCommand`, `TpaCommand`, `EconomyManager`, `MarketManager`, `TpaManager`, `HudManager`, partial `HomeCommand`).

## Core runtime flow
- Join flow (`AuthEventHandler.onPlayerJoin`): load token file, start auth session, freeze player, prompt login/register.
- Auth timeout is handled in one global `ServerTickEvents.END_SERVER_TICK` handler (registered once with `tickRegistered`).
- Unauthenticated players are forced to `SPECTATOR` and teleported back to join position every auth tick.
- Expired sessions are kicked using a hardcoded Vietnamese message.
- Successful `/login` or `/register` ends session, marks user authenticated in-memory, and restores `SURVIVAL`.

## Data boundaries and persistence
- Account store: `run/config/tryagain/players.json` via `PlayerDataManager`.
- Token store: `run/config/tryagain/token.json` via `TokenConfig`; tokens are consumed and persisted immediately.
- Keys are normalized to lowercase usernames for map/set lookups.
- Passwords are currently plaintext in JSON; preserve behavior unless explicitly changing auth model.
- Session/auth state is memory-only (`SessionManager`, `authenticated` set) and is cleared on disconnect.

## Build/run workflow
- Gradle + Fabric Loom project targeting Java 21 (`build.gradle`, `gradle.properties`).
- Typical local commands (Windows PowerShell):
  - `./gradlew.bat build`
  - `./gradlew.bat runServer`
- Runtime test environment is under `run/` (mods, world, logs, config).

## Integration points
- Compile-time APIs include Geyser and Floodgate (`build.gradle`), and runtime jars exist in `run/mods/`.
- Geyser/Floodgate behavior is config-driven in `run/config/Geyser-Fabric/config.yml` and `run/config/floodgate/config.yml`.
- Current mod code does not call Geyser/Floodgate APIs directly; integration is operational (same server), not code-coupled.

## Project-specific coding patterns
- Commands use Brigadier registration static methods (`register(dispatcher)`), often with inline auth checks.
- User-facing text is centralized in `util/Messages.java` and uses section-sign formatting (`§`), mostly Vietnamese strings.
- JSON persistence uses Gson pretty-printing with explicit UTF-8 readers/writers.
- Thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) are used for shared runtime state.

## Agent guardrails for edits
- If adding a new command, wire it in `TryAgain.onInitialize()` or it will never execute.
- Keep auth gating consistent: unauthenticated users should be blocked and receive `Messages.MUST_LOGIN`.
- Treat `run/config/tryagain/*.json` as persistent contract files; avoid schema churn without migration logic.
- When changing tick/session behavior, verify join-freeze, timeout kick, and login/register unfreeze still work end-to-end.

