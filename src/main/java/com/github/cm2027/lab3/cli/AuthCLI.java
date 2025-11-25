package com.github.cm2027.lab3.cli;

import com.github.cm2027.lab3.model.mongo.User;
import com.github.cm2027.lab3.service.AuthService;

public class AuthCLI extends CLIMenu {

    private final AuthService authService;

    public AuthCLI(AuthService authService) {
        this.authService = authService;
    }

    public boolean showLoginScreen() {
        clearScreen();
        printHeader("CLINIC MANAGEMENT SYSTEM - LOGIN");

        System.out.println("\nWelcome! Please log in to continue.");
        printDivider();

        while (true) {
            String username = getStringInput("\nUsername (or 'exit' to quit): ");

            if (username.equalsIgnoreCase("exit")) {
                return false;
            }

            if (username.isEmpty()) {
                printError("Username cannot be empty.");
                continue;
            }

            String password = getStringInput("Password: ");

            if (password.isEmpty()) {
                printError("Password cannot be empty.");
                continue;
            }

            if (authService.login(username, password)) {
                User currentUser = authService.getCurrentUser();
                clearScreen();
                printSuccess("Login successful!");
                printInfo("Welcome, " + getUserDisplayName(currentUser) + "!");
                pauseForUser();
                return true;
            } else {
                printError("Invalid username or password. Please try again.");
            }
        }
    }

    private String getUserDisplayName(User user) {
        if (user.getRole() == User.Role.RECEPTIONIST) {
            return user.getName();
        } else {
            return user.getFirstName() + " " + user.getLastName();
        }
    }

    public boolean confirmLogout() {
        return getConfirmation("\nAre you sure you want to logout?");
    }
}