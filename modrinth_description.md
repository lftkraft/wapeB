# 🔨 wapeB - Ultimate Minecraft Punishment System, AI Sentinel & Developer API

**wapeB** is a modern, high-performance Paper/Spigot punishment management plugin built for Minecraft **1.18.2 – 1.21.x**. It combines advanced punishment features, AI-powered automatic chat moderation, a comprehensive Java API, Bukkit Custom Events, dynamic command overrides, and a built-in HTTP REST Web API with an included **Cyberpunk Web Panel**.

---

## ✨ Key Features

- 🔨 **Complete Punishment Suite**: Ban, TempBan, IP-Ban, Temp-IP-Ban, Mute, TempMute, IP-Mute, Temp-IP-Mute, Warn, Kick, KickAll, Freeze, and Lockdown.
- 🤖 **Sentinel AI Auto-Moderation**: Integrates with Groq AI to detect toxicity, swearing, and chat violations automatically in real time with zero server lag.
- 🎨 **Included Cyberpunk Web Panel**: A modern Neon/Glassmorphism web interface featuring in-game 2FA login, Chart.js statistics, and live punishment management.
- 🥶 **Advanced Freeze / Screenshare System**: Prevents movement, PvP, block breaking, and item drops while frozen, with automatic logout enforcement.
- 🔒 **Server Lockdown Mode (`/lockdown`)**: Instantly restrict server entry during maintenance or bot raids with customizable kick messages.
- ⏱️ **Smart Dynamic Duration**: `%duration%` automatically calculates the exact remaining time until expiration in real time.
- ⚙️ **Dynamic Command Aliases**: Customize or translate any command (e.g. `/ban` → `/kitiltas`, `/mute` → `/nemit`) via `config.yml` or runtime API.
- 🔌 **Developer Java API & Custom Events**: Full event cancellation, executor name overrides, and seamless integration for external plugins (DiscordSRV, SyncCord, custom bots).
- 🌐 **Built-in HTTP REST Web API**: Remote punishment administration with `X-API-Key` authentication for web dashboards and external services.
- 📜 **Staff & Player History Tracking**: Track staff member performance (`/staffhistory`) and full player punishment history (`/history`).
- 👥 **Advanced Alt-Account Detection**: Detect alternative accounts linked by IP with whitelist exemption support (`/alts`, `/altexempt`).
- 💾 **Dual Storage Backend**: Supports SQLite database and YAML storage.

---

## 🖥️ Included Cyberpunk Web Dashboard

wapeB comes with a standalone, full-featured web dashboard located in the `web/` folder of the plugin jar:
- **In-game 2-Factor Authentication (2FA)** / OTP verification codes for staff members.
- **Interactive Charts & Analytics** powered by Chart.js.
- **Live Punishment Management**: Issue and revoke bans, mutes, and warnings directly from your browser.
- **Player & Alt Account Lookup** with GeoIP country mapping.

---

## 📜 Commands & Permissions

| Command | Usage | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/ban` | `/ban <player/ip> [time] [reason] [-s]` | Ban or Temp-Ban a player | `wapeb.ban` |
| `/banip` | `/banip <player/ip> [time] [reason] [-s]` | IP-Ban a player | `wapeb.banip` |
| `/mute` | `/mute <player/ip> [time] [reason] [-s]` | Mute or Temp-Mute a player | `wapeb.mute` |
| `/muteip` | `/muteip <player/ip> [time] [reason] [-s]` | IP-Mute a player | `wapeb.muteip` |
| `/warn` | `/warn <player> [reason] [-s]` | Warn a player | `wapeb.warn` |
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
| `/alts` | `/alts <player>` | View alternative accounts linked by IP | `wapeb.alts` |
| `/altexempt` | `/altexempt <player> [add/remove]` | Whitelist accounts from alt detection | `wapeb.altexempt` |
| `/lockdown` | `/lockdown [on/off] [reason]` | Toggle server lockdown mode | `wapeb.lockdown` |
| `/punishrollback` | `/punishrollback <staff> <time>` | Undo punishments issued by staff | `wapeb.rollback` |
| `/punish` | `/punish <player>` | Open punishment GUI / template selection | `wapeb.punish` |
| `/wapeb` | `/wapeb [reload/status]` | Main plugin management command | `wapeb.admin` |

---

## ☕ Developer API & JitPack Setup

> 📖 **Full API Reference:** [GitHub API Documentation](https://github.com/lftkraft/wapeB/blob/main/API_DOCUMENTATION.md)

### 🐘 Gradle (Groovy DSL - `build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.13-alpha.1'
}
```

### 🐘 Gradle (Kotlin DSL - `build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.lftkraft:wapeB:v1.0.13-alpha.1")
}
```

### 📦 Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.lftkraft</groupId>
        <artifactId>wapeB</artifactId>
        <version>v1.0.13-alpha.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 📄 `plugin.yml`
```yaml
depend: [wapeB]
# or for soft dependency:
# softdepend: [wapeB]
```

---

## 💻 Java API Examples

### Accessing the API
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;
import dev.azuyo.wapeB.api.WapeBAPIProvider;

// Option 1 (Direct):
WapeBAPI api = WapeB.getApi();

// Option 2 (Provider):
WapeBAPI api = WapeBAPIProvider.getAPI();
```

