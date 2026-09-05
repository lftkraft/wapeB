# 🔨 wapeB - Comprehensive Minecraft Punishment System & API

A powerful, modern Paper/Spigot punishment plugin featuring a comprehensive **Java API**, **Bukkit Event System**, **Dynamic Command Overrides**, and **HTTP REST Web API**.

[![JitPack](https://jitpack.io/v/lftkraft/wapeB.svg)](https://jitpack.io/#lftkraft/wapeB)

---

## 📌 Installation / Dependency Setup

### Gradle (`build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.lftkraft:wapeB:v1.0.5'
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
        <version>v1.0.5</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### `plugin.yml`
```yaml
name: MyPlugin
version: 1.0.0
main: dev.example.myplugin.Main
depend: [wapeB]
```

---

## ☕ Java API Usage

### Accessing the API
```java
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.WapeBAPI;
import dev.azuyo.wapeB.api.WapeBAPIProvider;

// Option 1:
WapeBAPI api = WapeB.getApi();

// Option 2:
WapeBAPI api = WapeBAPIProvider.getAPI();
```

### Key API Methods

#### Querying Status & Punishments
```java
// Check active states
boolean isBanned = api.isBanned("PlayerName");
boolean isMuted = api.isMuted("PlayerName");
boolean isFrozen = api.isFrozen("PlayerName");

// Get active punishment details
Punishment activeBan = api.getActiveBan("PlayerName");
Punishment activeMute = api.getActiveMute("PlayerName");

// Get all punishments & history
List<Punishment> history = api.getHistory("PlayerName");
List<String> alts = api.getAlts("PlayerName");
```

#### Executing Sanctions
```java
// Ban player (duration in ms, -1 for permanent)
api.banPlayer("PlayerName", "Cheating", "Console", 86400000L, false, false);

// Mute player
api.mutePlayer("PlayerName", "Spam", "Admin", 3600000L, false, false);

// Warn player
api.warnPlayer("PlayerName", "Swearing", "Staff", false);

// Kick player
api.kickPlayer("PlayerName", "AFK", "System", false);

// Freeze / Unfreeze
api.freezePlayer("PlayerName", "ScreenShare", "Staff");
api.unfreezePlayer("PlayerName", "Staff");

// Revoke punishments
api.unbanPlayer("PlayerName", "Appeal accepted", "Admin");
api.unmutePlayer("PlayerName", "Appeal accepted", "Admin");
```

---

## ⚡ Bukkit Custom Events

Listen to wapeB events in your plugin:

```java
import dev.azuyo.wapeB.api.events.PlayerPunishEvent;
import dev.azuyo.wapeB.api.events.PlayerUnpunishEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyPunishListener implements Listener {

    @EventHandler
    public void onPunish(PlayerPunishEvent event) {
        // Inspect or modify punishment details before execution
        if (event.getPlayerName().startsWith("VIP_")) {
            event.setCancelled(true); // Cancel punishment for VIPs
        }

        // Override executor name on the fly
        event.setExecutor("Custom AntiCheat System");
    }

    @EventHandler
    public void onUnpunish(PlayerUnpunishEvent event) {
        System.out.println("Punishment revoked: " + event.getPunishment().getId());
    }
}
```

---

## ⚙️ Dynamic Command Overrides

Admins can configure custom command aliases in `config.yml`:

```yaml
command-overrides:
  ban:
    - kitiltas
    - b
  mute:
    - nemit
  freeze:
    - lefagyaszt
```

Or programmatically via Java API:
```java
WapeB.getApi().registerCommandAlias("ban", "szankcio-ban");
```

---

## 🌐 Web REST API Endpoints

wapeB includes a lightweight HTTP REST server. Pass header `X-API-Key: YOUR_API_KEY`.

- `GET /api/player/punishments?player=PlayerName` - Fetch all punishments for a player.
- `GET /api/player/checkban?player=PlayerName` - Check active ban status.
- `GET /api/player/checkmute?player=PlayerName` - Check active mute status.
- `GET /api/commands/list` - List registered commands and custom aliases.
- `POST /api/punish/execute` - Execute a punishment via REST.
- `POST /api/punish/remove` - Remove a punishment via REST.

---

## 📜 License
Developed for Spigot/Paper Minecraft Servers.
