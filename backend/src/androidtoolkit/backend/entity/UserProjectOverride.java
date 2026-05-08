package androidtoolkit.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_project_overrides",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
public class UserProjectOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String localApkFolder;

    public UserProjectOverride() {}

    public UserProjectOverride(User user, Project project, String localApkFolder) {
        this.user = user;
        this.project = project;
        this.localApkFolder = localApkFolder;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getLocalApkFolder() { return localApkFolder; }
    public void setLocalApkFolder(String localApkFolder) { this.localApkFolder = localApkFolder; }
}
