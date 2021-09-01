package com.wanpan.app.repository;

import com.wanpan.app.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {

    NotificationType findByName(String name);
}
