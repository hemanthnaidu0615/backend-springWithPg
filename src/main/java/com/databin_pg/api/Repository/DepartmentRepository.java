package com.databin_pg.api.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.databin_pg.api.Entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
}
