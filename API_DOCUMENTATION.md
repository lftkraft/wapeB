# 📚 wapeB API - Teljeskörű Fejlesztői Dokumentáció (v1.0.5)

Ez a dokumentáció a **wapeB** Minecraft büntetési rendszer **Java API**-jának, **Bukkit Eseménykezelőinek (Events)**, **Parancs-átírási felületének**, valamint **HTTP REST Web API**-jának a teljeskörű és részletes útmutatója.

---

## 📑 Tartalomjegyzék
1. [Projekt Beállítás és Függőségek](#1-projekt-beállítás-és-függőségek)
2. [Java API Belépési Pontok és Szálbiztonság](#2-java-api-belépési-pontok-és-szálbiztonság)
3. [Részletes Java API Metódus Referencia](#3-részletes-java-api-metódus-referencia)
   - [A) Lekérdező Metódusok](#a-lekérdező-metódusok-query-methods)
   - [B) Szankció Végrehajtó Metódusok](#b-szankció-végrehajtó-metódusok-execution-methods)
   - [C) Parancs Átírási Metódusok](#c-parancs-átírási-metódusok-command-alias-methods)
4. [Bukkit Custom Eventek (Eseménykezelés)](#4-bukkit-custom-eventek-eseménykezelés)
5. [Integrációs Minták és Kódpéldák](#5-integrációs-minták-és-kódpéldák)
   - [Minta 1: Egyedi Némító Parancs (GMute)](#minta-1-egyedi-némító-parancs-gmute)
   - [Minta 2: Discord Bot (SyncCord / DiscordSRV) Végrehajtó Átírás](#minta-2-discord-bot-synccord--discordsrv-végrehajtó-átírás)
   - [Minta 3: Chat Tag & Mute Jelzés](#minta-3-chat-tag--mute-jelzés)
6. [HTTP REST Web API Referencia](#6-http-rest-web-api-referencia)

---

## 1. Projekt Beállítás és Függőségek

A wapeB API elérhető mind **JitPack** felhős tárolóból, mind helyi Maven (`.m2`) tárolóból.

### 🐘 Gradle (Kotlin DSL - `build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.lftkraft:wapeB:v1.0.5")
}
```

### 🐘 Gradle (Groovy DSL - `build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.5'
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
        <version>v1.0.5</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 📄 `plugin.yml` Beállítás
A saját pluginod `plugin.yml` fájljában állítsd be a függőséget:
```yaml
name: SajatPlugin
version: 1.0.0
main: dev.azuyo.sajatplugin.Main
api-version: '1.21'

# Kötelező függőség esetén:
depend: [wapeB]

# Opcionális függőség esetén:
# softdepend: [wapeB]
```

#### Opcionális (Softdepend) ellenőrzése kódból:
```java
if (Bukkit.getPluginManager().isPluginEnabled("wapeB")) {
    WapeBAPI api = WapeB.getApi();
    // wapeB API használata
}
```

---

## 2. Java API Belépési Pontok és Szálbiztonság

### API Példány Lekérése
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;
import dev.azuyo.wapeB.api.WapeBAPIProvider;

// 1. Elsődleges metódus:
WapeBAPI api = WapeB.getApi();

// 2. Szolgáltatón (Provider) keresztüli elérés:
WapeBAPI api = WapeBAPIProvider.getAPI();
```

### ⚠️ Szálbiztonság (Thread Safety Guidelines)
- A **lekérdező metódusok** (`isBanned`, `getActiveBan`, `getPunishments`, `getAlts`) szálbiztosak, elágaztathatók aszinkron szálakon is (pl. Discord bot eventekben).
- A **szankció végrehajtó metódusok** (`banPlayer`, `mutePlayer`, `kickPlayer`, `freezePlayer`) a Bukkit főszálán (Server Main Thread) futnak be biztonságosan, mert Bukkit Eventeket lőnek ki és játékosokat rúgnak ki a szerverről. Ha Discord JDA vagy egyéb aszinkron szálból hívod, csomagold `Bukkit.getScheduler().runTask(plugin, ...)` blokkba!

---

## 3. Részletes Java API Metódus Referencia

### A) Lekérdező Metódusok (Query Methods)

#### `getPunishments`
Lekéri a játékos összes eddigi büntetését (aktív és már lejárt/feloldott büntetéseket is).
- `List<Punishment> getPunishments(UUID playerUuid)`
- `List<Punishment> getPunishments(String playerName)`

#### `getActiveBan`
Visszaadja a játékos jelenleg aktív kitiltását (vagy `null`-t, ha nincs kitiltva).
- `Punishment getActiveBan(UUID playerUuid)`
- `Punishment getActiveBan(String playerName)`
- `Punishment getActiveBanByIp(String ipAddress)`

#### `getActiveMute`
Visszaadja a játékos jelenleg aktív némítását (vagy `null`-t, ha nincs némítva).
- `Punishment getActiveMute(UUID playerUuid)`
- `Punishment getActiveMute(String playerName)`
- `Punishment getActiveMuteByIp(String ipAddress)`

#### `getWarnings`
Lekéri a játékos jelenleg érvényes figyelmeztetéseit.
- `List<Punishment> getWarnings(UUID playerUuid)`
- `List<Punishment> getWarnings(String playerName)`

#### `getAlts`
Visszaadja az egyező IP-cím alapján talált alternatív fiókok (alts) nevének listáját.
- `List<String> getAlts(UUID playerUuid)`
- `List<String> getAlts(String playerName)`

#### Status Checkerek (`boolean`)
- `boolean isBanned(UUID/String/IP)` – Igaz, ha aktív ban alatt áll.
- `boolean isMuted(UUID/String/IP)` – Igaz, ha aktív mute alatt áll.
- `boolean isFrozen(UUID/String)` – Igaz, ha a játékos fagyasztva van (`/freeze`).
- `boolean isLockdownActive()` – Igaz, ha a szerver zárolva van (`/lockdown`).
- `String getLockdownReason()` – Visszaadja a zárolás indokát.

---

## 3. Részletes Java API Metódus Referencia

### B) Szankció Végrehajtó Metódusok (Execution Methods)

Minden végrehajtó metódus meghívja a wapeB `PlayerPunishEvent` eseményét! Ha egy listener megszakítja az eseményt (`event.setCancelled(true)`), a metódus `false` értékkel tér vissza.

#### `banPlayer`
```java
boolean success = api.banPlayer(
    "JátékosNév",             // Célszemély neve vagy UUID
    "Csalás / Hacking",        // Indok
    "Console",                // Végrehajtó neve (tetszőleges szöveg)
    86400000L,                // Időtartam ms-ben (-1 = Örök kitiltás)
    false,                    // Silent (néma bejelentés)?
    false                     // IP-ban is legyen?
);
```

#### `mutePlayer`
```java
boolean success = api.mutePlayer(
    "JátékosNév", 
    "Spam a chaten", 
    "ywxlol - DISCORD",       // Egyedi végrehajtó név
    3600000L,                 // Időtartam: 1 óra (ms)
    false,                    // Silent
    false                     // IP-mute
);
```

#### `warnPlayer`
```java
boolean success = api.warnPlayer("JátékosNév", "Trágár beszéd", "AdminName", false);
```

#### `kickPlayer`
```java
boolean success = api.kickPlayer("JátékosNév", "AFK túl hosszú ideje", "System", false);
```

#### `freezePlayer` / `unfreezePlayer`
```java
api.freezePlayer("JátékosNév", "Képernyőmegosztás szükséges", "StaffMember");
api.unfreezePlayer("JátékosNév", "StaffMember");
```

#### `unbanPlayer` / `unmutePlayer` / `revokePunishment`
```java
api.unbanPlayer("JátékosNév", "Téves ban", "Admin");
api.unmutePlayer("JátékosNév", "Kérelem elfogadva", "Admin");
api.revokePunishment(105, "Admin"); // Büntetés törlése ID alapján
```

---

### C) Parancs Átírási Metódusok (Command Alias Methods)

Saját parancs-aliasokat regisztrálhatsz futásidőben:
```java
// Hozzáadja a /kitiltas aliast a /ban parancshoz
api.registerCommandAlias("ban", "kitiltas");

// Lekéri a bejegyzett egyedi aliasokat
List<String> aliases = api.getCommandAliases("ban");
```

---

## 4. Bukkit Custom Eventek (Eseménykezelés)

A wapeB Bukkit eseményeket generál a büntetések kiszabása és feloldása előtt.

### 1. `PlayerPunishEvent` (Cancellable)
Minden szankció kiszabásakor (akár parancsból, akár GUI-ból, akár Web API-ból, akár kódból indították) lefut.

#### Elérhető Metódusok az Eventben:
- `getPlayerUuid()` / `getPlayerName()` / `getIpAddress()`
- `getType()` – `PunishmentType` (BAN, TEMPBAN, MUTE, WARN, KICK, stb.)
- `getReason()` / `setReason(String)` – **Módosítható indok**
- `getExecutor()` / `setExecutor(String)` – **Módosítható végrehajtó név** *(pl. Discord név felülírásához)*
- `getDuration()` / `setDuration(long)` – **Módosítható időtartam**
- `isSilent()` / `setSilent(boolean)` – **Módosítható néma mód**
- `isCancelled()` / `setCancelled(boolean)` – **Esemény megszakítása**

#### Példa Listener:
```java
import dev.azuyo.wapeB.api.events.PlayerPunishEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PunishListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPunish(PlayerPunishEvent event) {
        // VIP védelem példa:
        if (event.getPlayerName().startsWith("VIP_") && event.getType().name().contains("BAN")) {
            event.setCancelled(true);
            return;
        }

        // Végrehajtó nevének felülbírálása futás közben:
        if (event.getExecutor().equalsIgnoreCase("Console")) {
            event.setExecutor("Automatikus Védelmi Rendszer");
        }
    }
}
```

### 2. `PlayerUnpunishEvent` (Cancellable)
Unban / Unmute / Unwarn esetén fut le:
- `getPunishment()` – A feloldandó `Punishment` objektum.
- `getExecutor()` – A feloldást végző személy neve.

---

## 5. Integrációs Minták és Kódpéldák

### Minta 1: Egyedi Némító Parancs (GMute)
Egy pehelykönnyű parancs, ami tetszőleges szankcionáló névvel némít:

```java
public class GMuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /gmute <játékos> <indok> <végrehajtó>
        String target = args[0];
        String reason = args[1];
        String executor = args[2];

        WapeB.getApi().mutePlayer(target, reason, executor, -1, false, false);
        sender.sendMessage("§aSikeresen némítva! Végrehajtó: " + executor);
        return true;
    }
}
```

---

### Minta 2: Discord Bot (SyncCord / DiscordSRV) Végrehajtó Átírás
Ha egy Discord bot ad ki Minecraft parancsot a konzolon keresztül, a `PlayerPunishEvent` segítségével a konzol neve helyett a Discord felhasználó Minecraft neve kerül mentésre:

```java
// 1. A parancs kiadásakor a Bukkit főszálán eltároljuk a nevet ThreadLocal-ban:
Bukkit.getScheduler().runTask(plugin, () -> {
    try {
        WapeBHook.setCurrentDiscordExecutor("ywxlol");
        Bukkit.dispatchCommand(customSender, "mute pistike 1h spam");
    } finally {
        WapeBHook.clearCurrentDiscordExecutor();
    }
});

// 2. A Listener felülírja a végrehajtó nevét a wapeB eseményében:
@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
public void onPlayerPunish(PlayerPunishEvent event) {
    String discordExecutor = WapeBHook.getActiveDiscordExecutor();
    if (discordExecutor != null && !discordExecutor.trim().isEmpty()) {
        event.setExecutor(discordExecutor + " - DISCORD");
    }
}
```

---

### Minta 3: Chat Tag & Mute Jelzés
Annak megjelenítése a chaten vagy Discordon, ha egy játékos némítva van:

```java
@EventHandler
public void onChat(AsyncPlayerChatEvent event) {
    WapeBAPI api = WapeB.getApi();
    if (api != null && api.isMuted(event.getPlayer().getUniqueId())) {
        Punishment mute = api.getActiveMute(event.getPlayer().getUniqueId());
        String reason = (mute != null) ? mute.getReason() : "Némítva";
        
        event.getPlayer().sendMessage("§cNem tudsz írni, mert némítva vagy! Indok: " + reason);
        event.setCancelled(true);
    }
}
```

---

## 6. HTTP REST Web API Referencia

A wapeB beépített HTTP webszerverrel rendelkezik a távoli kezeléshez (pl. Web Dashboard, Discord botok).

- **Fejléc**: `X-API-Key: YOUR_API_KEY_HERE`

### Végpontok Összefoglalója:

| Végpont | Metódus | Paraméterek | Leírás |
|---|---|---|---|
| `/api/player/punishments` | GET | `player=Név` | Játékos összes büntetésének lekérése JSON tömbként |
| `/api/player/checkban` | GET | `player=Név` | Aktív ban státusz és részletek |
| `/api/player/checkmute` | GET | `player=Név` | Aktív mute státusz és részletek |
| `/api/commands/list` | GET | - | Parancsok és beállított aliasok listázása |
| `/api/punish/execute` | GET/POST | `target=Név&type=BAN&reason=Indok&duration=1d` | Szankció kiszabása Web API-n át |
| `/api/punish/remove` | GET/POST | `id=105` | Szankció törlése ID alapján |
| `/api/stats` | GET | - | Napi és órás szankció statisztikák |
| `/api/lockdown` | GET/POST | `action=on&reason=Karbantartás` | Szerver zárolás kezelése |

---

## 📜 Licenc és Támogatás
Készült Spigot / Paper 1.18.2 - 1.21.x Minecraft szerverekhez.
GitHub Repository: [https://github.com/lftkraft/wapeB](https://github.com/lftkraft/wapeB)
