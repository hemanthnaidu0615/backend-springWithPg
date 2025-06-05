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
    public void run(String... args) {
        // Look up only by unique identifier
        Role adminRole = roleRepository.findByIdentifier("ADMIN_GLOBAL")
            .orElseGet(() -> {
                Role newAdminRole = new Role();
                newAdminRole.setRoleLevel("admin");
                newAdminRole.setAccessType("full");
                newAdminRole.setIdentifier("ADMIN_GLOBAL");
                newAdminRole.setDepartmentId(null);
                return roleRepository.save(newAdminRole);
            });

        // Seed admin user
        userRepository.findByEmail("meridianTechsol@meridianit.com").ifPresentOrElse(
            u -> System.out.println("ℹ️ Admin user already exists."),
            () -> {
                User admin = new User();
                admin.setEmail("meridianTechsol@meridianit.com");
                admin.setPassword(passwordEncoder.encode("meridianit"));
                admin.setRole(adminRole);
                admin.setRoleIdentifier(adminRole.getIdentifier());
                admin.setDepartment(null);
                userRepository.save(admin);
                System.out.println("✅ Global admin user created.");
            });
    }
}
