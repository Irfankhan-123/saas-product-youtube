package com.YouTubeTools.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.*;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String API_KEY;

    @Value("${gemini.api.key}")
    private String apiKey;

    // 🔹 Extract Video ID
    public String extractVideoId(String url) {
        try {
            if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
            if (url.contains("/live/")) return url.split("/live/")[1].split("\\?")[0];
            if (url.contains("/shorts/")) return url.split("/shorts/")[1].split("\\?")[0];

            Matcher m = Pattern.compile("v=([^&]+)").matcher(url);
            if (m.find()) return m.group(1);

        } catch (Exception e) {}
        return null;
    }

    // 🔹 Thumbnail
    public Map<String, String> getThumbnail(String url) {
        String id = extractVideoId(url);
        Map<String, String> map = new HashMap<>();
        map.put("HD", "https://img.youtube.com/vi/" + id + "/maxresdefault.jpg");
        map.put("HQ", "https://img.youtube.com/vi/" + id + "/hqdefault.jpg");
        return map;
    }

    // 🔹 Search
    public List<Map<String, String>> searchVideos(String keyword) {

        String api = "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet&type=video&maxResults=5&q=" + keyword
                + "&key=" + API_KEY;

        RestTemplate rt = new RestTemplate();
        Map res = rt.getForObject(api, Map.class);

        List<Map<String, String>> list = new ArrayList<>();

        List items = (List) res.get("items");

        for (Object o : items) {
            Map item = (Map) o;
            Map id = (Map) item.get("id");
            Map sn = (Map) item.get("snippet");

            String vid = (String) id.get("videoId");
            Map thumb = (Map)((Map)sn.get("thumbnails")).get("high");

            Map<String, String> v = new HashMap<>();
            v.put("title", (String) sn.get("title"));
            v.put("thumbnail", (String) thumb.get("url"));
            v.put("url", "https://www.youtube.com/watch?v=" + vid);

            list.add(v);
        }

        return list;
    }

    // ✅ DO NOT TOUCH - Gemini AI (working perfectly)
    public Map<String, String> generateAI(String text) {

        Map<String, String> result = new HashMap<>();

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;
            RestTemplate restTemplate = new RestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> part = new HashMap<>();
            part.put("text", "Generate YouTube SEO optimized Title, Description and Tags for: " + text +
                    ". Respond strictly in this format:\nTitle: <title here>\nDescription: <description here>\nTags: <tags here>");

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(content));

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<Map> response =
                    restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, Map.class);

            Map responseBody = response.getBody();
            System.out.println("Gemini Response: " + responseBody);

            List candidates = (List) responseBody.get("candidates");
            Map first = (Map) candidates.get(0);
            Map contentMap = (Map) first.get("content");
            List parts = (List) contentMap.get("parts");

            String aiText = (String) ((Map) parts.get(0)).get("text");

            System.out.println("AI Raw Text: " + aiText);

            String title = "", desc = "", tags = "";
            String[] lines = aiText.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.toLowerCase().startsWith("title:")) {
                    title = line.replaceFirst("(?i)title:\\s*", "").trim();
                } else if (line.toLowerCase().startsWith("description:")) {
                    desc = line.replaceFirst("(?i)description:\\s*", "").trim();
                    while (i + 1 < lines.length && !lines[i + 1].toLowerCase().startsWith("tags:")) {
                        i++;
                        desc += " " + lines[i].trim();
                    }
                } else if (line.toLowerCase().startsWith("tags:")) {
                    tags = line.replaceFirst("(?i)tags:\\s*", "").trim();
                }
            }

            result.put("title", title);
            result.put("description", desc.trim());
            result.put("tags", tags);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("title", "AI Error: " + e.getMessage());
            result.put("description", "Check API key or internet");
            result.put("tags", "#error");
        }

        return result;
    }

    // ✅ FIXED - Video Analyzer
    public Map<String, Object> getVideoDetails(String url) {
        Map<String, Object> result = new HashMap<>();

        try {
            String videoId = extractVideoId(url);

            if (videoId == null || videoId.isEmpty()) {
                result.put("error", "Invalid YouTube URL");
                return result;
            }

            String api = "https://www.googleapis.com/youtube/v3/videos"
                    + "?part=snippet,statistics&id=" + videoId
                    + "&key=" + API_KEY;

            RestTemplate rt = new RestTemplate();
            Map res = rt.getForObject(api, Map.class);

            List items = (List) res.get("items");

            if (items == null || items.isEmpty()) {
                result.put("error", "Video not found");
                return result;
            }

            Map item = (Map) items.get(0);
            Map snippet = (Map) item.get("snippet");
            Map stats = (Map) item.get("statistics");

            result.put("title", snippet.getOrDefault("title", ""));
            result.put("description", snippet.getOrDefault("description", ""));
            result.put("channel", snippet.getOrDefault("channelTitle", ""));
            result.put("views", stats != null ? stats.getOrDefault("viewCount", "0") : "0");
            result.put("likes", stats != null ? stats.getOrDefault("likeCount", "0") : "0");

            // Thumbnail with fallback chain
            String thumbnail = "";
            Map thumbnails = (Map) snippet.get("thumbnails");
            if (thumbnails != null) {
                if (thumbnails.containsKey("maxres")) {
                    thumbnail = (String) ((Map) thumbnails.get("maxres")).get("url");
                } else if (thumbnails.containsKey("high")) {
                    thumbnail = (String) ((Map) thumbnails.get("high")).get("url");
                } else if (thumbnails.containsKey("default")) {
                    thumbnail = (String) ((Map) thumbnails.get("default")).get("url");
                }
            }
            result.put("thumbnail", thumbnail);

            List<String> tags = (List<String>) snippet.get("tags");
            result.put("tags", tags != null ? tags : new ArrayList<>());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "Failed to fetch video details: " + e.getMessage());
        }

        return result;
    }

    // ✅ FIXED - Video Summary (returns JSON Map, not plain String)
    public Map<String, Object> getSummary(String url) {
        Map<String, Object> result = new HashMap<>();

        try {
            String videoId = extractVideoId(url);

            if (videoId == null || videoId.isEmpty()) {
                result.put("error", "Invalid YouTube URL");
                return result;
            }

            String api = "https://www.googleapis.com/youtube/v3/videos"
                    + "?part=snippet&id=" + videoId
                    + "&key=" + API_KEY;

            RestTemplate rt = new RestTemplate();
            Map res = rt.getForObject(api, Map.class);

            List items = (List) res.get("items");

            if (items == null || items.isEmpty()) {
                result.put("error", "Video not found");
                return result;
            }

            Map item = (Map) items.get(0);
            Map snippet = (Map) item.get("snippet");

            String title = (String) snippet.getOrDefault("title", "");
            String channel = (String) snippet.getOrDefault("channelTitle", "");
            String description = (String) snippet.getOrDefault("description", "");

            // Filter out promo/noise lines
            String[] skipKeywords = {"subscribe", "like", "follow", "instagram", "twitter",
                    "facebook", "click", "bell", "notification", "comment", "share",
                    "http", "www.", "#", "@"};

            String[] rawLines = description.split("\n");
            List<String> cleanedLines = new ArrayList<>();
            List<String> keyPoints = new ArrayList<>();

            for (String line : rawLines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                boolean skip = false;
                String lower = trimmed.toLowerCase();
                for (String kw : skipKeywords) {
                    if (lower.contains(kw)) { skip = true; break; }
                }

                if (!skip) {
                    cleanedLines.add(trimmed);
                    if (trimmed.split("\\s+").length >= 5 && keyPoints.size() < 10) {
                        keyPoints.add(trimmed);
                    }
                }
            }

            // Summary: first 250 chars of cleaned description
            String cleanedDesc = String.join(" ", cleanedLines);
            String summary = cleanedDesc.length() > 250
                    ? cleanedDesc.substring(0, 250).trim() + "..."
                    : cleanedDesc.trim();

            if (summary.isEmpty()) {
                summary = "No description available for this video.";
            }

            // Fallback key points
            if (keyPoints.isEmpty() && !cleanedLines.isEmpty()) {
                for (int i = 0; i < Math.min(5, cleanedLines.size()); i++) {
                    keyPoints.add(cleanedLines.get(i));
                }
            }

            result.put("title", title);
            result.put("channel", channel);
            result.put("summary", summary);
            result.put("keyPoints", keyPoints);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "Summary failed: " + e.getMessage());
        }

        return result;
    }
}