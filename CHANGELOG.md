# 📝 Changelog

All notable changes to **wapeB** will be documented in this file.

---

## [v1.0.13-alpha.2] - alpha.2-1.0.13-aiupdate (2026-10-04)

### 🤖 Sentinel AI & Performance Enhancements

* **🔄 Multi-API Key Pool & Round-Robin Rotation:**
  * Added support for `groq-api-keys` list in `config.yml`. The plugin automatically balances chat analysis requests across multiple Groq API keys in a round-robin rotation.
  * Instant failover on `429 Too Many Requests`: automatically switches to the next available API key in the pool with zero delay.
* **⚡ Intelligent Local Heuristic Pre-Filtering:**
  * Added `sentinel.ai.pre-filter` to intercept and skip trivial messages locally with 0 ms overhead (saving 50–70% of API quotas).
  * Automatically filters out short chatter (< 3 chars like `k`, `xd`, `?`), coordinates/numbers (`100 64 -200`, `500k`), punctuation/emojis (`???`, `:)`, `:D`, `^^`), and common harmless gaming terms (`gg`, `wp`, `ez`, `szia`, `oks`, `ty`, etc.).
* **🛡️ Prompt Injection & Jailbreak Protection:**
  * Hardened system prompt instructions to completely ignore user-injected instructions (e.g. `ignore previous instructions`, `do not mute`, JSON overrides), preventing LLM manipulation.
* **🧠 Context-Aware Chat Analysis:**
  * Added `sentinel.ai.context-lines` (default: 5) to supply recent conversation history from `ChatSnapshotManager`, allowing the AI to understand multi-line context, ongoing arguments, and contextual toxicity.
* **🚀 Default Model & Parameter Optimization:**
  * Defaulted to `llama-3.1-8b-instant` for ultra-fast latency and high free-tier daily rate limits (14,400 RPD).

---

## [v1.0.13-alpha.1] - 2026-10-03

### 🚀 New Features & Enhancements

#### 1. Multi-Server Scope Separation (`activeserver` vs `server`)
- **`activeserver` (Enforcement Scope)**: Defines the server(s) where a punishment is active.
  - Supports `global` for network-wide enforcement.
  - Supports specific server names (e.g. `survival`, `szerver2`).
  - Supports comma-separated multi-server targets (e.g. `szerver1,szerver2`).
- **`server` (Origin Server)**: Accurately records where the punishment was initiated from (e.g. `lobby`, `szerver1`, `web`).
- **Automatic Database Migrations**: Added `active_server VARCHAR(255)` and `server VARCHAR(64)` columns to SQLite and MySQL schemas.

#### 2. Velocity Proxy Integration (`wapeb-velocity`)
- Released the official companion plugin `wapeb-velocity-1.0.jar` for Velocity proxies.
- Instant, non-blocking packet forwarding across backend servers via Plugin Messaging channel `wapeb:channel`.
- Proxy-level disconnects for network-wide bans and kicks.
- Smart deduplication to prevent duplicate chat and console broadcasts.

#### 3. Java Developer API (`WapeBAPI`) Overhauls
- Added overloaded methods supporting target scope and origin server parameters:
  - `banPlayer(target, reason, executor, duration, silent, ipBan, activeServer, server)`
  - `mutePlayer(target, reason, executor, duration, silent, ipMute, activeServer, server)`
  - `warnPlayer(target, reason, executor, silent, activeServer, server)`
  - `kickPlayer(target, reason, executor, silent, activeServer, server)`
- Added `getActiveServer()`, `setActiveServer(String)`, `getServer()`, and `setServer(String)` to `PlayerPunishEvent`.
- Query methods (`getPunishments`, `getActiveBan`, `getHistory`, etc.) return full `Punishment` instances containing both `getActiveServer()` and `getServer()`.

#### 4. HTTP REST Web API Extensions
- `/api/punish/execute` now accepts `active_server` (target scope) and `origin_server` / `server` (origin).
- `/api/player/punishments`, `/api/player/checkban`, `/api/player/checkmute`, and `/api/punish/active` JSON responses now serialize `activeServer` and `server`.

#### 5. Message Placeholders
- `%server%`: Origin server name.
- `%activeserver%` / `%active_server%`: Target enforcement scope.

---

## [v1.0.12] - 2026-09-11
- Ceiling duration rounding for exact expiration display.
- Case-insensitive smart player lookup.
- Warn-action escalation threshold support.
- CIDR `/24` and wildcard `192.168.1.*` subnet ban matching.
- Punishment templates support (`$template`).
