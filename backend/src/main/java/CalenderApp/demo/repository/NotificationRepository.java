package main.java.CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(AppUser user);

    List<Notification> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<Notification> findByIdAndUser(Long id, AppUser user);
}
