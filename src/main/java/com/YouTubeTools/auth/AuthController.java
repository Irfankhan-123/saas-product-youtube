package com.YouTubeTools.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public Object login(@RequestParam String email, @RequestParam String password) {
        return authService.login(email, password);
    }
    @PostMapping("/register")
public Object register(@RequestBody Map<String, String> body) {
    return authService.register(body);
}
}