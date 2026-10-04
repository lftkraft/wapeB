package dev.azuyo.wapeB.ai;

import com.google.gson.Gson;
import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.ConfigurationSection;

import dev.azuyo.wapeB.utils.ChatMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GroqApiClient {
    private final WapeB plugin;
    private final HttpClient httpClient;
    private final Gson gson;
    private final List<String> apiKeys = new ArrayList<>();
    private final AtomicInteger keyIndex = new AtomicInteger(0);
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
            // Load multiple API keys if configured
            List<String> keyList = aiSection.getStringList("groq-api-keys");
            if (keyList != null && !keyList.isEmpty()) {
                for (String k : keyList) {
                    if (k != null && !k.trim().isEmpty() && !k.contains("YOUR_GROQ_API_KEY")) {
                        this.apiKeys.add(k.trim());
                    }
                }
            }
            
            // Fallback / single key support
            String singleKey = aiSection.getString("groq-api-key", "");
            if (singleKey != null && !singleKey.trim().isEmpty() && !singleKey.contains("YOUR_GROQ_API_KEY") && !this.apiKeys.contains(singleKey.trim())) {
                this.apiKeys.add(singleKey.trim());
            }

            this.model = aiSection.getString("groq-model", "llama-3.1-8b-instant");
            this.prompt = aiSection.getString("prompt", "");
            this.maxRetries = aiSection.getInt("max-retries", 3);
            this.retryDelay = aiSection.getInt("retry-delay-seconds", 10);
        } else {
            this.model = "llama-3.1-8b-instant";
            this.prompt = "";
            this.maxRetries = 3;
            this.retryDelay = 10;
            plugin.getLogger().warning("Sentinel AI configuration section not found!");
        }

        if (apiKeys.isEmpty()) {
            plugin.getLogger().warning("No valid Groq API key found in config. Sentinel AI will be inactive.");
        } else {
            plugin.getLogger().info("Groq API Client initialized with " + apiKeys.size() + " active API key(s).");
        }
    }

    private String getNextApiKey() {
        if (apiKeys.isEmpty()) return "";
        int index = Math.abs(keyIndex.getAndIncrement() % apiKeys.size());
        return apiKeys.get(index);
    }

    public CompletableFuture<AIResponse> analyzeChatMessage(String message) {
        return analyzeChatMessage(null, message, null);
    }

    public CompletableFuture<AIResponse> analyzeChatMessage(String senderName, String message, List<ChatMessage> history) {
        CompletableFuture<AIResponse> future = new CompletableFuture<>();
        
        if (apiKeys.isEmpty() || prompt.isEmpty()) {
            future.complete(new AIResponse(false, "API not configured"));
            return future;
        }

        StringBuilder userContent = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            userContent.append("=== ELŐZMÉNYEK (Kontextus) ===\n");
            for (ChatMessage cm : history) {
                userContent.append("[").append(cm.getPlayerName() != null ? cm.getPlayerName() : "Unknown")
                           .append("]: ").append(cm.getMessage()).append("\n");
            }
            userContent.append("=== VIZSGÁLANDÓ ÜZENET ===\n");
        }

        if (senderName != null && !senderName.isEmpty()) {
            userContent.append("[").append(senderName).append("]: ");
        }
        userContent.append(message);

        String currentApiKey = getNextApiKey();
        sendRequestWithApiKey(userContent.toString(), currentApiKey, future, 0);
        return future;
    }

    private void sendRequestWithApiKey(String userContent, String currentApiKey, CompletableFuture<AIResponse> future, int attempt) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", prompt),
                Map.of("role", "user", "content", userContent)
            ),
            "response_format", Map.of("type", "json_object")
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
            .header("Authorization", "Bearer " + currentApiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();

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
                    // If multiple keys exist, switch to next key immediately, otherwise wait with delay
                    String nextKey = getNextApiKey();
                    if (apiKeys.size() > 1 && !nextKey.equals(currentApiKey)) {
                        sendRequestWithApiKey(userContent, nextKey, future, attempt + 1);
                    } else {
                        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, 
                            () -> sendRequestWithApiKey(userContent, nextKey, future, attempt + 1), 
                            retryDelay * 20L);
                    }
                } else {
                    future.completeExceptionally(new RuntimeException("API error: " + response.statusCode() + " - " + response.body()));
                }
            })
            .exceptionally(e -> {
                if (attempt < maxRetries) {
                    String nextKey = getNextApiKey();
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, 
                        () -> sendRequestWithApiKey(userContent, nextKey, future, attempt + 1), 
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
