package com.databin_pg.api.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.databin_pg.api.Entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {}

