package com.tontinepro.tontinepro_backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndLuFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.user.id = :userId AND n.lu = false")
    void marquerToutesLues(UUID userId);
}
