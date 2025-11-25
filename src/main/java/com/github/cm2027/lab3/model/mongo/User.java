package com.github.cm2027.lab3.model.mongo;

import org.bson.types.ObjectId;
import java.util.Date;

public class User {
    private ObjectId id;
    private String username;
    private String password;
    private Role role;
    private String personRef;  // Neo4j doctorID/patientPN OR RC0001
    private String firstName;
    private String lastName;
    private String name;  // For receptionist ("ClinicApp")
    private Date createdAt;

    public enum Role {
        DOCTOR,
        PATIENT,
        RECEPTIONIST
    }

    // Constructors
    public User() {this.createdAt = new Date();}

    public User(String username, String password, Role role, String personRef) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.personRef = personRef;
        this.createdAt = new Date();
    }

    // Getters
    public ObjectId getId() {return id;}
    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public Role getRole() {return role;}
    public String getPersonRef() {return personRef;}
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public String getName() {return name;}
    public Date getCreatedAt() {return createdAt;}

    // Setters
    public void setId(ObjectId id) {this.id = id;}
    public void setUsername(String username) {this.username = username;}
    public void setPassword(String password) {this.password = password;}
    public void setRole(Role role) {this.role = role;}
    public void setPersonRef(String personRef) {this.personRef = personRef;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setName(String name) {this.name = name;}
    public void setCreatedAt(Date createdAt) {this.createdAt = createdAt;}

    // To String
    @Override
    public String toString() {
        if (role == Role.RECEPTIONIST) {
            return String.format("User{username='%s', role=%s, name='%s'}",
                    username, role, name);
        }
        return String.format("User{username='%s', role=%s, name='%s %s', personRef='%s'}",
                username, role, firstName, lastName, personRef);
    }
}