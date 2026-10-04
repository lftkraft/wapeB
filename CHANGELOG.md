# 📝 Changelog

All notable changes to **wapeB** will be documented in this file.

---

## [v1.0.13-alpha.3] - 2026-10-04

### 📦 1. Extensible Importer System & Importer API
* **Built-in Plugin Importers:** Added one-click automated database & file importers for:
  - **LiteBans:** Imports bans, mutes, warnings, kicks from SQLite (`litebans.sqlite`) or external MySQL/MariaDB databases.
  - **AdvancedBan:** Imports punishments and punishment history from SQLite (`AdvancedBan.db`) or MySQL.
  - **Vanilla Minecraft JSON:** Imports standard `banned-players.json` and `banned-ips.json` with duration and expiry calculation.
* **Extensible Java API:** Developers can register custom importers via `WapeBAPI#registerImporter(PunishmentImporter)` or perform direct batch imports with `WapeBAPI#importPunishments(List<Punishment>)`.
* **REST API Endpoints:** 
  - `GET /api/import/sources` - List all registered punishment importers.
  - `POST /api/import/execute` - Execute an import asynchronously via REST webhook/API.
* **In-Game Command:** Added `/wapeb import <source> [file/database]` with live progress report and tab completion.

### 👥 2. AltExempt Extended API & REST Endpoints
* **Full Java API Integration:** Added `WapeBAPI#getAllAltExempts()`, `WapeBAPI#getAltExemptDetails(UUID/player)`, and `WapeBAPI#setAltExempt(...)` returning complete audit records (`AltExemptInfo`).
* **REST API Endpoints:**
  - `GET /api/altexempt/list` - Query all currently alt-exempted accounts with metadata.
  - `GET /api/altexempt/check?player=...` - Instant check if a target is alt-exempt.
  - `POST /api/altexempt/set` - Programmatically add or remove alt exemptions from external web dashboards.

### ⚡ 3. Quick-Punish Templates & Shortcut System
* **Shortcut Codes:** Support for numeric and custom shortcuts (e.g., `#1`, `#2`, `$cheating`) directly in punishment commands (`/ban <player> #1`, `/mute <player> #spam`, `/warn <player> #toxicity`).
* **Silent Flag Inheritance:** Templates can specify `silent: true` in `templates.yml` to automatically execute silently without manually appending `-s`.
* **Dynamic Template API:** Create, modify, delete, and lookup templates at runtime via `WapeBAPI#saveTemplate(...)`, `WapeBAPI#deleteTemplate(...)`, and `WapeBAPI#findTemplate(...)`.
* **REST API Endpoints:**
  - `GET /api/templates/list` - Retrieve all templates or filter by category.
  - `GET /api/templates/get` - Fetch template details by key or shortcut.
  - `POST /api/templates/save` - Create or update templates remotely.
  - `POST /api/templates/delete` - Remove templates via REST.

### 🤖 4. Sentinel AI & Performance Enhancements
* **🔄 Multi-API Key Pool & Round-Robin Rotation:** Added `groq-api-keys` list support in `config.yml` with round-robin load balancing and instant zero-delay failover on `429 Too Many Requests`.
* **⚡ Intelligent Local Heuristic Pre-Filtering:** Added `sentinel.ai.pre-filter` to skip trivial, harmless chatter (< 3 chars, coordinates, numbers, emojies, gaming terms) locally with 0 ms overhead, saving 50–70% of API quotas.
* **🛡️ Prompt Injection & Jailbreak Protection:** Hardened system instructions to ignore user-injected commands, custom bypasses, and JSON overrides.
* **🧠 Context-Aware Chat History Analysis:** Added `sentinel.ai.context-lines` (default: 5) to supply conversation history from `ChatSnapshotManager` for accurate evaluation of ongoing disputes.
* **🚀 Default Model Optimization:** Defaulted to `llama-3.1-8b-instant` for ultra-low latency and 14,400 RPD free-tier limit.

