package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);
    void deleteByUserId(Long userId);
}
