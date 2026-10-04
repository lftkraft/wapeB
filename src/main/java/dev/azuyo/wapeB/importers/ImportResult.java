package dev.azuyo.wapeB.importers;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {

    private final boolean success;
    private final String sourceName;
    private int importedCount = 0;
    private int skippedCount = 0;
    private int failedCount = 0;
    private long durationMillis = 0;
    private final List<String> errors = new ArrayList<>();
    private final List<String> details = new ArrayList<>();

    public ImportResult(String sourceName, boolean success) {
        this.sourceName = sourceName;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSource() {
        return sourceName;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void incrementImported() {
        this.importedCount++;
    }

    public void addImported(int count) {
        this.importedCount += count;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void incrementFailed() {
        this.failedCount++;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getDetails() {
        return details;
    }

    public void addDetail(String detail) {
        this.details.add(detail);
    }

    public int getTotalProcessed() {
        return importedCount + skippedCount + failedCount;
    }
}
