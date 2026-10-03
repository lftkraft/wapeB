# 🔨 wapeB - Ultimate Minecraft Punishment System, Velocity Bridge & Developer API

[![Velocity](https://img.shields.io/badge/Velocity%20Proxy-Companion%20Bridge%20(wapeb--velocity)-6366f1?logo=velocity&logoColor=white)](https://github.com/lftkraft/wapeb-velocity)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)](https://github.com/lftkraft/wapeB)
[![Version](https://img.shields.io/badge/version-v1.0.13--alpha.1-orange?logo=github)](https://github.com/lftkraft/wapeB/releases)
[![JitPack](https://jitpack.io/v/lftkraft/wapeB.svg)](https://jitpack.io/#lftkraft/wapeB)
[![Java](https://img.shields.io/badge/Java-17%2B-red?logo=openjdk)](https://adoptium.net/)

**wapeB** is a complete, high-performance Minecraft punishment and moderation ecosystem built for **Paper/Spigot (1.18.2 – 1.21.x)** and powered by the official **`wapeb-velocity`** proxy companion bridge. 

It provides instant cross-server network synchronization (`wapeb:channel`), intelligent multi-server scoping (`activeserver` vs `server`), AI-driven automatic chat moderation (Sentinel), an advanced Java API, Bukkit Custom Events, dynamic command overrides, and a built-in HTTP REST Web API.

---

## ✨ Key Features

- ⚡ **Official Velocity Companion (`wapeb-velocity`)**: High-speed, non-blocking cross-server synchronization via native Minecraft plugin messaging (`wapeb:channel`) with proxy-level kick enforcement.
- 🌐 **Intelligent Multi-Server Scoping (`activeserver` vs `server`)**: Target specific sub-servers (`survival`, `server1,server2`) or enforce network-wide (`global`), while preserving the exact origin server audit trail.
- 🔨 **Complete Punishment Suite**: Ban, TempBan, IP-Ban, Temp-IP-Ban, Mute, TempMute, IP-Mute, Temp-IP-Mute, Warn, Kick, KickAll, Freeze, and Lockdown.
- 🤖 **Sentinel AI Auto-Moderation**: Integrates with Groq AI to detect toxicity, swearing, and chat violations automatically in real time with zero server lag.
- 🎨 **Included Cyberpunk Web Panel**: A modern Neon/Glassmorphism web interface with in-game 2FA login, Chart.js statistics, and live punishment management.
- 🥶 **Advanced Freeze / Screenshare System**: Prevents movement, PvP, block breaking, and item drops while frozen, with automatic logout enforcement.
- 🔒 **Server Lockdown Mode (`/lockdown`)**: Instantly restrict server entry during maintenance or bot raids with customizable kick messages.
- ⏱️ **Smart Dynamic Duration**: `%duration%` automatically calculates the exact remaining time until expiration in real time with ceiling rounding.
- ⚙️ **Dynamic Command Aliases**: Customize or translate any command (e.g. `/ban` → `/kitiltas`, `/mute` → `/nemit`) via `config.yml` or runtime API.
- 🔌 **Developer Java API & Custom Events**: Full event cancellation, executor name overrides, and seamless integration for external plugins (DiscordSRV, SyncCord, custom bots).
- 🌐 **Built-in HTTP REST Web API**: Remote punishment administration with `X-API-Key` authentication for web dashboards and external services.
- 📜 **Staff & Player History Tracking**: Track staff member performance (`/staffhistory`) and full player punishment history (`/history`).
- 👥 **Advanced Alt-Account & CIDR Detection**: Detect alternative accounts linked by IP or `/24` subnets with whitelist exemption support (`/alts`, `/altexempt`).
- 💾 **Multi-Storage Backend**: Supports MySQL / MariaDB (HikariCP connection pool), SQLite, and YAML storage.

---

## 📜 Commands & Permissions

| Command | Usage | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/ban` | `/ban <player/ip> [time] [reason] [-s] [-server]` | Ban or Temp-Ban a player | `wapeb.ban` |
| `/banip` | `/banip <player/ip> [time] [reason] [-s] [-server]` | IP/CIDR Subnet Ban | `wapeb.banip` |
| `/mute` | `/mute <player/ip> [time] [reason] [-s] [-server]` | Mute or Temp-Mute a player | `wapeb.mute` |
| `/muteip` | `/muteip <player/ip> [time] [reason] [-s] [-server]` | IP-Mute a player | `wapeb.muteip` |
| `/warn` | `/warn <player> [reason] [-s] [-server]` | Warn a player | `wapeb.warn` |
| `/kick` | `/kick <player> [reason] [-s]` | Kick a player from the server | `wapeb.kick` |
| `/kickall` | `/kickall [reason]` | Kick all non-staff players | `wapeb.kickall` |
| `/unban` | `/unban <player/ip> [reason] [-s]` | Unban a player or IP address | `wapeb.unban` |
| `/unmute` | `/unmute <player/ip> [reason] [-s]` | Unmute a player or IP address | `wapeb.unmute` |
| `/unwarn` | `/unwarn <player> [id/all]` | Remove warnings from a player | `wapeb.unwarn` |
| `/freeze` | `/freeze <player> [reason]` | Freeze/unfreeze player for screenshare | `wapeb.freeze` |
| `/checkban` | `/checkban <player/ip>` | Inspect active ban status | `wapeb.checkban` |
| `/checkmute` | `/checkmute <player/ip>` | Inspect active mute status | `wapeb.checkmute` |
| `/history` | `/history <player>` | View full punishment history of a player | `wapeb.history` |
| `/staffhistory` | `/staffhistory <staff>` | View staff member punishment actions | `wapeb.staffhistory` |
| `/alts` | `/alts <player>` | View alternative accounts linked by IP/CIDR | `wapeb.alts` |
| `/altexempt` | `/altexempt <player> [add/remove]` | Whitelist accounts from alt detection | `wapeb.altexempt` |
| `/lockdown` | `/lockdown [on/off] [reason]` | Toggle server lockdown mode | `wapeb.lockdown` |
| `/punish-rollback` | `/punish-rollback <id>` | Undo specific punishment | `wapeb.rollback` |
| `/punish` | `/punish <player>` | Open punishment GUI / template selection | `wapeb.punish` |
| `/wapeb` | `/wapeb [reload/status]` | Main plugin management command | `wapeb.admin` |

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
    compileOnly 'com.github.lftkraft:wapeB:v1.0.13-alpha.1'
}
```

### Maven (`pom.xml`)
```xml
<dependencies>
    <dependency>
        <groupId>com.github.lftkraft</groupId>
        <artifactId>wapeB</artifactId>
        <version>v1.0.13-alpha.1</version>
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

// Issue a network-wide ban
api.banPlayer("PlayerName", "Cheating detected", "Console", 86400000L, false, false, "global");

// Issue a server-specific mute
api.mutePlayer("PlayerName", "Chat Spam", "Admin", 3600000L, false, false, "survival");
```

---

## 🌐 HTTP REST Web API

wapeB includes a built-in HTTP REST server for remote web dashboards and external bots.

- **Authentication Header**: `X-API-Key: YOUR_API_KEY_HERE`
- `GET /api/player/checkban?player=PlayerName`
- `GET /api/player/checkmute?player=PlayerName`
- `GET /api/player/punishments?player=PlayerName`
- `GET /api/punish/active`
- `POST /api/punish/execute` (supports `active_server` & `origin_server`)
- `POST /api/punish/remove`
- `POST /api/lockdown`

---

## 📜 Compatibility & Support
- **Supported Platforms**: Paper, Purpur, Spigot (1.18.2 – 1.21.x) & Velocity Proxy
- **Java Requirement**: Java 17+
- **Source Code & Issue Tracker**: [GitHub Repository](https://github.com/lftkraft/wapeB)
