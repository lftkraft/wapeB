package dev.azuyo.wapeB.importers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ImportManager {

    private final WapeB plugin;
    private final Map<String, PunishmentImporter> registeredImporters = new ConcurrentHashMap<>();

    public ImportManager(WapeB plugin) {
        this.plugin = plugin;
        registerDefaultImporters();
    }

    private void registerDefaultImporters() {
        registerImporter(new LiteBansImporter(plugin));
        registerImporter(new AdvancedBanImporter(plugin));
        registerImporter(new VanillaImporter(plugin));
    }

    /**
     * Registers a custom punishment importer (for external plugins/developers).
     */
    public void registerImporter(PunishmentImporter importer) {
        if (importer == null || importer.getName() == null) return;
        registeredImporters.put(importer.getName().toLowerCase(), importer);
    }

    /**
     * Retrieves an importer by name.
     */
    public PunishmentImporter getImporter(String name) {
        if (name == null) return null;
        return registeredImporters.get(name.toLowerCase());
    }

    /**
     * Returns all currently registered importers.
     */
    public List<PunishmentImporter> getRegisteredImporters() {
        return new ArrayList<>(registeredImporters.values());
    }

    /**
     * Executes an import asynchronously using a registered importer name.
     */
    public CompletableFuture<ImportResult> executeImport(String importerName, Map<String, Object> options) {
        PunishmentImporter importer = getImporter(importerName);
        if (importer == null) {
            ImportResult failed = new ImportResult(importerName, false);
            failed.addError("No importer found with name: " + importerName);
            return CompletableFuture.completedFuture(failed);
        }
        return importer.executeImport(options);
    }

    /**
     * Direct batch import of Punishment objects into wapeB database.
     */
    public CompletableFuture<ImportResult> importBatch(List<Punishment> punishments) {
        CompletableFuture<ImportResult> future = new CompletableFuture<>();
        if (punishments == null || punishments.isEmpty()) {
            ImportResult res = new ImportResult("batch", true);
            res.addDetail("No punishments to import (empty list).");
            future.complete(res);
            return future;
        }

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            ImportResult res = new ImportResult("batch", true);

            for (Punishment p : punishments) {
                try {
                    plugin.getDataManager().savePunishment(p);
                    res.incrementImported();
                } catch (Exception e) {
                    res.incrementFailed();
                    res.addError("Failed saving punishment: " + e.getMessage());
                }
            }

            res.setDurationMillis(System.currentTimeMillis() - start);
            res.addDetail("Batch import completed: " + res.getImportedCount() + " imported, " + res.getFailedCount() + " failed.");
            future.complete(res);
        });

        return future;
    }
}
