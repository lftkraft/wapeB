# 🔨 wapeB - Ultimate Minecraft Punishment System, AI Sentinel & Developer API
newest: v1.0.14-alpha.1

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)](https://github.com/lftkraft/wapeB)
[![Discord](https://img.shields.io/badge/Discord-Community-5865F2?logo=discord&logoColor=white)](https://dc.coolnw.eu)
[![Velocity](https://img.shields.io/badge/Velocity%20Proxy-Companion%20Bridge%20(wapeb--velocity)-6366f1?logo=velocity&logoColor=white)](https://github.com/lftkraft/wapeb-velocity)
[![Version](https://img.shields.io/github/v/tag/lftkraft/wapeB?label=Version)](https://github.com/lftkraft/wapeB/releases)
[![JitPack](https://jitpack.io/v/lftkraft/wapeB.svg)](https://jitpack.io/#lftkraft/wapeB)
[![Java](https://img.shields.io/badge/Java-17%2B-red?logo=openjdk)](https://adoptium.net/)

**wapeB** is a modern, high-performance Paper/Spigot punishment management system built for Minecraft **1.18.2 – 1.21.x** with full **Velocity Proxy** support. It combines an advanced punishment suite, AI-powered automatic chat moderation, multi-server scoping, an extensible database importer framework, a comprehensive Java API, Bukkit Custom Events, dynamic command overrides, and a built-in HTTP REST Web API.

---

## ✨ Key Features

- 🔨 **Complete Punishment Suite**: Ban, TempBan, IP-Ban, Temp-IP-Ban, Mute, TempMute, IP-Mute, Temp-IP-Mute, **ShadowMute** (Ghost Mute), Temp-ShadowMute, IP-ShadowMute, Warn, Kick, KickAll, Freeze, and Lockdown.
- 👻 **Shadow-Mute (Szellem-Némítás) Suite**: Silently mute toxic players without alerting them. The shadow-muted player sees their messages appear normally in chat, while messages are filtered for others and staff receive live violation notifications.
- 👥 **Alt-Account Audit & Join Alerts**: Instant MiniMessage alerts on player join when alt accounts are detected (with customizable click actions, hover details, and filter options).
- 📦 **Extensible Importers & Auto-Detection**: One-click automated database/file migration for **LiteBans** (SQLite/MySQL), **AdvancedBan** (SQLite/MySQL), and **Vanilla JSON** (`banned-players.json`, `banned-ips.json`).
- ⚡ **Velocity Proxy Support & Companion Plugin**: Instant cross-server plugin messaging (`wapeb:channel`) and proxy-level disconnects with `wapeb-velocity`.
- 🔗 **Evidence & Proof System (`/proof`)**: Attach clickable screenshot/video proof URLs directly to punishments via commands (`-proof:<url>`), GUI, Webhook, Java API, or REST.
- 📸 **Automated Chat Snapshots**: In-memory rolling buffer automatically captures and freezes relevant chat history upon punishment into human-readable JSON files with retention cleanup.
- ⚡ **Quick-Punish Templates & Shortcuts**: Fast numeric and custom command shortcuts (e.g. `/ban Player #1`, `/mute Player #spam`, `/warn Player #toxicity`) with automatic silent flag inheritance.
- 🌐 **Multi-Server Scoping (`activeserver` vs `server`)**: Target specific servers (`survival`, `server1,server2`) or apply network-wide (`global`), while preserving the origin server audit trail.
- 🤖 **Sentinel AI Auto-Moderation**: Multi-API key pool with round-robin failover, local heuristic pre-filtering, and Groq AI chat violation analysis with zero server lag.
- 🎨 **Included Cyberpunk Web Panel**: A modern Neon/Glassmorphism web interface with in-game 2FA login, Chart.js statistics, and live punishment management.
- 🥶 **Advanced Freeze / Screenshare System**: Prevents movement, PvP, block breaking, and item drops while frozen, with automatic logout enforcement.
- 🔒 **Server Lockdown Mode (`/lockdown`)**: Instantly restrict server entry during maintenance or bot raids with customizable kick messages.
- ⏱️ **Smart Dynamic Duration**: `%duration%` automatically calculates the exact remaining time until expiration in real time with ceiling rounding.
- ⚙️ **Dynamic Command Aliases**: Customize or translate any command (e.g. `/ban` → `/kitiltas`, `/mute` → `/nemit`) via `config.yml` or runtime API.
- 🔌 **Developer Java API & Custom Events**: Full event cancellation, executor name overrides, extensible importer registration, and seamless integration for external plugins (DiscordSRV, SyncCord, custom bots).
- 🌐 **Built-in HTTP REST Web API**: Remote punishment administration with `X-API-Key` authentication for web dashboards and external services.
- 📜 **Staff & Player History Tracking**: Track staff member performance (`/staffhistory`) and full player punishment history (`/history`).
- 💾 **Multi-Storage Backend**: Supports MySQL / MariaDB (HikariCP connection pool), SQLite, and YAML storage.

---

## 📜 Commands & Permissions

| Command | Usage | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/ban` | `/ban <player/ip> [time] [reason/#code] [-s] [-server] [-proof:url]` | Ban or Temp-Ban a player | `wapeb.ban` |
| `/banip` | `/banip <player/ip> [time] [reason/#code] [-s] [-server] [-proof:url]` | IP/CIDR Subnet Ban | `wapeb.banip` |
| `/mute` | `/mute <player/ip> [time] [reason/#code] [-s] [-server] [-proof:url]` | Mute or Temp-Mute a player | `wapeb.mute` |
| `/muteip` | `/muteip <player/ip> [time] [reason/#code] [-s] [-server] [-proof:url]` | IP-Mute a player | `wapeb.muteip` |
| `/shadowmute` | `/shadowmute <player> [time] [reason/#code] [-s] [-ip] [-server] [-proof:url]` | Shadow-mute player (ghost mute) | `wapeb.shadowmute` |
| `/shadowmuteip` | `/shadowmuteip <player/ip> [time] [reason/#code] [-s] [-server] [-proof:url]` | IP Shadow-mute player | `wapeb.shadowmuteip` |
| `/warn` | `/warn <player> [reason/#code] [-s] [-server] [-proof:url]` | Warn a player | `wapeb.warn` |
| `/kick` | `/kick <player> [reason/#code] [-s] [-server] [-proof:url]` | Kick a player from the server | `wapeb.kick` |
| `/kickall` | `/kickall [reason]` | Kick all non-staff players | `wapeb.kickall` |
| `/proof` | `/proof <set\|remove\|reset\|check> <id> [url]` | Inspect, set, or remove punishment evidence URL | `wapeb.proof` |
| `/unban` | `/unban <player/ip> [reason] [-s]` | Unban a player or IP address | `wapeb.unban` |
| `/unmute` | `/unmute <player/ip> [reason] [-s]` | Unmute or un-shadowmute a player | `wapeb.unmute` |
| `/unwarn` | `/unwarn <player> [id/all]` | Remove warnings from a player | `wapeb.unwarn` |
| `/freeze` | `/freeze <player> [reason]` | Freeze/unfreeze player for screenshare | `wapeb.freeze` |
| `/checkban` | `/checkban <player/ip>` | Inspect active ban status | `wapeb.checkban` |
| `/checkmute` | `/checkmute <player/ip>` | Inspect active mute & shadowmute status | `wapeb.checkmute` |
| `/history` | `/history <player>` | View full punishment history of a player | `wapeb.history` |
| `/staffhistory` | `/staffhistory <staff>` | View staff member punishment actions | `wapeb.staffhistory` |
| `/alts` | `/alts <player>` | View alternative accounts linked by IP/CIDR | `wapeb.alts` |
| `/altexempt` | `/altexempt <player> [add/remove]` | Whitelist accounts from alt detection | `wapeb.altexempt` |
| `/lockdown` | `/lockdown [on/off] [reason]` | Toggle server lockdown mode | `wapeb.lockdown` |
| `/punish-rollback` | `/punish-rollback <id>` | Undo specific punishment | `wapeb.rollback` |
| `/punish` | `/punish <player>` | Open punishment GUI / template selection | `wapeb.punish` |
| `/wapeb` | `/wapeb <reload\|unlink\|import>` | Main plugin management & import command | `wapeb.admin` |

---

## ☕ Developer API & JitPack Setup

> 📖 **Full API Reference:** [GitHub API Documentation](https://github.com/lftkraft/wapeB/blob/main/API_DOCUMENTATION.md)

### Gradle (`build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.14-alpha.1'
}
```

### Maven (`pom.xml`)
```xml
<dependencies>
    <dependency>
        <groupId>com.github.lftkraft</groupId>
        <artifactId>wapeB</artifactId>
        <version>v1.0.14-alpha.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Java API Example
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;

WapeBAPI api = WapeB.getApi();

// Check if a player or alt account is banned
boolean isBanned = api.isBannedForPlayerOrAlt("PlayerName");

// Issue a network-wide ban with proof attachment
api.banPlayer("PlayerName", "Cheating detected", "Console", 86400000L, false, false, "global", "lobby", "https://youtu.be/proof123");

// Issue a server-specific mute
api.mutePlayer("PlayerName", "Chat Spam", "Admin", 3600000L, false, false, "survival");

// Quick-punish with pre-defined template
api.punishWithTemplate(targetUuid, "ban", "cheating", "Admin", false);
```

---

## 🌐 HTTP REST Web API

wapeB includes a built-in HTTP REST server for remote web dashboards and external bots.

- **Authentication Header**: `X-API-Key: YOUR_API_KEY_HERE`
- `GET /api/player/checkban?player=PlayerName`
- `GET /api/player/checkmute?player=PlayerName`
- `GET /api/player/punishments?player=PlayerName`
- `GET /api/punish/active`
- `GET /api/punish/proof?id=105`
- `GET /api/punish/snapshot?id=105`
- `GET /api/player/recentchat?player=PlayerName&limit=30`
- `GET /api/import/sources`
- `POST /api/import/execute`
- `GET /api/altexempt/list`
- `GET /api/altexempt/check?player=PlayerName`
- `POST /api/altexempt/set`
- `GET /api/templates/list`
- `GET /api/templates/get?key=cheating`
- `POST /api/templates/save`
- `POST /api/templates/delete`
- `POST /api/punish/execute` (supports `active_server`, `origin_server`, `proof`)
- `POST /api/punish/remove`
- `POST /api/lockdown`

---

## 📜 Compatibility & Support
- **Discord Community**: [dc.coolnw.eu](https://dc.coolnw.eu)
- **Supported Platforms**: Paper, Purpur, Spigot (1.18.2 – 1.21.x) & Velocity Proxy
- **Java Requirement**: Java 17+
- **Source Code & Issue Tracker**: [GitHub Repository](https://github.com/lftkraft/wapeB)
- minecraft punishment plugin, free litebans alternative, velocity ban plugin, paper ban plugin, ai chat moderation, freeze screenshare plugin, minecraft rest api punishment
