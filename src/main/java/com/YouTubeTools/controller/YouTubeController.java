package com.YouTubeTools.controller;

import com.YouTubeTools.service.YouTubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class YouTubeController {

    @Autowired
    private YouTubeService service;

    // 🔹 Search Videos
    @GetMapping("/search")
    public Object search(@RequestParam String keyword) {
        return service.searchVideos(keyword);
    }

    // 🔹 Thumbnail
    @GetMapping("/thumbnail")
    public Object thumbnail(@RequestParam String url) {
        return service.getThumbnail(url);
    }

    // 🔥 AI Generate (ONLY ONE)
    @PostMapping("/ai")
    public Object generateAI(@RequestBody Map<String, String> body) {
        return service.generateAI(body.get("text"));
    }

    // 🔹 TEST API (browser ke liye)
    @GetMapping("/ai-test")
    public Object testAI(@RequestParam String text) {
        return service.generateAI(text);
    }
    // 🔥 VIDEO ANALYZER (ADD THIS)
    @GetMapping("/video")
    public Object getVideo(@RequestParam String url) {
        return service.getVideoDetails(url);
    }

    // 🔥 SUMMARY (ADD THIS)
    @GetMapping("/summary")
    public Object summary(@RequestParam String url) {
        return service.getSummary(url);
    }
}