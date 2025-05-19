package com.databin_pg.api.Controlller.UserManagement.repository;

import com.databin_pg.api.Controlller.UserManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
