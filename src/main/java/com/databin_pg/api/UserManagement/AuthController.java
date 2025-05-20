package com.databin_pg.api.UserManagement;

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

import com.databin_pg.api.Config.JwtTokenUtil;
import com.databin_pg.api.DTO.UserInfoResponse;
import com.databin_pg.api.Entity.Department;
import com.databin_pg.api.Entity.Role;
import com.databin_pg.api.Entity.User;
import com.databin_pg.api.Repository.DepartmentRepository;
import com.databin_pg.api.Repository.UserRepository;
import com.databin_pg.api.Service.CustomUserDetailsService;
import com.databin_pg.api.Service.UserService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
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
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginRequest, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), loginRequest.getPassword()
                )
            );

            User dbUser = userService.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

            String token = jwtTokenUtil.generateToken(userDetails,
                    dbUser.getRole() != null ? dbUser.getRole().getId() : null);

            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // Set to false if not using HTTPS locally
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", token,
                "role", dbUser.getRole() != null ? dbUser.getRole().getRoleLevel() : "user",
                "email", dbUser.getEmail(),
                "id", dbUser.getId().toString()
            ));
        } catch (Exception e) {
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
            departmentName = departmentRepository.findById(role.getDepartmentId())
                    .map(Department::getName)
                    .orElse("All");
        }

        UserInfoResponse userInfo = new UserInfoResponse(user.getEmail(), roleLevel, departmentName);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

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
        cookie.setSecure(false); // Set to false for localhost, true in production
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete immediately

        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
