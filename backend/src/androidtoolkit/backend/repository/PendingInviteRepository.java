package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.PendingInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PendingInviteRepository extends JpaRepository<PendingInvite, Long> {
    List<PendingInvite> findByInvitedEmail(String email);
    List<PendingInvite> findByOwnerId(Long ownerId);
    void deleteByOwnerIdAndInvitedEmail(Long ownerId, String email);
    boolean existsByOwnerIdAndInvitedEmail(Long ownerId, String email);
}
