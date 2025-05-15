package com.databin_pg.api.Controlller.UserManagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roleLevel; // admin, manager, user
    private String accessType;
    private String identifier;

    @Column(name = "department_id")
    private Long departmentId; // nullable for admin

    // private String pagesPermission;

    // Constructors
    public Role() {}

    public Role(String roleLevel, String accessType, String identifier, Long departmentId, String pagesPermission) {
        this.roleLevel = roleLevel;
        this.accessType = accessType;
        this.identifier = identifier;
        this.departmentId = departmentId;
        // this.pagesPermission = pagesPermission;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(String roleLevel) {
        this.roleLevel = roleLevel;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

//     public String getPagesPermission() {
//         return pagesPermission;
//     }
// 
//     public void setPagesPermission(String pagesPermission) {
//         this.pagesPermission = pagesPermission;
//     }

    // Override toString(), equals(), hashCode() if necessary
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", roleLevel='" + roleLevel + '\'' +
                ", accessType='" + accessType + '\'' +
                ", identifier='" + identifier + '\'' +
                ", departmentId=" + departmentId +
                // ", pagesPermission='" + pagesPermission + '\'' +
                '}';
    }
}