---

## [v1.0.13-alpha.2] - 2026-10-04

### 📸 1. Automatic Chat Snapshot System
- **In-Memory Rolling Buffer:** Automatically tracks the last 300 chat messages globally with nanosecond precision and player context.
- **Incident Snapshot Creation:** Automatically captures and persists relevant chat history upon punishment execution into formatted, human-readable JSON files (`plugins/wapeB/snapshots/<id>.json`).
- **Human-Readable Formatting:** Snapshots now include formatted dates (`created_date`), per-message timestamps (`time`, `date`), and a direct `chat_log` string array for effortless staff inspection.
- **Flexible Retention Policy:** Added hour and day retention cleanup (`retention: "1h"`, `"12h"`, `"7d"`, `"30d"`), running asynchronously every 15 minutes with zero server tick impact.
- **Offline Delivery Queue:** If backend servers issue punishments while empty, snapshots are safely queued in `pending_snapshots/` and automatically flushed to Velocity upon player join.

### 🔗 2. Comprehensive Proof (Evidence) System
- **New `/proof` Command:** Added `/proof <set|remove|reset|check> <id> [url]` with full tab-completion.
- **Punishment Command Flag:** All punishment commands (`/ban`, `/mute`, `/kick`, `/warn`, `/banip`, `/muteip`) now accept `-proof:<url>` directly (e.g., `/mute Player 1h Toxicity -proof:https://imgur.com/...`).
- **Clean Clickable Links:** Proof URLs in chat (`/checkban`, `/checkmute`, `/proof check`) are cleanly clickable with browser opening support without intrusive hover text.
- **Full Ecosystem Integration:** Proof links seamlessly display across GUI menus, Discord Webhooks, Java API, and the REST Web API (`/api/punish/proof`).

### 🔄 3. Automatic Config & Language File Synchronization (Auto-Sync)
- **Seamless Upgrades:** Updating the plugin jar now automatically scans and merges all newly introduced configuration sections and message keys into existing files on disk.
- **Preserves Custom Settings:** Existing customized messages, prefixes, and database credentials remain 100% untouched while new features are added seamlessly.
- **Missing Key Fallback:** Safe fallback to default language strings prevents any `Missing message` errors.

### 🌐 4. Multi-Server & Velocity Cross-Server Synchronization
- **Server Identity:** Configurable `server-name` identifier per Spigot instance (`Lobby`, `szerver1`, `szerver2`, etc.).
- **Broadcast Scoping:** Added `broadcast.show-remote-punishments` configuration to toggle whether punishments targeting external servers broadcast locally.
- **Real-Time Network Sync:** Synchronized punishment broadcasts and chat snapshots across the network via the `wapeb:main` Plugin Messaging channel.

### ⚡ 5. Asynchronous Event & Threading Fixes (Paper/Purpur)
- **Async Event Compatibility:** `PlayerPunishEvent`, `PlayerUnpunishEvent`, `PlayerFreezeEvent`, `PlayerUnfreezeEvent`, and `LockdownToggleEvent` now dynamically detect and report their calling thread context (`super(!Bukkit.isPrimaryThread())`).
- Completely resolves `IllegalStateException: Event may only be triggered synchronously` when executing punishments on asynchronous database worker threads.

### 📜 6. Smart History Sorting & Pagination
- **Guaranteed Newest-First Order:** Resolved query order reversal so the most recent punishments (e.g., #17, #16) always appear on Page 1.
- **Page Header Indicators:** Added dynamic page tracking (`(%page%/%max_page%)`) and `%total%` placeholders in `/history`.

### 🌍 7. Complete Localization & UTF-8 Refresh
- Updated all 11 built-in language configurations (`hu`, `en`, `de`, `fr`, `es`, `pt`, `ru`, `ro`, `da`, `sv`, `custom`) with the latest Proof, Snapshot, and Console notification keys.
- Enforced clean UTF-8 encoding across Hungarian and international message files.

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
