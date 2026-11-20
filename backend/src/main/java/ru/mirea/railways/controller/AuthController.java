package ru.mirea.railways.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.security.CurrentUser;
import ru.mirea.railways.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest r) { return auth.refresh(r); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest r) {
        auth.logout(r);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me() { return new CurrentUserResponse(auth.me(CurrentUser.email())); }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest r) {
        auth.changePassword(CurrentUser.email(), r);
        return ResponseEntity.noContent().build();
    }
}