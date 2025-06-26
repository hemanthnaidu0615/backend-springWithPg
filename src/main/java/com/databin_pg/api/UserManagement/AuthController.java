package com.databin_pg.api.UserManagement;

import com.databin_pg.api.Config.JwtTokenUtil;
import com.databin_pg.api.DTO.UserInfoResponse;
import com.databin_pg.api.Entity.Department;
import com.databin_pg.api.Entity.Role;
import com.databin_pg.api.Entity.User;
import com.databin_pg.api.Repository.DepartmentRepository;
import com.databin_pg.api.Repository.UserRepository;
import com.databin_pg.api.Service.CustomUserDetailsService;
import com.databin_pg.api.Service.UserService;
import com.databin_pg.api.Service.PostgresService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "APIs for user authentication and user management")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PostgresService postgresService;

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
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()));

            User dbUser = userService.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

            String token = jwtTokenUtil.generateToken(userDetails,
                    dbUser.getRole() != null ? dbUser.getRole().getId() : null);

            addJwtCookie(response, token, request);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "token", token,
                    "role", dbUser.getRole() != null ? dbUser.getRole().getRoleLevel() : "user",
                    "email", dbUser.getEmail(),
                    "id", dbUser.getId().toString()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        deleteJwtCookie(response, request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
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

    @Operation(summary = "Get All Users", description = "Returns paginated, filterable, and sortable list of users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, String> allParams) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOptional = userRepository.findByEmail(authentication.getName());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User currentUser = userOptional.get();
        String roleLevel = currentUser.getRole() != null ? currentUser.getRole().getRoleLevel() : "user";

        if (!roleLevel.equalsIgnoreCase("admin") && !roleLevel.equalsIgnoreCase("manager")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        if (page < 0)
            page = 0;
        if (size <= 0)
            size = 50;
        int offset = page * size;

        try {
            // Map logical field names to actual SQL columns
            Map<String, String> fieldToColumn = Map.of(
                    "id", "u.id",
                    "email", "u.email",
                    "role", "u.role",
                    "role_level", "r.role_level",
                    "department", "d.name");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : fieldToColumn.keySet()) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String column = fieldToColumn.get(field);
                    String condition = switch (matchMode) {
                        case "startsWith" -> "LOWER(CAST(%s AS TEXT)) LIKE '%s%%'".formatted(column, value);
                        case "endsWith" -> "LOWER(CAST(%s AS TEXT)) LIKE '%%%s'".formatted(column, value);
                        case "notContains" -> "LOWER(CAST(%s AS TEXT)) NOT LIKE '%%%s%%'".formatted(column, value);
                        case "equals" -> "LOWER(CAST(%s AS TEXT)) = '%s'".formatted(column, value);
                        default -> "LOWER(CAST(%s AS TEXT)) LIKE '%%%s%%'".formatted(column, value);
                    };
                    whereClause.append(" AND ").append(condition);
                }
            }

            String sortField = allParams.getOrDefault("sortField", "id");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc").equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // Use actual column for sorting if allowed, else default to u.id
            String sortColumn = fieldToColumn.getOrDefault(sortField, "u.id");

            String dataQuery = String.format("""
                        SELECT u.id, u.email, u.role, r.role_level, d.name AS department
                        FROM users u
                        LEFT JOIN roles r ON u.role_id = r.id
                        LEFT JOIN departments d ON u.department = d.id
                        %s
                        ORDER BY %s %s
                        LIMIT %d OFFSET %d
                    """, whereClause.toString(), sortColumn, sortOrder, size, offset);

            String countQuery = String.format("""
                        SELECT COUNT(*) AS count
                        FROM users u
                        LEFT JOIN roles r ON u.role_id = r.id
                        LEFT JOIN departments d ON u.department = d.id
                        %s
                    """, whereClause.toString());

            List<Map<String, Object>> data = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = ((Number) countResult.get(0).get("count")).intValue();

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "page", page,
                    "size", size,
                    "count", totalCount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching users", "details", e.getMessage()));
        }
    }

    private void addJwtCookie(HttpServletResponse response, String token, HttpServletRequest request) {
        boolean isSecure = request.getServerName() != null && !request.getServerName().contains("localhost");

        response.setHeader("Set-Cookie", String.format(
                "jwt=%s; HttpOnly; Secure=%s; SameSite=None; Path=/; Max-Age=3600",
                token, isSecure));
    }

    private void deleteJwtCookie(HttpServletResponse response, HttpServletRequest request) {
        boolean isSecure = request.getServerName() != null && !request.getServerName().contains("localhost");

        response.setHeader("Set-Cookie", String.format(
                "jwt=; HttpOnly; Secure=%s; SameSite=None; Path=/; Max-Age=0",
                isSecure));
    }
}