### Querying Status & Active Punishments
```java
// Check if player or any linked alt account is banned/muted
boolean isBanned = api.isBannedForPlayerOrAlt("PlayerName");
boolean isMuted = api.isMutedForPlayerOrAlt("PlayerName");
boolean isFrozen = api.isFrozen("PlayerName");

// Get active punishment details
Punishment activeBan = api.getActiveBanForPlayerOrAlt("PlayerName");
Punishment activeMute = api.getActiveMuteForPlayerOrAlt("PlayerName");

// Query alt accounts & history
List<String> alts = api.getAlts("PlayerName");
List<Punishment> history = api.getPunishments("PlayerName");
```

### Executing Punishments Programmatically
```java
// Ban player (duration in milliseconds, -1 for permanent, silent flag)
api.banPlayer("PlayerName", "Cheating detected", "Console", 86400000L, false, false);

// Mute player
api.mutePlayer("PlayerName", "Spamming", "Admin", 3600000L, false, false);

// Warn player
api.warnPlayer("PlayerName", "Swearing in chat", "Staff", false);

// Kick player
api.kickPlayer("PlayerName", "AFK in combat zone", "System", false);

// Freeze / Unfreeze player
api.freezePlayer("PlayerName", "Staff");
api.unfreezePlayer("PlayerName");

// Record external staff action into history
api.recordStaffAction("StaffMember", "TargetPlayer", PunishmentType.MUTE, "Toxicity", 3600000L);
```

### Bukkit Custom Events
Listen to wapeB events in your plugin to cancel actions or integrate with custom bots:
- `PlayerPunishEvent` (Cancellable)
- `PlayerUnpunishEvent` (Cancellable)
- `PlayerFreezeEvent` (Cancellable)
- `PlayerUnfreezeEvent` (Cancellable)
- `LockdownToggleEvent` (Cancellable)

```java
@EventHandler
public void onPlayerPunish(PlayerPunishEvent event) {
    String player = event.getTargetName();
    String reason = event.getReason();
    String operator = event.getOperator();
    PunishmentType type = event.getType();

    // Example: send alert to custom Discord webhook or cancel if exempt
    if (player.equalsIgnoreCase("AdminPlayer")) {
        event.setCancelled(true);
    }
}
```

---

## 🌐 HTTP REST Web API

wapeB includes a built-in HTTP server for remote web dashboards and bot integrations:

- **Authentication Header**: `X-API-Key: YOUR_API_KEY_HERE`

### Key Endpoints:
- `GET /api/player/checkban?player=PlayerName`
- `GET /api/player/checkmute?player=PlayerName`
- `GET /api/player/punishments?player=PlayerName`
- `GET /api/alts?player=PlayerName`
- `GET /api/stats`
- `POST /api/punish/execute`
- `POST /api/lockdown`
- `POST /api/login/request` & `POST /api/login/verify` (In-game 2FA)

---

## 📜 Compatibility & Requirements
- **Supported Server Software**: Paper, Purpur, Spigot (1.18.2 – 1.21.x)
- **Java Requirement**: Java 17+
- **GitHub Repository**: [lftkraft/wapeB](https://github.com/lftkraft/wapeB)
