package com.databin_pg.api.Config;

// import com.databin_pg.api.Entity.Department;
import com.databin_pg.api.Entity.Role;
import com.databin_pg.api.Entity.User;
// import com.databin_pg.api.Repository.DepartmentRepository;
import com.databin_pg.api.Repository.RoleRepository;
import com.databin_pg.api.Repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      RoleRepository roleRepository,
                    //   DepartmentRepository departmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        // this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if admin role exists
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> "admin".equalsIgnoreCase(r.getRoleLevel()) && r.getDepartmentId() == null)
                .findFirst()
                .orElse(null);

        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setRoleLevel("admin");
            adminRole.setAccessType("full");
            adminRole.setIdentifier("ADMIN_GLOBAL");
            adminRole.setDepartmentId(null); // global admin has no department
            roleRepository.save(adminRole);
        }

        // Check if admin user exists
        Optional<User> existingAdmin = userRepository.findByEmail("meridianTechsol@meridianit.com");

        if (existingAdmin.isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail("meridianTechsol@meridianit.com");
            adminUser.setPassword(passwordEncoder.encode("meridianit"));
            adminUser.setRole(adminRole);
            adminUser.setRoleIdentifier("ADMIN_GLOBAL");
            adminUser.setDepartment(null); // No department for admin
            userRepository.save(adminUser);

            System.out.println("✅ Admin user created.");
        } else {
            System.out.println("ℹ️ Admin user already exists.");
        }
    }
}
