package com.github.cm2027.lab3.service;

import com.github.cm2027.lab3.dao.mongodb.UserRepository;
import com.github.cm2027.lab3.model.mongo.User;

public class AuthService {

    private final UserRepository userRepository;
    private User currentUser;

    public AuthService() {
        this.userRepository = new UserRepository();
        this.currentUser = null;
    }

    /**
     * Login with username and password
     */
    public boolean login(String username, String password) {
        User user = userRepository.authenticate(username, password);
        if (user != null) {
            this.currentUser = user;
            return true;
        }
        return false;
    }

    /**
     * Logout current user
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Get currently logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if current user is a doctor
     */
    public boolean isDoctor() {
        return currentUser != null && currentUser.getRole() == User.Role.DOCTOR;
    }

    /**
     * Check if current user is a patient
     */
    public boolean isPatient() {
        return currentUser != null && currentUser.getRole() == User.Role.PATIENT;
    }

    /**
     * Check if current user is receptionist
     */
    public boolean isReceptionist() {
        return currentUser != null && currentUser.getRole() == User.Role.RECEPTIONIST;
    }
}