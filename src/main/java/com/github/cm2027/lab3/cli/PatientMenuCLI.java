package com.github.cm2027.lab3.cli;

import com.github.cm2027.lab3.dao.fhir.AppointmentFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.model.mongo.User;
import com.github.cm2027.lab3.service.AuthService;
import org.hl7.fhir.r4.model.Appointment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class PatientMenuCLI extends CLIMenu {

    private final AuthService authService;
    private final PatientFhirRepository patientRepo;
    private final AppointmentFhirRepository appointmentRepo;
    private final MessagingCLI messagingCLI;

    public PatientMenuCLI(AuthService authService) {
        this.authService = authService;
        this.patientRepo = new PatientFhirRepository();
        this.appointmentRepo = new AppointmentFhirRepository();
        this.messagingCLI = new MessagingCLI(authService);
    }

    public void viewMySummary() {
        try {
            User currentUser = authService.getCurrentUser();
            String patientPN = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY SUMMARY");

            String summary = patientRepo.getPatientSummary(patientPN);
            System.out.println("\n" + summary);

            printDivider();
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load summary: " + e.getMessage());
            pauseForUser();
        }
    }

    public void viewMyAppointments() {
        try {
            User currentUser = authService.getCurrentUser();
            String patientPN = currentUser.getPersonRef();

            clearScreen();
            printHeader("MY APPOINTMENTS");

            List<Appointment> appointments = appointmentRepo.listByPatient(patientPN);

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

    public void showMessagingMenu() {
        messagingCLI.showMessagingMenu();
    }
}