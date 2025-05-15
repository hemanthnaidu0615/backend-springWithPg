package com.databin_pg.api.Controlller.UserManagement.repository;
import com.databin_pg.api.Controlller.UserManagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
}
