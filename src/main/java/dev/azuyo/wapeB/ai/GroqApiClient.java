package dev.azuyo.wapeB.ai;

import com.google.gson.Gson;
import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.ConfigurationSection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GroqApiClient {
    private final WapeB plugin;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String model;
    private final String prompt;
    private final int maxRetries;
    private final int retryDelay;

    public GroqApiClient(WapeB plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        
        ConfigurationSection aiSection = plugin.getConfigManager().getConfig().getConfigurationSection("sentinel.ai");
        if (aiSection != null) {
            this.apiKey = aiSection.getString("groq-api-key", "");
            this.model = aiSection.getString("groq-model", "llama-3.1-8b-instant");
            this.prompt = aiSection.getString("prompt", "");
            this.maxRetries = aiSection.getInt("max-retries", 3);
            this.retryDelay = aiSection.getInt("retry-delay-seconds", 10);
        } else {
            this.apiKey = "";
            this.model = "llama-3.1-8b-instant";
            this.prompt = "";
            this.maxRetries = 3;
            this.retryDelay = 10;
            plugin.getLogger().warning("Sentinel AI configuration section not found!");
        }
    }

    public CompletableFuture<AIResponse> analyzeChatMessage(String message) {
        CompletableFuture<AIResponse> future = new CompletableFuture<>();
        
        if (apiKey.isEmpty() || prompt.isEmpty()) {
            plugin.getLogger().warning("Groq API key or prompt is not configured.");
            future.complete(new AIResponse(false, "API not configured"));
            return future;
        }

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", prompt),
                Map.of("role", "user", "content", message)
            ),
            "response_format", Map.of("type", "json_object")
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();

        sendRequestWithRetry(request, future, 0);
        return future;
    }

    private void sendRequestWithRetry(HttpRequest request, CompletableFuture<AIResponse> future, int attempt) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() == 200) {
                    try {
                        Map<String, Object> jsonResponse = gson.fromJson(response.body(), Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");
                        String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                        future.complete(gson.fromJson(content, AIResponse.class));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                } else if (response.statusCode() == 429 && attempt < maxRetries) {
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, 
                        () -> sendRequestWithRetry(request, future, attempt + 1), 
                        retryDelay * 20L);
                } else {
                    future.completeExceptionally(new RuntimeException("API error: " + response.statusCode() + " - " + response.body()));
                }
            })
            .exceptionally(e -> {
                if (attempt < maxRetries) {
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, 
                        () -> sendRequestWithRetry(request, future, attempt + 1), 
                        retryDelay * 20L);
                } else {
                    future.completeExceptionally(e);
                }
                return null;
            });
    }

    public static class AIResponse {
        private final boolean should_mute;
        private final String reason;

        public AIResponse(boolean should_mute, String reason) {
            this.should_mute = should_mute;
            this.reason = reason;
        }

        public boolean isShouldMute() {
            return should_mute;
        }

        public String getReason() {
            return reason;
        }
    }
}
