package androidtoolkit.backend.entity;

import androidtoolkit.domain.tenant.SubscriptionTier;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents an isolated organizational unit within the platform.
 * Each tenant has its own users, projects, devices, logs, and configuration.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionTier tier = SubscriptionTier.FREE;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false, unique = true)
    private String storagePrefix;

    public Tenant() {}

    public Tenant(String name, SubscriptionTier tier) {
        this.name = name;
        this.tier = tier;
        this.storagePrefix = "tenants/" + java.util.UUID.randomUUID();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStoragePrefix() { return storagePrefix; }
    public void setStoragePrefix(String storagePrefix) { this.storagePrefix = storagePrefix; }
}
