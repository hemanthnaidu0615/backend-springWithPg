package com.databin_pg.api.Controlller.UserManagement.dto;

public class UserInfoResponse {
    private String email;
    private String roleLevel;
    private String department;

    public UserInfoResponse(String email, String roleLevel, String department) {
        this.email = email;
        this.roleLevel = roleLevel;
        this.department = department;
    }

    // Getters and setters (or use Lombok's @Data if you prefer)
    public String getEmail() { return email; }
    public String getRoleLevel() { return roleLevel; }
    public String getDepartment() { return department; }

    public void setEmail(String email) { this.email = email; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }
    public void setDepartment(String department) { this.department = department; }
}
