package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.CommunicationFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PractitionerFhirRepository;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;

public class TestCommunicationFhir {

    public static void main(String[] args) {

        PatientFhirRepository patientRepo = new PatientFhirRepository();
        PractitionerFhirRepository practitionerRepo = new PractitionerFhirRepository();
        CommunicationFhirRepository communicationRepo = new CommunicationFhirRepository();

        String testPN = null;
        String testDoctorID = null;

        try {
            System.out.println("=== Testing Communication FHIR Repository ===\n");

            testPN = "19880808-" + (System.currentTimeMillis() % 10000);
            testDoctorID = "DR" + (System.currentTimeMillis() % 10000);
            String testCommID1 = "CM" + (System.currentTimeMillis() % 10000);
            String testCommID2 = "CM" + ((System.currentTimeMillis() + 1) % 10000);

            System.out.println("0. Setup: Creating patient and practitioner...");
            Patient patient = patientRepo.createPatient(testPN, "Emma", "Johansson", "KVINNA", "070-777777", null);
            Practitioner practitioner = practitionerRepo.createPractitioner(testDoctorID, "Peter", "Olsson", "08-888888");
            System.out.println("Created patient PN: " + testPN);
            System.out.println("Created practitioner ID: " + testDoctorID);

            System.out.println("\n1. Patient sends message to doctor...");
            Communication message1 = communicationRepo.sendMessage(
                    testCommID1,
                    testPN,
                    null,
                    null,
                    testDoctorID,
                    "Hello Doctor, I have a question about my medication."
            );
            System.out.println("Created communication ID: " + message1.getIdElement().getIdPart());

            System.out.println("\n2. Doctor replies to patient...");
            Communication message2 = communicationRepo.sendMessage(
                    testCommID2,
                    null,
                    testDoctorID,
                    testPN,
                    null,
                    "Hello Emma, I will review your medication and get back to you."
            );
            System.out.println("Created communication ID: " + message2.getIdElement().getIdPart());

            System.out.println("\n3. Finding communication by ID...");
            Communication foundCommunication = communicationRepo.findByID(testCommID1);
            System.out.println("Found communication: " + communicationRepo.getCommunicationID(foundCommunication));

            System.out.println("\n4. Getting communication details...");
            String details = communicationRepo.getCommunicationDetails(testCommID1);
            System.out.println(details);

            System.out.println("\n5. Getting messages between patient and doctor...");
            List<Communication> conversation = communicationRepo.getMessagesBetween(
                    testPN, null,
                    null, testDoctorID
            );
            System.out.println("Found " + conversation.size() + " message(s) in conversation");

            System.out.println("\n6. Getting all messages for patient...");
            List<Communication> patientMessages = communicationRepo.getMyMessages(testPN, null);
            System.out.println("Patient has " + patientMessages.size() + " message(s)");

            System.out.println("\n7. Getting all messages for doctor...");
            List<Communication> doctorMessages = communicationRepo.getMyMessages(null, testDoctorID);
            System.out.println("Doctor has " + doctorMessages.size() + " message(s)");

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\nNote: Communications, patient, and practitioner remain in FHIR server.");
            System.out.println("They will be cleaned up automatically or can be deleted manually if needed.");
            System.out.println("Patient PN: " + testPN);
            System.out.println("Practitioner ID: " + testDoctorID);
        }
    }
}