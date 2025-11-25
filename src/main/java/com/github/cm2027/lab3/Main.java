package com.github.cm2027.lab3;

import com.github.cm2027.lab3.cli.AuthCLI;
import com.github.cm2027.lab3.cli.MainMenuCLI;
import com.github.cm2027.lab3.config.MongoConfig;
import com.github.cm2027.lab3.service.AuthService;

public class Main {

    public static void main(String[] args) {

        try {
            System.out.println("============================================");
            System.out.println("  CLINIC MANAGEMENT SYSTEM - LAB 3 (FHIR)");
            System.out.println("============================================");
            System.out.println("\nConnecting to databases...");

            // Initialize MongoDB for authentication
            MongoConfig.getDatabase();
            System.out.println("✓ Connected to MongoDB");

            // Test FHIR connection
            ClientSingleton.getInstance();
            System.out.println("✓ Connected to FHIR Server");

            System.out.println("\nSystem ready!");

            // Initialize services
            AuthService authService = new AuthService();
            AuthCLI authCLI = new AuthCLI(authService);
            MainMenuCLI mainMenu = new MainMenuCLI(authService);

            // Main application loop
            while (true) {
                // Show login screen
                if (!authCLI.showLoginScreen()) {
                    // User chose to exit
                    break;
                }

                // Show appropriate menu based on role
                mainMenu.show();

                // After logout, loop continues to login screen
            }

            System.out.println("\n============================================");
            System.out.println("  Thank you for using Clinic Management System!");
            System.out.println("============================================\n");

        } catch (Exception e) {
            System.err.println("\n✗ Fatal error: " + e.getMessage());
            System.err.println("\nPlease check your database connections and try again.");
            e.printStackTrace();
        } finally {
            // Cleanup
            System.out.println("\nClosing database connections...");
            MongoConfig.close();
            System.out.println("✓ MongoDB connection closed");
            System.out.println("\nGoodbye!");
        }
    }
}