package main.java.CalenderApp.demo.controller;

import CalenderApp.demo.controller.dto.AuthLoginRequest;
import CalenderApp.demo.controller.dto.AuthRegisterRequest;
import CalenderApp.demo.controller.dto.AuthResponse;
import CalenderApp.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return new AuthResponse(authService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return new AuthResponse(authService.login(request.username(), request.password()));
    }
}
