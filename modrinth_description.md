# 🔨 wapeB - Ultimate Minecraft Punishment System, AI Sentinel & Developer API

**wapeB** is a modern, high-performance Paper/Spigot punishment management plugin built for Minecraft **1.18.2 – 1.21.x**. It combines advanced punishment features, AI-powered automatic chat moderation, a powerful Java API, Bukkit Custom Events, dynamic command overrides, and a built-in HTTP REST Web API.

---

## ✨ Key Features

- 🔨 **Complete Punishment Suite**: Ban, TempBan, IP-Ban, Temp-IP-Ban, Mute, TempMute, IP-Mute, Temp-IP-Mute, Warn, Kick, KickAll, Freeze, and Lockdown.
- 🤖 **Sentinel AI Auto-Moderation**: Integrates with Groq AI to detect toxicity, swearing, and chat violations automatically with zero lag.
- 🥶 **Advanced Freeze / Screenshare System**: Prevents movement, pvp, block breaking, and dropping items while frozen, with automatic logout enforcement.
- 🔒 **Server Lockdown Mode (`/lockdown`)**: Instantly restrict server entry during maintenance or bot raids with custom kick messages.
- ⏱️ **Smart Dynamic Duration**: `%duration%` automatically calculates exact remaining time until expiration in real time.
- ⚙️ **Dynamic Command Aliases**: Customize or translate any command (e.g. `/ban` → `/kitiltas`, `/mute` → `/nemit`) via `config.yml` or runtime API.
- 🔌 **Developer Java API & Bukkit Custom Events**: Full event cancellation, executor name overrides, and seamless integration for external plugins (like SyncCord, DiscordSRV, or custom bots).
- 🌐 **Built-in HTTP REST Web API**: Remote punishment administration with `X-API-Key` authentication for web dashboards and external services.
- 📜 **Staff & Player History Tracking**: Track staff member performance (`/staffhistory`) and full player punishment history (`/history`).
- 💾 **Dual Storage Backend**: Supports SQLite database and YAML storage.

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
| `/lockdown` | `/lockdown [on/off] [reason]` | Toggle server lockdown mode | `wapeb.lockdown` |
| `/punishrollback` | `/punishrollback <staff> <time>` | Undo punishments issued by staff | `wapeb.rollback` |
| `/wapeb` | `/wapeb [reload/status]` | Main plugin management command | `wapeb.admin` |

---

## ☕ Developer API & JitPack Setup

### Gradle (`build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.8'
}
```

### Maven (`pom.xml`)
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
        <version>v1.0.8</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Accessing the Java API
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;

WapeBAPI api = WapeB.getApi();

// Check if a player or alt account is muted
boolean isMuted = api.isMutedForPlayerOrAlt("PlayerName");

// Issue a mute programmatically
api.mutePlayer("PlayerName", "Chat Spam", "Console", 3600000L, false, false);

// Record an action into a specific staff member's history
api.recordStaffAction("StaffMember", "TargetPlayer", PunishmentType.MUTE, "Reason", 3600000L);
```

---

## 🌐 HTTP REST Web API

wapeB includes a built-in HTTP server for remote web dashboards or Discord bot integration.

- **Header**: `X-API-Key: YOUR_API_KEY_HERE`
- `GET /api/player/checkban?player=PlayerName`
- `GET /api/player/checkmute?player=PlayerName`
- `GET /api/player/punishments?player=PlayerName`
- `POST /api/punish/execute`
- `POST /api/lockdown`

---

## 📜 Compatibility & Support
- **Supported Server Software**: Paper, Purpur, Spigot (1.18.2 – 1.21.x)
- **Java Requirement**: Java 17+
- **GitHub Repository**: [lftkraft/wapeB](https://github.com/lftkraft/wapeB)
