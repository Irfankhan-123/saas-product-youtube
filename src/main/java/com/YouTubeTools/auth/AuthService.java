package com.YouTubeTools.auth;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    // 👉 Yaha apna Google Apps Script URL paste karna
    private final String SHEET_URL = "https://script.google.com/macros/s/AKfycbxrOYtlecdE0YpBJ3hQvksS8eugAPg4SrBXEaF4pbThyx9JvLwbivotO9HRZvkQAl_eRA/exec";

    public Object login(String email, String password) {
        String url = SHEET_URL + "?email=" + email + "&password=" + password;

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, Object.class);
    }
    public Object register(Object body) {
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.postForObject(SHEET_URL, body, String.class);
}
}