package com.github.cm2027.lab3.cli;

import com.github.cm2027.lab3.dao.fhir.*;
import com.github.cm2027.lab3.model.enums.Severity;
import com.github.cm2027.lab3.model.mongo.User;
import com.github.cm2027.lab3.service.AuthService;
import org.hl7.fhir.r4.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DoctorMenuCLI extends CLIMenu {

    private final AuthService authService;
    private final PatientFhirRepository patientRepo;
    private final PractitionerFhirRepository practitionerRepo;
    private final AppointmentFhirRepository appointmentRepo;
    private final ObservationFhirRepository observationRepo;
    private final ConditionFhirRepository conditionRepo;
    private final MessagingCLI messagingCLI;

    public DoctorMenuCLI(AuthService authService) {
        this.authService = authService;
        this.patientRepo = new PatientFhirRepository();
        this.practitionerRepo = new PractitionerFhirRepository();
        this.appointmentRepo = new AppointmentFhirRepository();
        this.observationRepo = new ObservationFhirRepository();
        this.conditionRepo = new ConditionFhirRepository();
        this.messagingCLI = new MessagingCLI(authService);
    }

    public void viewMyPatients() {
        printInfo("View all patients feature");
        pauseForUser();
    }

    public void viewMyAppointments() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY APPOINTMENTS");

            List<Appointment> appointments = appointmentRepo.listByPractitioner(doctorID);

            if (appointments.isEmpty()) {
                printInfo("No appointments found.");
                pauseForUser();
                return;
            }

            printDivider();
            System.out.println("ID           Date & Time          Reason");
            printDivider();

            for (Appointment apt : appointments) {
                String id = appointmentRepo.getAppointmentID(apt);

                LocalDateTime start = LocalDateTime.ofInstant(
                        apt.getStart().toInstant(),
                        ZoneId.systemDefault()
                );

                String reason = apt.getDescription();
                if (reason == null || reason.isBlank()) {
                    reason = "N/A";
                }
                if (reason.length() > 20) {
                    reason = reason.substring(0, 17) + "...";
                }

                System.out.println(id + "    " + formatDateTime(start) + "    " + reason);
            }

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load appointments: " + e.getMessage());
            pauseForUser();
        }
    }

    public void manageAppointments() {
        while (true) {
            clearScreen();
            printHeader("MANAGE APPOINTMENTS");

            printMenuOptions(
                    "Create Appointment",
                    "View Appointment Details",
                    "Delete Appointment"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                createAppointment();
            } else if (choice == 2) {
                viewAppointmentDetails();
            } else if (choice == 3) {
                deleteAppointment();
            } else if (choice == 0) {
                return;
            } else {
                printError("Invalid option.");
                pauseForUser();
            }
        }
    }

    private void createAppointment() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            clearScreen();
            printHeader("CREATE APPOINTMENT");

            String appointmentID = getStringInput("\nEnter Appointment ID (e.g., AP001): ");
            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            String patientPN = getStringInput("Enter Patient PN: ");
            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nEnter start date and time (format: yyyy-MM-dd HH:mm)");
            String startsStr = getStringInput("Start: ");

            System.out.println("\nEnter end date and time (format: yyyy-MM-dd HH:mm)");
            String endsStr = getStringInput("End: ");

            String reason = getStringInput("\nReason for visit: ");
            if (reason.isEmpty()) {
                printError("Reason cannot be empty.");
                pauseForUser();
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime starts = LocalDateTime.parse(startsStr, formatter);
            LocalDateTime ends = LocalDateTime.parse(endsStr, formatter);

            appointmentRepo.createAppointment(appointmentID, patientPN, doctorID, starts, ends, reason);

            printSuccess("Appointment created successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to create appointment: " + e.getMessage());
            pauseForUser();
        }
    }

    private void viewAppointmentDetails() {
        try {
            String appointmentID = getStringInput("\nEnter Appointment ID: ");
            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = appointmentRepo.getAppointmentDetails(appointmentID);

            clearScreen();
            printHeader("APPOINTMENT DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load appointment details: " + e.getMessage());
            pauseForUser();
        }
    }

    private void deleteAppointment() {
        try {
            String appointmentID = getStringInput("\nEnter Appointment ID to delete: ");
            if (appointmentID.isEmpty()) {
                printError("Appointment ID cannot be empty.");
                pauseForUser();
                return;
            }

            if (!getConfirmation("\nAre you sure you want to delete appointment " + appointmentID + "?")) {
                printInfo("Deletion cancelled.");
                pauseForUser();
                return;
            }

            String result = appointmentRepo.deleteAppointment(appointmentID, true);
            printSuccess(result);
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to delete appointment: " + e.getMessage());
            pauseForUser();
        }
    }

    public void manageObservations() {
        while (true) {
            clearScreen();
            printHeader("MANAGE OBSERVATIONS");

            printMenuOptions(
                    "Create Observation",
                    "View Observation Details"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                createObservation();
            } else if (choice == 2) {
                viewObservationDetails();
            } else if (choice == 0) {
                return;
            } else {
                printError("Invalid option.");
                pauseForUser();
            }
        }
    }

    private void createObservation() {
        try {
            clearScreen();
            printHeader("CREATE OBSERVATION");

            String observationID = getStringInput("\nEnter Observation ID (e.g., OB001): ");
            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            String patientPN = getStringInput("Enter Patient PN: ");
            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nEnter observation date and time (format: yyyy-MM-dd HH:mm)");
            String observedAtStr = getStringInput("Date/Time: ");

            String text = getStringInput("\nObservation text: ");
            if (text.isEmpty()) {
                printError("Observation text cannot be empty.");
                pauseForUser();
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime observedAt = LocalDateTime.parse(observedAtStr, formatter);

            observationRepo.createObservation(observationID, patientPN, observedAt, text);

            printSuccess("Observation created successfully!");
            pauseForUser();

        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
            pauseForUser();
        } catch (Exception e) {
            printError("Failed to create observation: " + e.getMessage());
            pauseForUser();
        }
    }

    private void viewObservationDetails() {
        try {
            String observationID = getStringInput("\nEnter Observation ID: ");
            if (observationID.isEmpty()) {
                printError("Observation ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = observationRepo.getObservationDetails(observationID);

            clearScreen();
            printHeader("OBSERVATION DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load observation details: " + e.getMessage());
            pauseForUser();
        }
    }

    public void manageConditions() {
        while (true) {
            clearScreen();
            printHeader("MANAGE CONDITIONS");

            printMenuOptions(
                    "Create Condition",
                    "View Condition Details"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                createCondition();
            } else if (choice == 2) {
                viewConditionDetails();
            } else if (choice == 0) {
                return;
            } else {
                printError("Invalid option.");
                pauseForUser();
            }
        }
    }

    private void createCondition() {
        try {
            clearScreen();
            printHeader("CREATE CONDITION");

            String conditionID = getStringInput("\nEnter Condition ID (e.g., CD001): ");
            if (conditionID.isEmpty()) {
                printError("Condition ID cannot be empty.");
                pauseForUser();
                return;
            }

            String patientPN = getStringInput("Enter Patient PN: ");
            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            System.out.println("\nSelect severity:");
            System.out.println("1. High");
            System.out.println("2. Medium");
            System.out.println("3. Low");

            int severityChoice = getIntInput("Choice: ");
            Severity severity = null;

            if (severityChoice == 1) {
                severity = Severity.HÖG;
            } else if (severityChoice == 2) {
                severity = Severity.MEDEL;
            } else if (severityChoice == 3) {
                severity = Severity.LÅG;
            } else {
                printError("Invalid severity choice.");
                pauseForUser();
                return;
            }

            String details = getStringInput("\nCondition details: ");
            if (details.isEmpty()) {
                printError("Details cannot be empty.");
                pauseForUser();
                return;
            }

            conditionRepo.createCondition(conditionID, patientPN, severity, details);

            printSuccess("Condition created successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to create condition: " + e.getMessage());
            pauseForUser();
        }
    }

    private void viewConditionDetails() {
        try {
            String conditionID = getStringInput("\nEnter Condition ID: ");
            if (conditionID.isEmpty()) {
                printError("Condition ID cannot be empty.");
                pauseForUser();
                return;
            }

            String details = conditionRepo.getConditionDetails(conditionID);

            clearScreen();
            printHeader("CONDITION DETAILS");
            System.out.println("\n" + details);
            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load condition details: " + e.getMessage());
            pauseForUser();
        }
    }

    public void showMessagingMenu() {
        messagingCLI.showMessagingMenu();
    }
}