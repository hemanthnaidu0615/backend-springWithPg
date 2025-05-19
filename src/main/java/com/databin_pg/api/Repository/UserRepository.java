package com.databin_pg.api.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.databin_pg.api.Entity.User;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
