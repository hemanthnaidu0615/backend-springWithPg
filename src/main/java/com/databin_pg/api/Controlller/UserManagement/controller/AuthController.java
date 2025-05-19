package com.databin_pg.api.Controlller.UserManagement.controller;

import com.databin_pg.api.Controlller.UserManagement.entity.Department;
import com.databin_pg.api.Controlller.UserManagement.entity.User;
import com.databin_pg.api.Controlller.UserManagement.service.UserService;
import com.databin_pg.api.Controlller.UserManagement.config.JwtTokenUtil;
import com.databin_pg.api.Controlller.UserManagement.entity.Role;
import com.databin_pg.api.Controlller.UserManagement.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.databin_pg.api.Controlller.UserManagement.repository.UserRepository;
import com.databin_pg.api.Controlller.UserManagement.repository.DepartmentRepository;
import com.databin_pg.api.Controlller.UserManagement.dto.UserInfoResponse;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
     @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;


    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return ResponseEntity.ok(userService.registerUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginRequest,HttpServletResponse response){
        logger.info("Login attempt for user: {}", loginRequest.getEmail());
        try {
           authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), loginRequest.getPassword()
                )
            );

           User dbUser = userService.findByEmail(loginRequest.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

            Long roleId = dbUser.getRole() != null ? dbUser.getRole().getId() : null;
            String token = jwtTokenUtil.generateToken(userDetails, roleId);

            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // true if using HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);


            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", token,
                "role", dbUser.getRole().getRoleLevel(),  // "admin", "manager", "user"
                "email", dbUser.getEmail(),
                "id", dbUser.getId().toString()
    ));
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        Role role = user.getRole();
        String roleLevel = (role != null && role.getRoleLevel() != null) ? role.getRoleLevel() : "user";

        String departmentName = "All";
        if (role != null && role.getDepartmentId() != null) {
            Optional<Department> departmentOptional = departmentRepository.findById(role.getDepartmentId());
            if (departmentOptional.isPresent()) {
                departmentName = departmentOptional.get().getName();
            }
        }

        UserInfoResponse userInfo = new UserInfoResponse(user.getEmail(), roleLevel, departmentName);
        return ResponseEntity.ok(userInfo);
    }
    @GetMapping("/users")
public ResponseEntity<?> getAllUsers(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    // Optional: Only allow ADMIN and MANAGER
    String email = authentication.getName();
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    User currentUser = userOptional.get();
    String roleLevel = currentUser.getRole() != null ? currentUser.getRole().getRoleLevel() : "user";
    if (!roleLevel.equalsIgnoreCase("admin") && !roleLevel.equalsIgnoreCase("manager")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

    return ResponseEntity.ok(userRepository.findAll());
}
@PostMapping("/logout")
public ResponseEntity<?> logout(HttpServletResponse response) {
    Cookie cookie = new Cookie("jwt", "");
    cookie.setHttpOnly(true);
    cookie.setSecure(false); // ❗ Set to false for localhost, true in production
    cookie.setPath("/");
    cookie.setMaxAge(0); // Delete immediately

    response.addCookie(cookie);
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
}



    
}
