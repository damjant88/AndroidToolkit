package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String remoteApkLocation;

    @Column(nullable = false)
    private String localApkFolder;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Project() {}

    public Project(String name, String remoteApkLocation, String localApkFolder) {
        this.name = name;
        this.remoteApkLocation = remoteApkLocation;
        this.localApkFolder = localApkFolder;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRemoteApkLocation() { return remoteApkLocation; }
    public void setRemoteApkLocation(String remoteApkLocation) { this.remoteApkLocation = remoteApkLocation; }

    public String getLocalApkFolder() { return localApkFolder; }
    public void setLocalApkFolder(String localApkFolder) { this.localApkFolder = localApkFolder; }

    public Instant getCreatedAt() { return createdAt; }
}
