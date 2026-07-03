package androidtoolkit.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private String remoteApkLocation;

    @Column
    private String localApkFolder;

    @Column(length = 1024)
    private String localLogFolder;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private String sharedLogStoragePath;

    @Column(length = 1024)
    private String figmaLink;

    @Column(length = 1024)
    private String figmaLinkIos;

    public Project() {}

    public Project(String name, String remoteApkLocation, String localApkFolder, String localLogFolder) {
        this.name = name;
        this.remoteApkLocation = remoteApkLocation;
        this.localApkFolder = localApkFolder;
        this.localLogFolder = localLogFolder;
    }

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRemoteApkLocation() { return remoteApkLocation; }
    public void setRemoteApkLocation(String remoteApkLocation) { this.remoteApkLocation = remoteApkLocation; }

    public String getLocalApkFolder() { return localApkFolder; }
    public void setLocalApkFolder(String localApkFolder) { this.localApkFolder = localApkFolder; }

    public String getLocalLogFolder() { return localLogFolder; }
    public void setLocalLogFolder(String localLogFolder) { this.localLogFolder = localLogFolder; }

    public Instant getCreatedAt() { return createdAt; }

    public String getSharedLogStoragePath() { return sharedLogStoragePath; }
    public void setSharedLogStoragePath(String sharedLogStoragePath) { this.sharedLogStoragePath = sharedLogStoragePath; }

    public String getFigmaLink() { return figmaLink; }
    public void setFigmaLink(String figmaLink) { this.figmaLink = figmaLink; }

    public String getFigmaLinkIos() { return figmaLinkIos; }
    public void setFigmaLinkIos(String figmaLinkIos) { this.figmaLinkIos = figmaLinkIos; }
}
