package com.databin_pg.api.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.databin_pg.api.Entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByIdentifier(String identifier);
}

