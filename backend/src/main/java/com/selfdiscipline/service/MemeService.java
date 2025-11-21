package com.selfdiscipline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MemeService {

    @Autowired
    private AIService aiService;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Category Name -> List of Image Paths
    private final Map<String, List<String>> memeLibrary = new ConcurrentHashMap<>();
    // Category Name -> Embedding
    private final Map<String, List<Double>> categoryEmbeddings = new ConcurrentHashMap<>();

    private static final String GITHUB_TREE_URL = "https://api.github.com/repos/zhaoolee/ChineseBQB/git/trees/master?recursive=1";
    private static final String RAW_BASE_URL = "https://raw.githubusercontent.com/zhaoolee/ChineseBQB/master/";

    @PostConstruct
    public void init() {
        new Thread(this::loadMemes).start();
    }

    private void loadMemes() {
        try {
            String response = webClient.get()
                    .uri(GITHUB_TREE_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode tree = root.path("tree");

            if (tree.isArray()) {
                boolean first = true;
                for (JsonNode node : tree) {
                    String path = node.path("path").asText();
                    if (isImage(path)) {
                        String category = extractCategory(path);
                        if (first) {
                            System.out.println("First loaded category: " + category);
                            System.out.println("Hex: " + java.util.Arrays.toString(category.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                            first = false;
                        }
                        memeLibrary.computeIfAbsent(category, k -> new ArrayList<>()).add(path);
                    }
                }
            }
            
            // Pre-calculate embeddings for categories
            for (String category : memeLibrary.keySet()) {
                // Clean category name (remove numbers, underscores, and "BQB")
                String cleanName = category.replaceAll("^[0-9]+_", "")
                                           .replaceAll("_", " ")
                                           .replaceAll("(?i)bqb", "") // Remove BQB/bqb
                                           .trim();
                
                List<Double> embedding = aiService.generateEmbeddings(cleanName);
                if (!embedding.isEmpty()) {
                    categoryEmbeddings.put(category, embedding);
                }
            }
            System.out.println("Loaded " + memeLibrary.size() + " meme categories.");
        } catch (Exception e) {
            System.err.println("Failed to load memes from GitHub: " + e.getMessage());
        }
    }

    private boolean isImage(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif");
    }

    private String extractCategory(String path) {
        // Path example: "005_可爱/01.jpg" -> "005_可爱"
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }
        return "Uncategorized";
    }

    public String findBestMeme(String text) {
        if (categoryEmbeddings.isEmpty()) {
            System.out.println("MemeService: No category embeddings available. Returning random meme.");
            return getRandomMeme();
        }

        // Truncate text to avoid excessive length issues (e.g. first 300 chars)
        String processedText = text.length() > 300 ? text.substring(0, 300) : text;
        
        // Debug input text encoding
        System.out.println("Processing text hex: " + java.util.Arrays.toString(processedText.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        List<Double> textEmbedding = aiService.generateEmbeddings(processedText);
        if (textEmbedding.isEmpty()) {
            System.out.println("MemeService: Failed to generate embedding for text. Returning random meme.");
            return getRandomMeme();
        }

        String bestCategory = null;
        double maxSimilarity = -1.0;
        
        // PriorityQueue to keep track of top matches for debugging
        PriorityQueue<Map.Entry<String, Double>> topMatches = new PriorityQueue<>(
            (a, b) -> Double.compare(b.getValue(), a.getValue())
        );

        for (Map.Entry<String, List<Double>> entry : categoryEmbeddings.entrySet()) {
            double similarity = cosineSimilarity(textEmbedding, entry.getValue());
            topMatches.offer(new AbstractMap.SimpleEntry<>(entry.getKey(), similarity));
            
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestCategory = entry.getKey();
            }
        }
        
        // Log top 5 matches
        System.out.println("--- Meme Matching for text: [" + processedText.substring(0, Math.min(20, processedText.length())) + "...] ---");
        int count = 0;
        while (!topMatches.isEmpty() && count < 5) {
            Map.Entry<String, Double> match = topMatches.poll();
            System.out.printf("Category: %s, Score: %.4f%n", match.getKey(), match.getValue());
            count++;
        }

        if (bestCategory != null) {
            List<String> images = memeLibrary.get(bestCategory);
            if (images != null && !images.isEmpty()) {
                String randomImage = images.get(new Random().nextInt(images.size()));
                return RAW_BASE_URL + randomImage;
            }
        }

        return getRandomMeme();
    }

    private String getRandomMeme() {
        if (memeLibrary.isEmpty()) return null;
        List<String> keys = new ArrayList<>(memeLibrary.keySet());
        String randomKey = keys.get(new Random().nextInt(keys.size()));
        List<String> images = memeLibrary.get(randomKey);
        if (images != null && !images.isEmpty()) {
            return RAW_BASE_URL + images.get(new Random().nextInt(images.size()));
        }
        return null;
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
