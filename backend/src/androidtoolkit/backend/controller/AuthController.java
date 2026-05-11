package androidtoolkit.backend.controller;

import androidtoolkit.backend.entity.AccessGrant;
import androidtoolkit.backend.entity.RefreshToken;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.AccessGrantRepository;
import androidtoolkit.backend.repository.PendingInviteRepository;
import androidtoolkit.backend.repository.RefreshTokenRepository;
import androidtoolkit.backend.repository.UserRepository;
import androidtoolkit.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final PendingInviteRepository pendingInviteRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                          AccessGrantRepository accessGrantRepository, PendingInviteRepository pendingInviteRepository,
                          JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.pendingInviteRepository = pendingInviteRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String email = body.getOrDefault("email", "").trim();
        String password = body.getOrDefault("password", "");

        if (username.isEmpty() || email.isEmpty() || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username, email, and password (min 6 chars) required");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        userRepository.save(user);

        // Convert any pending invites for this email into actual grants
        var pendingInvites = pendingInviteRepository.findByInvitedEmail(email);
        for (var invite : pendingInvites) {
            accessGrantRepository.save(new AccessGrant(invite.getOwner(), user, true));
            // Apply the assigned tier from the invite (use the highest tier if multiple invites)
            if (invite.getAssignedTier() != null && invite.getAssignedTier().ordinal() > user.getTier().ordinal()) {
                user.setTier(invite.getAssignedTier());
            }
        }
        if (!pendingInvites.isEmpty()) {
            userRepository.save(user);
            pendingInviteRepository.deleteAll(pendingInvites);
        }

        return Map.of("message", "Registration successful", "username", username);
    }

    @PostMapping("/login")
    @Transactional
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        boolean rememberMe = Boolean.parseBoolean(body.getOrDefault("rememberMe", "false"));
        String deviceFingerprint = body.getOrDefault("deviceFingerprint", "unknown");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Non-admin users must be in the owner's allowed list
        if (user.getRole() != User.Role.ADMIN) {
            User owner = userRepository.findByUsername("admin").orElse(null);
            if (owner != null && !accessGrantRepository.existsByOwnerIdAndGrantedUserId(owner.getId(), user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access not granted. Ask the device owner to add you.");
            }
        }

        String accessToken = jwtService.generateAccessToken(user);

        Map<String, Object> response = new java.util.HashMap<>(Map.of(
                "accessToken", accessToken,
                "username", user.getUsername(),
                "tier", user.getTier().name(),
                "role", user.getRole().name()
        ));

        if (rememberMe) {
            // Remove old refresh token for this device
            refreshTokenRepository.deleteByUserIdAndDeviceFingerprint(user.getId(), deviceFingerprint);
            // Create new refresh token (30 days)
            String refreshToken = UUID.randomUUID().toString();
            refreshTokenRepository.save(new RefreshToken(
                    refreshToken, user, deviceFingerprint,
                    Instant.now().plus(30, ChronoUnit.DAYS)
            ));
            response.put("refreshToken", refreshToken);
        }

        return response;
    }

    @PostMapping("/refresh")
    @Transactional
    public Map<String, Object> refresh(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("refreshToken", "");
        String deviceFingerprint = body.getOrDefault("deviceFingerprint", "unknown");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        if (!refreshToken.getDeviceFingerprint().equals(deviceFingerprint)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device mismatch");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user);

        return Map.of(
                "accessToken", accessToken,
                "username", user.getUsername(),
                "tier", user.getTier().name(),
                "role", user.getRole().name()
        );
    }

    @PostMapping("/logout")
    @Transactional
    public Map<String, String> logout(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("refreshToken", "");
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
        return Map.of("message", "Logged out");
    }
}
