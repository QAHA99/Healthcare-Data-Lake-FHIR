package com.github.cm2027.lab3.cli;

import com.github.cm2027.lab3.model.mongo.User;
import com.github.cm2027.lab3.service.AuthService;

public class MainMenuCLI extends CLIMenu {

    private final AuthService authService;
    private PatientMenuCLI patientMenu;
    private DoctorMenuCLI doctorMenu;

    public MainMenuCLI(AuthService authService) {
        this.authService = authService;
    }

    public void show() {
        User currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            printError("No user logged in.");
            return;
        }

        if (currentUser.getRole() == User.Role.PATIENT) {
            showPatientMenu();
        } else if (currentUser.getRole() == User.Role.DOCTOR) {
            showDoctorMenu();
        } else {
            printError("Unknown user role.");
        }
    }

    private void showPatientMenu() {
        if (patientMenu == null) {
            patientMenu = new PatientMenuCLI(authService);
        }

        while (authService.isLoggedIn()) {
            clearScreen();
            User user = authService.getCurrentUser();
            printHeader("PATIENT MENU - " + user.getFirstName() + " " + user.getLastName());

            printMenuOptions(
                    "View My Summary",
                    "View My Appointments",
                    "Messages",
                    "Logout"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                patientMenu.viewMySummary();
            } else if (choice == 2) {
                patientMenu.viewMyAppointments();
            } else if (choice == 3) {
                patientMenu.showMessagingMenu();
            } else if (choice == 4) {
                if (confirmLogout()) {
                    authService.logout();
                    printSuccess("Logged out successfully.");
                    pauseForUser();
                    return;
                }
            } else if (choice == 0) {
                if (confirmLogout()) {
                    authService.logout();
                    return;
                }
            } else {
                printError("Invalid option. Please try again.");
                pauseForUser();
            }
        }
    }

    private void showDoctorMenu() {
        if (doctorMenu == null) {
            doctorMenu = new DoctorMenuCLI(authService);
        }

        while (authService.isLoggedIn()) {
            clearScreen();
            User user = authService.getCurrentUser();
            printHeader("DOCTOR MENU - Dr. " + user.getFirstName() + " " + user.getLastName());

            printMenuOptions(
                    "View My Patients",
                    "View My Appointments",
                    "Manage Appointments",
                    "Manage Observations",
                    "Manage Conditions",
                    "Messages",
                    "Logout"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                doctorMenu.viewMyPatients();
            } else if (choice == 2) {
                doctorMenu.viewMyAppointments();
            } else if (choice == 3) {
                doctorMenu.manageAppointments();
            } else if (choice == 4) {
                doctorMenu.manageObservations();
            } else if (choice == 5) {
                doctorMenu.manageConditions();
            } else if (choice == 6) {
                doctorMenu.showMessagingMenu();
            } else if (choice == 7) {
                if (confirmLogout()) {
                    authService.logout();
                    printSuccess("Logged out successfully.");
                    pauseForUser();
                    return;
                }
            } else if (choice == 0) {
                if (confirmLogout()) {
                    authService.logout();
                    return;
                }
            } else {
                printError("Invalid option. Please try again.");
                pauseForUser();
            }
        }
    }

    private boolean confirmLogout() {
        return getConfirmation("Are you sure you want to logout?");
    }
}