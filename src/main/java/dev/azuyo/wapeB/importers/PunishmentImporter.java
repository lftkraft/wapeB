package dev.azuyo.wapeB.importers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for extensible punishment importers.
 */
public interface PunishmentImporter {

    /**
     * Unique identifier of the importer (e.g. "litebans", "advancedban", "vanilla").
     */
    String getName();

    /**
     * Human-readable display description.
     */
    String getDescription();

    /**
     * Executes the import process asynchronously.
     * @param options Custom options/parameters passed to the importer (e.g. database path, override flag).
     * @return Future with the ImportResult summary.
     */
    CompletableFuture<ImportResult> executeImport(Map<String, Object> options);
}
