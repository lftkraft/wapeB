# 📚 wapeB API - Complete Developer Documentation (v1.0.8)

This documentation provides a comprehensive guide to the **wapeB** Minecraft punishment system's **Java API**, **Bukkit Events**, **Dynamic Command Overrides**, and **HTTP REST Web API**.

---

## 📑 Table of Contents
1. [Setup & Dependencies](#1-setup--dependencies)
2. [Java API Access & Thread Safety](#2-java-api-access--thread-safety)
3. [Detailed Java API Reference](#3-detailed-java-api-reference)
   - [A) Query Methods](#a-query-methods)
   - [B) Staff History & Action Recording Methods](#b-staff-history--action-recording-methods)
   - [C) Execution Methods](#c-execution-methods)
   - [D) Command Alias Methods](#d-command-alias-methods)
4. [Bukkit Custom Events](#4-bukkit-custom-events)
5. [Integration Examples & Code Snippets](#5-integration-examples--code-snippets)
   - [Example 1: Custom Mute Command (GMute)](#example-1-custom-mute-command-gmute)
   - [Example 2: Discord Bot (SyncCord / DiscordSRV) Executor Override](#example-2-discord-bot-synccord--discordsrv-executor-override)
   - [Example 3: External Staff History Recording](#example-3-external-staff-history-recording)
   - [Example 4: Chat Listener & Mute Notice](#example-4-chat-listener--mute-notice)
6. [HTTP REST Web API Reference](#6-http-rest-web-api-reference)

---

## 1. Setup & Dependencies

The wapeB API is available via **JitPack** or local Maven repository (`.m2`).

### 🐘 Gradle (Kotlin DSL - `build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.lftkraft:wapeB:v1.0.8")
}
```

### 🐘 Gradle (Groovy DSL - `build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.8'
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
        <version>v1.0.8</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 📄 `plugin.yml` Configuration
In your plugin's `plugin.yml`, declare `wapeB` as a dependency:
```yaml
name: MyPlugin
version: 1.0.0
main: dev.example.myplugin.Main
api-version: '1.21'

# Required dependency:
depend: [wapeB]

# Optional dependency:
# softdepend: [wapeB]
```

#### Checking Softdepend in Code:
```java
if (Bukkit.getPluginManager().isPluginEnabled("wapeB")) {
    WapeBAPI api = WapeB.getApi();
    // Use wapeB API
}
```

---

## 2. Java API Access & Thread Safety

### Obtaining the API Instance
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;
import dev.azuyo.wapeB.api.WapeBAPIProvider;

// Option 1 (Primary):
WapeBAPI api = WapeB.getApi();

// Option 2 (Provider):
WapeBAPI api = WapeBAPIProvider.getAPI();
```

### ⚠️ Thread Safety Guidelines
- **Query Methods** (`isBanned`, `getActiveBan`, `getPunishments`, `getAlts`) are thread-safe and can be queried safely on async threads (e.g., in Discord bot events).
- **Execution Methods** (`banPlayer`, `mutePlayer`, `kickPlayer`, `freezePlayer`) must run on the Server Main Thread, as they trigger Bukkit events and kick/affect online players. If invoking from a Discord JDA or async thread, wrap execution in `Bukkit.getScheduler().runTask(plugin, ...)`.

---

## 3. Detailed Java API Reference

### A) Query Methods

#### `getPunishments`
Fetches all punishments for a player (both active and expired/revoked).
- `List<Punishment> getPunishments(UUID playerUuid)`
- `List<Punishment> getPunishments(String playerName)`

#### `getActiveBan` / `getActiveBanForPlayerOrAlt`
Returns the currently active ban for a player, resolving their online or offline IP and checking linked alt accounts.
- `Punishment getActiveBan(UUID playerUuid)`
- `Punishment getActiveBan(String playerName)`
- `Punishment getActiveBanByIp(String ipAddress)`
- `Punishment getActiveBanForPlayerOrAlt(UUID playerUuid)`
- `Punishment getActiveBanForPlayerOrAlt(String playerName)`

#### `getActiveMute` / `getActiveMuteForPlayerOrAlt`
Returns the currently active mute for a player, resolving their online or offline IP and checking linked alt accounts.
- `Punishment getActiveMute(UUID playerUuid)`
- `Punishment getActiveMute(String playerName)`
- `Punishment getActiveMuteByIp(String ipAddress)`
- `Punishment getActiveMuteForPlayerOrAlt(UUID playerUuid)`
- `Punishment getActiveMuteForPlayerOrAlt(String playerName)`

#### `getWarnings`
Fetches active warnings for a player.
- `List<Punishment> getWarnings(UUID playerUuid)`
- `List<Punishment> getWarnings(String playerName)`

#### `getAlts`
Returns a list of alternative account usernames linked by IP address.
- `List<String> getAlts(UUID playerUuid)`
- `List<String> getAlts(String playerName)`

#### Status Checkers (`boolean`)
- `boolean isBanned(UUID/String/IP)` – `true` if active ban exists for player, IP, or alts.
- `boolean isMuted(UUID/String/IP)` – `true` if active mute exists for player, IP, or alts.
- `boolean isBannedForPlayerOrAlt(UUID/String)` – Explicit check for player or alt bans.
- `boolean isMutedForPlayerOrAlt(UUID/String)` – Explicit check for player or alt mutes.
- `boolean isFrozen(UUID/String)` – `true` if player is currently frozen (`/freeze`).
- `boolean isLockdownActive()` – `true` if server lockdown is enabled (`/lockdown`).
- `String getLockdownReason()` – Returns current lockdown reason.

---

### B) Staff History & Action Recording Methods

External plugins can query a staff member's history or attribute new actions directly to a staff member's history.

#### `getStaffHistory`
Returns all punishments and actions recorded under a specific staff member's executor name.
- `List<Punishment> getStaffHistory(String executorName)`

#### `recordStaffAction`
Records a punishment action directly into a staff member's history.
```java
// Record a 1-hour mute performed by 'ywxlol' against 'Pistike'
boolean recorded = api.recordStaffAction(
    "ywxlol",                          // Staff executor name
    "Pistike",                         // Target player name
    Punishment.PunishmentType.MUTE,    // Punishment type
    "Chat Spam",                       // Reason
    3600000L                           // Duration (ms)
);
```

#### `addStaffHistoryEntry`
Adds an existing or custom `Punishment` object directly to a staff member's history.
```java
boolean recorded = api.addStaffHistoryEntry("ywxlol", punishment);
```

---

### C) Execution Methods

All execution methods trigger wapeB's `PlayerPunishEvent`. If a listener cancels the event (`event.setCancelled(true)`), the method returns `false`.

#### `banPlayer`
```java
boolean success = api.banPlayer(
    "PlayerName",             // Target name or UUID
    "Cheating / Hacking",      // Reason
    "Console",                // Executor name (custom string)
    86400000L,                // Duration in ms (-1 = Permanent)
    false,                    // Silent announcement?
    false                     // IP ban?
);
```

#### `mutePlayer`
```java
boolean success = api.mutePlayer(
    "PlayerName", 
    "Chat Spam", 
    "ywxlol - DISCORD",       // Custom executor override
    3600000L,                 // Duration: 1 hour (ms)
    false,                    // Silent
    false                     // IP mute
);
```

#### `warnPlayer`
```java
boolean success = api.warnPlayer("PlayerName", "Swearing", "AdminName", false);
```

#### `kickPlayer`
```java
boolean success = api.kickPlayer("PlayerName", "AFK for too long", "System", false);
```

#### `freezePlayer` / `unfreezePlayer`
```java
api.freezePlayer("PlayerName", "Screenshare required", "StaffMember");
api.unfreezePlayer("PlayerName", "StaffMember");
```

#### `unbanPlayer` / `unmutePlayer` / `revokePunishment`
```java
api.unbanPlayer("PlayerName", "Appeal accepted", "Admin");
api.unmutePlayer("PlayerName", "Appeal accepted", "Admin");
api.revokePunishment(105, "Admin"); // Remove punishment by ID
```

---

### C) Command Alias Methods

Register dynamic command aliases at runtime:
```java
// Register '/kitiltas' alias for '/ban'
api.registerCommandAlias("ban", "kitiltas");

// Get registered custom aliases
List<String> aliases = api.getCommandAliases("ban");
```

---

## 4. Bukkit Custom Events

wapeB fires custom Bukkit events before enforcing punishments and revocations.

### 1. `PlayerPunishEvent` (Cancellable)
Fires whenever a punishment is issued (via command, GUI, Web API, or code).

#### Available Event Methods:
- `getPlayerUuid()` / `getPlayerName()` / `getIpAddress()`
- `getType()` – `PunishmentType` (BAN, TEMPBAN, MUTE, WARN, KICK, etc.)
- `getReason()` / `setReason(String)` – **Modifiable reason**
- `getExecutor()` / `setExecutor(String)` – **Modifiable executor name** *(useful for Discord bot overrides)*
- `getDuration()` / `setDuration(long)` – **Modifiable duration**
- `isSilent()` / `setSilent(boolean)` – **Modifiable silent flag**
- `isCancelled()` / `setCancelled(boolean)` – **Cancel punishment execution**

#### Example Listener:
```java
import dev.azuyo.wapeB.api.events.PlayerPunishEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PunishListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPunish(PlayerPunishEvent event) {
        // VIP Protection Example:
        if (event.getPlayerName().startsWith("VIP_") && event.getType().name().contains("BAN")) {
            event.setCancelled(true);
            return;
        }

        // Override executor name dynamically:
        if (event.getExecutor().equalsIgnoreCase("Console")) {
            event.setExecutor("Automated Protection System");
        }
    }
}
```

### 2. `PlayerUnpunishEvent` (Cancellable)
Fires during Unban / Unmute / Unwarn:
- `getPunishment()` – The `Punishment` object being revoked.
- `getExecutor()` – The name of the revoking entity/admin.

---

## 5. Integration Examples & Code Snippets

### Example 1: Custom Mute Command (GMute)
A lightweight command that mutes with a custom executor name:

```java
public class GMuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /gmute <player> <reason> <executor>
        String target = args[0];
        String reason = args[1];
        String executor = args[2];

        WapeB.getApi().mutePlayer(target, reason, executor, -1, false, false);
        sender.sendMessage("§aMuted successfully! Executor: " + executor);
        return true;
    }
}
```

---

### Example 2: Discord Bot (SyncCord / DiscordSRV) Executor Override
When a Discord bot dispatches a command via console, override the executor name dynamically in `PlayerPunishEvent`:

```java
// 1. Dispatch command on Bukkit main thread, setting thread local context:
Bukkit.getScheduler().runTask(plugin, () -> {
    try {
        WapeBHook.setCurrentDiscordExecutor("ywxlol");
        Bukkit.dispatchCommand(customSender, "mute player123 1h spam");
    } finally {
        WapeBHook.clearCurrentDiscordExecutor();
    }
});

// 2. Listener overrides executor name in wapeB event:
@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
public void onPlayerPunish(PlayerPunishEvent event) {
    String discordExecutor = WapeBHook.getActiveDiscordExecutor();
    if (discordExecutor != null && !discordExecutor.trim().isEmpty()) {
        event.setExecutor(discordExecutor + " - DISCORD");
    }
}
```

---

### Example 3: Chat Listener & Mute Notice
Check if a player is muted when attempting to chat:

```java
@EventHandler
public void onChat(AsyncPlayerChatEvent event) {
    WapeBAPI api = WapeB.getApi();
    if (api != null && api.isMuted(event.getPlayer().getUniqueId())) {
        Punishment mute = api.getActiveMute(event.getPlayer().getUniqueId());
        String reason = (mute != null) ? mute.getReason() : "Muted";
        
        event.getPlayer().sendMessage("§cYou cannot speak because you are muted! Reason: " + reason);
        event.setCancelled(true);
    }
}
```

---

## 6. HTTP REST Web API Reference

wapeB includes a built-in HTTP REST server for remote management (e.g., Web Dashboards, Discord bots).

- **Header**: `X-API-Key: YOUR_API_KEY_HERE`

### Endpoints Overview:

| Endpoint | Method | Parameters | Description |
|---|---|---|---|
| `/api/player/punishments` | GET | `player=Name` | Fetch all punishments for a player as JSON array |
| `/api/player/checkban` | GET | `player=Name` | Active ban status and details |
| `/api/player/checkmute` | GET | `player=Name` | Active mute status and details |
| `/api/commands/list` | GET | - | List registered commands and aliases |
| `/api/punish/execute` | GET/POST | `target=Name&type=BAN&reason=Reason&duration=1d` | Issue punishment via REST |
| `/api/punish/remove` | GET/POST | `id=105` | Remove punishment by ID |
| `/api/stats` | GET | - | Daily and hourly punishment statistics |
| `/api/lockdown` | GET/POST | `action=on&reason=Maintenance` | Manage server lockdown state |

---

## 📜 License & Support
Developed for Spigot / Paper 1.18.2 - 1.21.x Minecraft servers.  
GitHub Repository: [https://github.com/lftkraft/wapeB](https://github.com/lftkraft/wapeB)
