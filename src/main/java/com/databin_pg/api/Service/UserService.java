package com.databin_pg.api.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.databin_pg.api.Entity.Department;
import com.databin_pg.api.Entity.Role;
import com.databin_pg.api.Entity.User;
import com.databin_pg.api.Repository.DepartmentRepository;
import com.databin_pg.api.Repository.RoleRepository;
import com.databin_pg.api.Repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final DepartmentRepository departmentRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo, RoleRepository roleRepo,  DepartmentRepository departmentRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.departmentRepo = departmentRepo;
        this.encoder = encoder;
    }

   public User registerUser(User user) {
    if (user.getRole() != null) {
        Role role = roleRepo.findById(user.getRole().getId())
            .orElseThrow(() -> new RuntimeException("Invalid role ID"));
        user.setRole(role);

         user.setRoleIdentifier(role.getIdentifier());

        // 🔑 Set department based on role's departmentId
        if (role.getDepartmentId() != null) {
            Department department = departmentRepo.findById(role.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found for role"));
            user.setDepartment(department);
        }
    }

    user.setPassword(encoder.encode(user.getPassword()));
    return userRepo.save(user); // ✅ this was missing
}

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }
}

