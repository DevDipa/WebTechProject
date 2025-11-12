package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Use Long as the ID type because User.id is a Long
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}