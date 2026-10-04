# 📚 wapeB API - Complete Developer Documentation (v1.0.13-alpha.3)

This documentation provides a comprehensive guide to the **wapeB** Minecraft punishment system's **Java API**, **Bukkit Events**, **Dynamic Command Overrides**, **Message Placeholders**, **Proof (Evidence) System**, **Cross-Server / Velocity Synchronization**, **Extensible Importers Framework**, **AltExempt Audit API**, **Quick-Punish Template Shortcuts**, and **HTTP REST Web API**.

---

## 📑 Table of Contents
1. [Setup & Dependencies](#1-setup--dependencies)
2. [Java API Access & Thread Safety](#2-java-api-access--thread-safety)
3. [Detailed Java API Reference](#3-detailed-java-api-reference)
   - [A) Query Methods](#a-query-methods)
   - [B) Staff History & Action Recording Methods](#b-staff-history--action-recording-methods)
   - [C) Execution Methods & Multi-Server Scoping](#c-execution-methods--multi-server-scoping)
   - [D) Proof (Evidence) API Methods](#d-proof-evidence-api-methods)
   - [E) Chat Snapshot API Methods](#e-chat-snapshot-api-methods-v1013-alpha2)
   - [F) CIDR Subnet & GeoIP API Methods](#f-cidr-subnet--geoip-api-methods)
   - [G) Punishment Templates & Quick-Punish API Methods](#g-punishment-templates--quick-punish-api-methods-v1013-alpha3)
   - [H) AltExempt Extended API Methods](#h-altexempt-extended-api-methods-v1013-alpha3)
   - [I) Extensible Importer API Methods](#i-extensible-importer-api-methods-v1013-alpha3)
   - [J) Warn-Action Escalation API Methods](#j-warn-action-escalation-api-methods)
   - [K) Command Alias Methods](#k-command-alias-methods)
   - [L) Message Placeholders & Duration Formatting](#l-message-placeholders--duration-formatting)
   - [M) Smart Player & Active Punishment Lookup](#m-smart-player--active-punishment-lookup)
4. [Bukkit Custom Events](#4-bukkit-custom-events)
5. [Cross-Server & Velocity Architecture](#5-cross-server--velocity-architecture)
6. [In-Game Commands & Proof Flags](#6-in-game-commands--proof-flags)
7. [Integration Examples & Code Snippets](#7-integration-examples--code-snippets)
   - [Example 1: Custom Mute Command (GMute)](#example-1-custom-mute-command-gmute)
   - [Example 2: Discord Bot (SyncCord / DiscordSRV) Executor Override](#example-2-discord-bot-synccord--discordsrv-override)
   - [Example 3: Punishing with Proof from Code](#example-3-punishing-with-proof-from-code)
   - [Example 4: Chat Listener & Mute Notice](#example-4-chat-listener--mute-notice)
   - [Example 5: Custom Punishment Importer Registration](#example-5-custom-punishment-importer-registration-v1013-alpha3)
8. [HTTP REST Web API Reference](#8-http-rest-web-api-reference)

---

## 1. Setup & Dependencies

The wapeB API is available via **JitPack** or local Maven repository (`.m2`).

### 🐘 Gradle (Groovy DSL - `build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.13-alpha.3'
}
```

### 🐘 Gradle (Kotlin DSL - `build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.lftkraft:wapeB:v1.0.13-alpha.3")
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
        <version>v1.0.13-alpha.3</version>
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
- **Query Methods** (`isBanned`, `getActiveBan`, `getPunishments`, `getAlts`, `getProof`) are thread-safe and can be queried safely on async threads (e.g., in Discord bot events).
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

#### `getAlts` & `getDetailedAlts`
Returns linked alternative account usernames or detailed `AltInfo` records (with punishment status, CIDR subnet indicator, exemptions, and last seen timestamps).
- `List<String> getAlts(UUID playerUuid)`
- `List<String> getAlts(String playerName)`
- `List<AltInfo> getDetailedAlts(UUID playerUuid)`
- `List<AltInfo> getDetailedAlts(String playerName)`

#### `getIpHistory`
Fetches historical IP addresses and timestamps logged for a player.
- `List<IpHistoryRecord> getIpHistory(UUID playerUuid)`
- `List<IpHistoryRecord> getIpHistory(String playerName)`

#### Alt Exemptions (`isAltExempt` / `setAltExempt`)
Manage family / roommate exemptions from alt account checks.
- `boolean isAltExempt(UUID/String player)` – Returns `true` if player is exempted.
- `boolean setAltExempt(UUID/String player, boolean exempt, String addedBy)` – Adds or removes exemption.

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

### C) Execution Methods & Multi-Server Scoping

All execution methods trigger wapeB's `PlayerPunishEvent`. If a listener cancels the event (`event.setCancelled(true)`), the method returns `false`.

#### Multi-Server Scoping Architecture (v1.0.13+)
wapeB cleanly separates the **Origin Server** (`server` - where the command or API was triggered) and the **Target Enforcement Scope** (`activeServer` - where the punishment is actually enforced):
- **`activeServer = "global"`**: Punishment is enforced on all servers across the network.
- **`activeServer = "survival"`**: Punishment is only enforced on the `survival` server.
- **`activeServer = "server1,server2"`**: Comma-separated list of target servers.
- **`server`**: The origin server name (defaults to `server-name` from `config.yml`).

#### `banPlayer`
```java
// Full overload with multi-server scoping and proof attachment (v1.0.13-alpha.2):
boolean success = api.banPlayer(
    "PlayerName",             // Target name or UUID
    "Cheating / Hacking",      // Reason
    "Console",                // Executor name (custom string)
    86400000L,                // Duration in ms (-1 = Permanent)
    false,                    // Silent announcement?
    false,                    // IP ban?
    "global",                 // Target scope (e.g. "global", "server2", or "server1,server2")
    "lobby",                  // Origin server (where the action was initiated)
    "https://imgur.com/evidence123.png" // Proof URL / evidence
);

// Convenience overloads:
api.banPlayer("PlayerName", "Cheating", "Console", 86400000L, false, false);
api.banPlayer("PlayerName", "Cheating", "Console", 86400000L, false, false, "server2");
api.banPlayer("PlayerName", "Cheating", "Console", 86400000L, false, false, "global", "lobby");
```

#### `mutePlayer`
```java
// Full overload with multi-server scoping and proof:
boolean success = api.mutePlayer(
    "PlayerName", 
    "Chat Spam", 
    "ywxlol - DISCORD",       // Custom executor override
    3600000L,                 // Duration: 1 hour (ms)
    false,                    // Silent
    false,                    // IP mute
    "global",                 // Target scope
    "lobby",                  // Origin server
    "https://gyazo.com/spam_proof.png" // Proof URL
);

// Convenience overloads:
api.mutePlayer("PlayerName", "Chat Spam", "Admin", 3600000L, false, false);
api.mutePlayer("PlayerName", "Chat Spam", "Admin", 3600000L, false, false, "survival");
```

#### `warnPlayer`
```java
// Full overload with proof:
boolean success = api.warnPlayer("PlayerName", "Swearing", "AdminName", false, "global", "lobby", "https://i.imgur.com/chat.png");

// Convenience overloads:
api.warnPlayer("PlayerName", "Swearing", "AdminName", false);
api.warnPlayer("PlayerName", "Swearing", "AdminName", false, "minigames");
```

#### `kickPlayer`
```java
// Full overload with proof:
boolean success = api.kickPlayer("PlayerName", "AFK for too long", "System", false, "global", "lobby", null);

// Convenience overload:
api.kickPlayer("PlayerName", "AFK for too long", "System", false);
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

### D) Proof (Evidence) API Methods (v1.0.13-alpha.2+)

wapeB provides dedicated API methods for attaching, updating, querying, and deleting proof links/evidence (e.g. Imgur, YouTube, Gyazo URLs) associated with any punishment:

```java
// 1. Fetch attached proof URL for a punishment ID (returns null or String):
String proofUrl = api.getProof(105);

// 2. Attach or update proof URL for a punishment:
boolean updated = api.setProof(105, "https://imgur.com/evidence123.png");

// 3. Remove proof from a punishment:
boolean removed = api.removeProof(105);
```

---

### E) Chat Snapshot API Methods (v1.0.13-alpha.2+)

wapeB automatically maintains a rolling in-memory buffer of recent chat messages and creates a frozen snapshot whenever a player receives a punishment:

```java
// 1. Fetch chat snapshot object for a punishment:
ChatSnapshot snapshot = api.getChatSnapshot(105);
if (snapshot != null) {
    for (ChatMessage msg : snapshot.getMessages()) {
        System.out.println("[" + msg.getPlayerName() + "]: " + msg.getMessage());
    }
}

// 2. Check if a snapshot exists:
boolean hasSnapshot = api.hasChatSnapshot(105);

// 3. Delete a local snapshot:
boolean deleted = api.deleteChatSnapshot(105);

// 4. Query live recent chat from memory (e.g. for custom automod before punishing):
List<ChatMessage> recentChat = api.getRecentChat(playerUuid, 30);

// 5. Manually capture a snapshot on-demand:
ChatSnapshot liveSnapshot = api.captureChatSnapshot(playerUuid, 30);
```

---

### F) CIDR Subnet & GeoIP API Methods

Manage CIDR IP subnet bans (`192.168.1.0/24` or `192.168.1.*`) and fetch GeoIP information asynchronously:

```java
// Check if an IP is covered by an active CIDR ban
boolean isBanned = api.isCidrBanned("192.168.1.50");

// Fetch active CIDR ban details
Punishment cidrBan = api.getActiveCidrBan("192.168.1.0/24");

// Ban an entire IP range / subnet
api.banIpRange("192.168.1.0/24", "VPN / Botnet Subnet", "Console", 7 * 86400000L, false);

// Unban an IP range
api.unbanIpRange("192.168.1.0/24", "Resolved", "Admin");

// Fetch GeoIP information (Country, City, ISP)
GeoIPUtil.GeoInfo geo = api.getGeoInfo("1.1.1.1");
String location = geo.getFormatted(); // e.g. "Australia (AU) | City: Sydney | ISP: Cloudflare"
```

---

### G) Punishment Templates & Quick-Punish API Methods (v1.0.13-alpha.3+)

Query, create, delete, and apply pre-defined punishment templates and fast `#shortcut` codes programmatically:

```java
// 1. Fetch a specific template by category and key (e.g., category: "ban", key: "cheating")
PunishmentTemplate template = api.getTemplate("ban", "cheating");
String reason = template.getReason();
String duration = template.getDuration();
boolean isSilent = template.isSilent();
String shortcut = template.getShortcut(); // e.g., "#1"

// 2. Global template lookup by name or shortcut (e.g. "#1", "$spam", "cheating")
PunishmentTemplate found = api.findTemplate("#1");

// 3. Fetch all templates or category templates
Map<String, Map<String, PunishmentTemplate>> allTemplates = api.getAllTemplates();
List<PunishmentTemplate> banTemplates = api.getTemplatesForCategory("ban");

// 4. Create or update a template dynamically at runtime
boolean saved = api.saveTemplate("ban", "flyhack", "Using illegal flight modifications", "30d", true, "#fly");

// 5. Delete a template dynamically
boolean deleted = api.deleteTemplate("ban", "flyhack");

// 6. Execute punishment directly using a template
api.punishWithTemplate(playerUuid, "ban", "cheating", "AdminName", false);
api.punishWithTemplate("PlayerName", "mute", "spam", "ModName", false);
```

---

### H) AltExempt Extended API Methods (v1.0.13-alpha.3+)

Full Java API access to manage and query alt-exemption audit details:

```java
// 1. Check if a player is alt-exempt
boolean exempt = api.isAltExempt(playerUuid);

// 2. Set alt-exempt state with custom issuer audit metadata
api.setAltExempt(playerUuid, true, "SeniorAdmin");

// 3. Fetch full audit details for a specific player (who exempted them and when)
AltExemptInfo details = api.getAltExemptDetails(playerUuid);
if (details != null) {
    String name = details.getPlayerName();
    String exemptBy = details.getExemptBy();
    long exemptDate = details.getExemptDate();
}

// 4. Query all currently alt-exempted players across the server
List<AltExemptInfo> allExempts = api.getAllAltExempts();
for (AltExemptInfo info : allExempts) {
    System.out.println(info.getPlayerName() + " (" + info.getPlayerUuid() + ") exempt by " + info.getExemptBy());
}
```

---

### I) Extensible Importer API Methods (v1.0.13-alpha.3+)

wapeB provides an open, extensible framework for registering custom punishment importers and executing batch migrations:

```java
// 1. Register a custom plugin/format importer:
api.registerImporter(new PunishmentImporter() {
    @Override
    public String getName() {
        return "mycustompunish";
    }

    @Override
    public String getDescription() {
        return "Imports punishments from MyCustomPunish plugin database.";
    }

    @Override
    public CompletableFuture<ImportResult> executeImport(Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            ImportResult result = new ImportResult(getName(), true);
            // Read source data and save via plugin
            return result;
        });
    }
});

// 2. Query registered importers
List<PunishmentImporter> importers = api.getRegisteredImporters();

// 3. Trigger an importer asynchronously
api.executeImport("litebans", Map.of("file", "plugins/LiteBans/litebans.sqlite"))
   .thenAccept(result -> {
       System.out.println("Imported: " + result.getImportedCount() + ", Failed: " + result.getFailedCount());
   });

// 4. Batch import a List of Punishment objects directly into database
List<Punishment> batch = List.of(new Punishment(...));
api.importPunishments(batch).thenAccept(res -> {
    System.out.println("Batch import finished: " + res.getImportedCount() + " records saved.");
});
```

---

### J) Warn-Action Escalation API Methods

Query warning thresholds and trigger automated escalation actions:

```java
// Get active warning count for player (respects configured warning-expiry)
int activeWarns = api.getActiveWarnCount(playerUuid);

// Fetch configured warn-action thresholds map (e.g. 3 -> "tempmute...", 5 -> "tempban...")
Map<Integer, String> warnActions = api.getWarnActions();

// Manually trigger warn-action threshold check for a player
api.triggerWarnActionCheck(playerUuid);
```

---

### K) Command Alias Methods

Register dynamic command aliases at runtime:
```java
// Register '/kitiltas' alias for '/ban'
api.registerCommandAlias("ban", "kitiltas");

// Get registered custom aliases
List<String> aliases = api.getCommandAliases("ban");
```

---

### L) Message Placeholders & Duration Formatting

wapeB provides rich placeholder replacement across all in-game messages, kick screens, broadcast messages, and Discord webhooks.

#### Supported Placeholders:
- `%proof%`: Attached proof URL or text (or localized `none` indicator). *(v1.0.13-alpha.2+)*
- `%time%` / `%duration%` / `%remaining%` / `%remaining_duration%` / `%time_left%` / `%expires_in%`: Returns formatted remaining time until expiration (e.g. `14d`, `2h 15m`).
- `%detailed_duration%` / `%detailed_remaining%`: Returns detailed remaining time (e.g. `14 days 2 hours`).
- `%original_duration%` / `%total_duration%`: Returns original assigned punishment duration.
- `%detailed_original_duration%`: Returns detailed original assigned duration.
- `%player%`: Target player username.
- `%executor%`: Staff member / executor name.
- `%reason%`: Punishment reason.
- `%type%`: Display name of the punishment type (e.g. `Ban`, `Temp-Mute`).
- `%punishment_id%`: Numeric ID of the punishment record.
- `%server%`: Origin server where the punishment was executed.
- `%activeserver%` / `%active_server%`: Target server scope where the punishment applies (e.g. `global` or `survival,skyblock`).
- `%date%`: Formatted issuance date (`yyyy-MM-dd HH:mm:ss`).
- `%end_date%`: Formatted expiration date (`yyyy-MM-dd HH:mm:ss`) or `Permanent`.

#### ⏱️ Ceiling Duration Rounding:
Remaining seconds are rounded **upward** `((millis + 999) / 1000)` so that newly issued punishments immediately show the exact full duration (e.g. a 14-day ban instantly displays as `14d` rather than `13d 23h 59m 59s`).

---

### M) Smart Player & Active Punishment Lookup

wapeB commands (`/unban`, `/unmute`, `/checkban`, `/checkmute`, `/history`, `/warnings`, `/unwarn`, `/ban`, `/mute`, `/banip`, `/muteip`, `/warn`, `/proof`, `/wapeb import`) and Java API methods resolve players and active punishments using multi-criteria queries:
- **Case-Insensitive Username Resolution**: Matches player names regardless of capitalization.
- **UUID & IP Resolution**: Automatically resolves offline player UUIDs and recorded IP addresses.
- **Alt Account Linkage**: Queries linked alt accounts when evaluating active bans/mutes.
- **No Restrictive Bukkit Blocking**: Removes legacy `hasPlayedBefore()` restrictions so that offline/unban/unmute operations always succeed when active database records exist.

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
- `getProof()` / `setProof(String)` – **Modifiable proof URL / evidence** *(v1.0.13-alpha.2+)*
- `getDuration()` / `setDuration(long)` – **Modifiable duration**
- `getActiveServer()` / `setActiveServer(String)` – **Modifiable target server scope** *(e.g. "global", "survival")*
- `getServer()` / `setServer(String)` – **Modifiable origin server name**
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

        // Automatically inject proof if empty:
        if (event.getProof() == null || event.getProof().isEmpty()) {
            event.setProof("https://panel.myserver.net/logs/" + event.getPlayerName());
        }

        // Override executor name dynamically:
        if (event.getExecutor().equalsIgnoreCase("Console")) {
            event.setExecutor("Automated Protection System");
        }
        
        // Ensure network-wide scope:
        if (event.getActiveServer() == null || event.getActiveServer().isEmpty()) {
            event.setActiveServer("global");
        }
    }
}
```

### 2. `PlayerUnpunishEvent` (Cancellable)
Fires during Unban / Unmute / Unwarn:
- `getPunishment()` – The `Punishment` object being revoked.
- `getExecutor()` – The name of the revoking entity/admin.

---

## 5. Cross-Server & Velocity Architecture

In v1.0.13+, wapeB integrates seamlessly with **Velocity** and **BungeeCord** proxies using the official companion plugin `wapeb-velocity`:

- **Instant Plugin Messaging**: Broadcasts (`wapeb:channel`) forward instantly across backend servers without polling lag.
- **Proxy-Level Disconnection**: When a player is banned or kicked with target scope `global` (or matching their current backend server), Velocity immediately disconnects them with the formatted kick screen.
- **Smart Deduplication & Fallback**: Deduplication keys prevent duplicate chat broadcasts, while background MySQL synchronization guarantees that empty or rebooting servers stay perfectly synced.

---

## 6. In-Game Commands & Proof Flags (v1.0.13-alpha.2+)

### A) Punishment Commands with `-proof` Flag
All punishment commands (`/ban`, `/banip`, `/mute`, `/muteip`, `/warn`, `/kick`) support inline `-proof:<url>` and `-proof=<url>` flags anywhere in the reason:

```text
/ban Player123 7d Hacking -proof:https://youtu.be/example -s
/mute Spammer 1h Chat Flood -proof:https://imgur.com/screenshot.png
/warn ToxicPlayer Swearing -proof:https://gyazo.com/chat.png
```

### B) Dedicated `/punish-proof` Command (Alias: `/proof`)
Manage and inspect proof URLs for existing punishments by ID:

- `/proof set <id> <url>` – Attaches or updates the proof URL for punishment `#<id>`.
- `/proof remove <id>` – Removes attached proof from punishment `#<id>`.
- `/proof reset <id> <url>` – Resets the proof URL for punishment `#<id>`.
- `/proof check <id>` – Displays complete punishment information (Player, Executor, Type, Reason, Status, Date) along with the attached proof URL.

---

## 7. Integration Examples & Code Snippets

### Example 1: Custom Mute Command (GMute)
A lightweight command that mutes with a custom executor name and server scope:

```java
public class GMuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /gmute <player> <reason> <executor> [server]
        String target = args[0];
        String reason = args[1];
        String executor = args[2];
        String activeServer = (args.length > 3) ? args[3] : "global";

        WapeB.getApi().mutePlayer(target, reason, executor, -1, false, false, activeServer);
        sender.sendMessage("§aMuted successfully! Scope: " + activeServer);
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

### Example 3: Punishing with Proof from Code
```java
// Ban player with direct proof attachment:
WapeB.getApi().banPlayer(
    "Cheater99",
    "Fly / Speed Hack",
    "StaffBot",
    30 * 86400000L, // 30 days
    false,
    false,
    "global",
    "lobby",
    "https://youtu.be/videoProof123"
);
```

---

### Example 4: Chat Listener & Mute Notice
Check if a player is muted on the current server when attempting to chat:

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

### Example 5: Custom Punishment Importer Registration (v1.0.13-alpha.3+)
Register an external database migration pipeline directly from your add-on plugin:

```java
public class MyBanMigrationPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        WapeBAPI api = WapeB.getApi();
        if (api != null) {
            api.registerImporter(new PunishmentImporter() {
                @Override
                public String getName() {
                    return "customsql";
                }

                @Override
                public String getDescription() {
                    return "Custom legacy MySQL ban table importer";
                }

                @Override
                public CompletableFuture<ImportResult> executeImport(Map<String, Object> options) {
                    return CompletableFuture.supplyAsync(() -> {
                        ImportResult result = new ImportResult(getName(), true);
                        // Query external DB and add records
                        result.incrementImported();
                        return result;
                    });
                }
            });
        }
    }
}
```

---

## 8. HTTP REST Web API Reference

wapeB includes a high-performance built-in HTTP REST server for remote management (e.g., Web Dashboards, Discord bots, Webhooks).

- **Authentication Header**: `X-API-Key: YOUR_API_KEY_HERE`

### Endpoints Overview:

| Endpoint | Method | Parameters | Description |
|---|---|---|---|
| `/api/player/punishments` | GET | `player=Name` | Fetch all punishments for a player as JSON array (includes `activeServer`, `server`, and `proof`) |
| `/api/player/profile` | GET | `player=Name` | Fetch player profile and complete punishment history with `proof` |
| `/api/player/checkban` | GET | `player=Name` | Active ban status and details (includes `proof`) |
| `/api/player/checkmute` | GET | `player=Name` | Active mute status and details (includes `proof`) |
| `/api/punish/active` | GET | - | Fetch all currently active punishments network-wide (includes `proof`) |
| `/api/punish/proof` | GET/POST | `id=105&action=get\|set\|reset\|remove[&proof=url]` | Inspect, set, update, or remove proof URL for a punishment record *(v1.0.13-alpha.2+)* |
| `/api/punish/snapshot` | GET | `id=105` | Fetch recorded JSON chat snapshot for punishment record *(v1.0.13-alpha.2+)* |
| `/api/player/recentchat` | GET | `player=Name&limit=30` | Fetch recent in-memory chat messages for a player *(v1.0.13-alpha.2+)* |
| `/api/commands/list` | GET | - | List registered commands and aliases |
| `/api/punish/execute` | GET/POST | `target=Name&type=BAN&reason=Reason&duration=1d&active_server=global&origin_server=web&proof=url` | Issue punishment via REST with custom server scoping and proof |
| `/api/punish/remove` | GET/POST | `id=105` | Remove punishment by ID |
| `/api/stats` | GET | - | Daily and hourly punishment statistics |
| `/api/lockdown` | GET/POST | `action=on&reason=Maintenance` | Manage server lockdown state |
| `/api/import/sources` | GET | - | List all available registered punishment importers *(v1.0.13-alpha.3+)* |
| `/api/import/execute` | POST | JSON: `{ "source": "litebans", "options": { "file": "path" } }` | Execute punishment import asynchronously *(v1.0.13-alpha.3+)* |
| `/api/altexempt/list` | GET | - | Query all currently alt-exempted accounts with timestamps and audit records *(v1.0.13-alpha.3+)* |
| `/api/altexempt/check` | GET | `player=Name` or `uuid=UUID` | Check alt-exemption state and issuer metadata *(v1.0.13-alpha.3+)* |
| `/api/altexempt/set` | POST | JSON: `{ "player": "Name", "exempt": true, "executor": "Admin" }` | Toggle alt exemption status programmatically *(v1.0.13-alpha.3+)* |
| `/api/templates/list` | GET | `category=ban` (optional) | Query all punishment templates and `#shortcut` codes *(v1.0.13-alpha.3+)* |
| `/api/templates/get` | GET | `key=cheating` or `shortcut=#1` | Fetch template details *(v1.0.13-alpha.3+)* |
| `/api/templates/save` | POST | JSON: `{ "category": "ban", "key": "cheating", "reason": "Hacking", "duration": "30d", "silent": true, "shortcut": "#1" }` | Create or update punishment template *(v1.0.13-alpha.3+)* |
| `/api/templates/delete` | POST | JSON: `{ "category": "ban", "key": "cheating" }` | Delete template *(v1.0.13-alpha.3+)* |

---

### REST JSON Examples:

#### 1. AltExempt Check (`GET /api/altexempt/check?player=Steve`)
```json
{
  "uuid": "8667ba71-b85a-4004-af54-457a9734eed7",
  "player": "Steve",
  "exempt": true,
  "exemptBy": "SeniorAdmin",
  "exemptDate": 1728054000000
}
```

#### 2. AltExempt Set (`POST /api/altexempt/set`)
**Request Body:**
```json
{
  "player": "Steve",
  "exempt": true,
  "executor": "WebDashboard"
}
```
**Response:**
```json
{
  "success": true,
  "uuid": "8667ba71-b85a-4004-af54-457a9734eed7",
  "exempt": true,
  "exemptBy": "WebDashboard"
}
```

#### 3. Execute Importer (`POST /api/import/execute`)
**Request Body:**
```json
{
  "source": "litebans",
  "options": {
    "file": "plugins/LiteBans/litebans.sqlite"
  }
}
```
**Response:**
```json
{
  "source": "litebans",
  "success": true,
  "imported": 1420,
  "failed": 0,
  "durationMs": 350,
  "errors": [],
  "details": [
    "Processed 1420 punishment records from LiteBans SQLite database."
  ]
}
```

#### 4. Templates List (`GET /api/templates/list?category=ban`)
```json
{
  "templates": [
    {
      "key": "cheating",
      "category": "ban",
      "reason": "Unfair Advantage / Cheat modifications",
      "duration": "30d",
      "silent": false,
      "shortcut": "#1"
    },
    {
      "key": "botting",
      "category": "ban",
      "reason": "Botnet / Automation",
      "duration": "perm",
      "silent": true,
      "shortcut": "#2"
    }
  ]
}
```

#### 5. Save Template (`POST /api/templates/save`)
**Request Body:**
```json
{
  "category": "ban",
  "key": "xray",
  "reason": "X-Ray / Mining Automation",
  "duration": "14d",
  "silent": false,
  "shortcut": "#xray"
}
```
**Response:**
```json
{
  "success": true,
  "category": "ban",
  "key": "xray",
  "reason": "X-Ray / Mining Automation",
  "duration": "14d",
  "silent": false,
  "shortcut": "#xray"
}
```

---

## 📜 License & Support
Developed for Spigot / Paper 1.18.2 - 1.21.x Minecraft servers.  
GitHub Repository: [https://github.com/lftkraft/wapeB](https://github.com/lftkraft/wapeB)

