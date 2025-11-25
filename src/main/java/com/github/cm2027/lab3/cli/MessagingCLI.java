package com.github.cm2027.lab3.cli;

import com.github.cm2027.lab3.dao.fhir.CommunicationFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PractitionerFhirRepository;
import com.github.cm2027.lab3.model.mongo.User;
import com.github.cm2027.lab3.service.AuthService;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Patient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class MessagingCLI extends CLIMenu {

    private final AuthService authService;
    private final CommunicationFhirRepository communicationRepo;
    private final PatientFhirRepository patientRepo;
    private final PractitionerFhirRepository practitionerRepo;

    public MessagingCLI(AuthService authService) {
        this.authService = authService;
        this.communicationRepo = new CommunicationFhirRepository();
        this.patientRepo = new PatientFhirRepository();
        this.practitionerRepo = new PractitionerFhirRepository();
    }

    public void showMessagingMenu() {
        while (true) {
            clearScreen();
            printHeader("MESSAGING");

            printMenuOptions(
                    "View My Messages",
                    "Send Message"
            );

            int choice = getIntInput("");

            if (choice == 1) {
                viewMyMessages();
            } else if (choice == 2) {
                sendMessage();
            } else if (choice == 0) {
                return;
            } else {
                printError("Invalid option.");
                pauseForUser();
            }
        }
    }

    private void viewMyMessages() {
        try {
            User currentUser = authService.getCurrentUser();

            clearScreen();
            printHeader("MY MESSAGES");

            List<Communication> messages = null;

            if (currentUser.getRole() == User.Role.PATIENT) {
                String patientPN = currentUser.getPersonRef();
                messages = communicationRepo.getMyMessages(patientPN, null);
            } else if (currentUser.getRole() == User.Role.DOCTOR) {
                String doctorID = currentUser.getPersonRef();
                messages = communicationRepo.getMyMessages(null, doctorID);
            }

            if (messages == null || messages.isEmpty()) {
                printInfo("No messages found.");
                pauseForUser();
                return;
            }

            printDivider();

            for (Communication comm : messages) {
                LocalDateTime sent = null;
                if (comm.hasSent()) {
                    sent = LocalDateTime.ofInstant(
                            comm.getSent().toInstant(),
                            ZoneId.systemDefault()
                    );
                }

                String messageText = "";
                if (!comm.getPayload().isEmpty()) {
                    var payload = comm.getPayload().get(0);
                    if (payload.getContent() instanceof org.hl7.fhir.r4.model.StringType) {
                        org.hl7.fhir.r4.model.StringType content =
                                (org.hl7.fhir.r4.model.StringType) payload.getContent();
                        messageText = content.getValue();
                    }
                }

                System.out.println("\n[" + formatDateTime(sent) + "]");
                System.out.println(messageText);
                printDivider();
            }

            pauseForUser();

        } catch (Exception e) {
            printError("Failed to load messages: " + e.getMessage());
            pauseForUser();
        }
    }

    private void sendMessage() {
        try {
            User currentUser = authService.getCurrentUser();

            clearScreen();
            printHeader("SEND MESSAGE");

            if (currentUser.getRole() == User.Role.PATIENT) {
                sendMessageAsPatient();
            } else if (currentUser.getRole() == User.Role.DOCTOR) {
                sendMessageAsDoctor();
            }

        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }

    private void sendMessageAsPatient() {
        try {
            User currentUser = authService.getCurrentUser();
            String patientPN = currentUser.getPersonRef();

            Patient patient = patientRepo.findByPN(patientPN);
            String doctorId = patientRepo.getPrimaryDoctorIdentifier(patient);

            if (doctorId == null) {
                printError("You don't have an assigned doctor.");
                pauseForUser();
                return;
            }

            printInfo("Sending message to your doctor (ID: " + doctorId + ")");

            String messageText = getStringInput("\nEnter your message: ");
            if (messageText.isEmpty()) {
                printError("Message cannot be empty.");
                pauseForUser();
                return;
            }

            String commID = "CM" + System.currentTimeMillis();

            communicationRepo.sendMessage(
                    commID,
                    patientPN,
                    null,
                    null,
                    doctorId,
                    messageText
            );

            printSuccess("Message sent successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }

    private void sendMessageAsDoctor() {
        try {
            User currentUser = authService.getCurrentUser();
            String doctorID = currentUser.getPersonRef();

            String patientPN = getStringInput("\nEnter Patient PN to message: ");
            if (patientPN.isEmpty()) {
                printError("Patient PN cannot be empty.");
                pauseForUser();
                return;
            }

            Patient patient = patientRepo.findByPN(patientPN);

            String messageText = getStringInput("\nEnter your message: ");
            if (messageText.isEmpty()) {
                printError("Message cannot be empty.");
                pauseForUser();
                return;
            }

            String commID = "CM" + System.currentTimeMillis();

            communicationRepo.sendMessage(
                    commID,
                    null,
                    doctorID,
                    patientPN,
                    null,
                    messageText
            );

            printSuccess("Message sent successfully!");
            pauseForUser();

        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            pauseForUser();
        }
    }
}