package androidtoolkit.backend.controller;

import androidtoolkit.backend.entity.AccessGrant;
import androidtoolkit.backend.entity.PendingInvite;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.AccessGrantRepository;
import androidtoolkit.backend.repository.PendingInviteRepository;
import androidtoolkit.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AccessGrantRepository accessGrantRepository;
    private final PendingInviteRepository pendingInviteRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();

    public AccessController(AccessGrantRepository accessGrantRepository, PendingInviteRepository pendingInviteRepository,
                            UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.accessGrantRepository = accessGrantRepository;
        this.pendingInviteRepository = pendingInviteRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    @GetMapping("/grants")
    public List<Map<String, Object>> getMyGrants() {
        User owner = getCurrentUser();
        if (owner == null) return List.of();

        List<Map<String, Object>> result = new java.util.ArrayList<>(accessGrantRepository.findByOwnerId(owner.getId()).stream()
                .map(g -> Map.<String, Object>of(
                        "username", g.getGrantedUser().getUsername(),
                        "email", g.getGrantedUser().getEmail(),
                        "permanent", g.isPermanent(),
                        "status", "active",
                        "createdAt", g.getCreatedAt().toString()
                )).toList());

        pendingInviteRepository.findByOwnerId(owner.getId()).forEach(invite ->
            result.add(Map.of(
                    "username", "(pending)",
                    "email", invite.getInvitedEmail(),
                    "permanent", true,
                    "status", "pending",
                    "createdAt", invite.getCreatedAt().toString()
            ))
        );
        return result;
    }

    @PostMapping("/grant")
    @Transactional
    public Map<String, Object> grantAccess(@RequestBody Map<String, String> body) {
        User owner = getCurrentUser();
        if (owner == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        if (!owner.canCollaborate()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADVANCED tier required for collaboration");
        }
        String email = body.getOrDefault("email", "").trim();
        if (email.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required");

        var existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User grantedUser = existingUser.get();
            if (grantedUser.getId().equals(owner.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot grant access to yourself");
            }
            if (accessGrantRepository.existsByOwnerIdAndGrantedUserId(owner.getId(), grantedUser.getId())) {
                return Map.of("message", "User already has access");
            }
            accessGrantRepository.save(new AccessGrant(owner, grantedUser, true));
            return Map.of("message", "Access granted to " + grantedUser.getUsername());
        } else {
            if (pendingInviteRepository.existsByOwnerIdAndInvitedEmail(owner.getId(), email)) {
                return Map.of("message", "Invite already sent to " + email);
            }
            pendingInviteRepository.save(new PendingInvite(owner, email));
            return Map.of("message", "Invite saved \u2014 access will be granted when " + email + " registers");
        }
    }

    @PostMapping("/revoke")
    @Transactional
    public Map<String, Object> revokeAccess(@RequestBody Map<String, String> body) {
        User owner = getCurrentUser();
        if (owner == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        String email = body.getOrDefault("email", "").trim();

        var existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            accessGrantRepository.deleteByOwnerIdAndGrantedUserId(owner.getId(), existingUser.get().getId());
            // Notify the revoked user to logout
            messagingTemplate.convertAndSend("/topic/access-revoked/" + existingUser.get().getUsername(),
                    Map.of("message", "Your access has been revoked"));
        }
        pendingInviteRepository.deleteByOwnerIdAndInvitedEmail(owner.getId(), email);
        return Map.of("message", "Access revoked for " + email);
    }

    @GetMapping("/check")
    public Map<String, Object> checkAccess(@RequestParam String ownerUsername) {
        User requester = getCurrentUser();
        if (requester == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        if (requester.getId().equals(owner.getId())) {
            return Map.of("hasAccess", true, "reason", "owner");
        }
        boolean granted = accessGrantRepository.existsByOwnerIdAndGrantedUserId(owner.getId(), requester.getId());
        return Map.of("hasAccess", granted, "reason", granted ? "granted" : "not_granted");
    }

    @PostMapping("/request")
    public Map<String, Object> requestAccess(@RequestBody Map<String, String> body) {
        User requester = getCurrentUser();
        if (requester == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String ownerUsername = body.getOrDefault("ownerUsername", "").trim();
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        if (!owner.canCollaborate()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner does not have collaboration enabled");
        }

        String requestId = java.util.UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingApprovals.put(requestId, future);

        messagingTemplate.convertAndSend("/topic/access-request/" + owner.getUsername(),
                Map.of("requestId", requestId, "fromUser", requester.getUsername()));

        try {
            Boolean approved = future.get(30, TimeUnit.SECONDS);
            pendingApprovals.remove(requestId);
            if (Boolean.TRUE.equals(approved)) {
                if (!accessGrantRepository.existsByOwnerIdAndGrantedUserId(owner.getId(), requester.getId())) {
                    accessGrantRepository.save(new AccessGrant(owner, requester, false));
                }
                return Map.of("approved", true, "message", "Access granted");
            }
            return Map.of("approved", false, "message", "Access denied by owner");
        } catch (Exception e) {
            pendingApprovals.remove(requestId);
            return Map.of("approved", false, "message", "Request timed out");
        }
    }

    @PostMapping("/respond")
    public Map<String, Object> respondToRequest(@RequestBody Map<String, Object> body) {
        String requestId = (String) body.getOrDefault("requestId", "");
        boolean approved = (boolean) body.getOrDefault("approved", false);

        CompletableFuture<Boolean> future = pendingApprovals.get(requestId);
        if (future != null) {
            future.complete(approved);
            return Map.of("message", approved ? "Approved" : "Denied");
        }
        return Map.of("message", "Request not found or expired");
    }
}
