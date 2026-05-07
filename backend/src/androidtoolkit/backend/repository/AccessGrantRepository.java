package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {
    List<AccessGrant> findByOwnerId(Long ownerId);
    Optional<AccessGrant> findByOwnerIdAndGrantedUserId(Long ownerId, Long grantedUserId);
    boolean existsByOwnerIdAndGrantedUserId(Long ownerId, Long grantedUserId);
    void deleteByOwnerIdAndGrantedUserId(Long ownerId, Long grantedUserId);
}
