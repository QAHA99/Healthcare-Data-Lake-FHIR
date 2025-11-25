package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.AppointmentFhirRepository;
import com.github.cm2027.lab3.dao.fhir.ObservationFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PractitionerFhirRepository;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.time.LocalDateTime;
import java.util.List;

public class TestObservationFhir {

    public static void main(String[] args) {

        try {
            PatientFhirRepository patientRepo = new PatientFhirRepository();
            PractitionerFhirRepository practitionerRepo = new PractitionerFhirRepository();
            AppointmentFhirRepository appointmentRepo = new AppointmentFhirRepository();
            ObservationFhirRepository observationRepo = new ObservationFhirRepository();

            System.out.println("=== Testing Observation FHIR Repository ===\n");

            String testPN = "19850505-" + (System.currentTimeMillis() % 10000);
            String testDoctorID = "DR" + (System.currentTimeMillis() % 10000);
            String testAppointmentID = "AP" + (System.currentTimeMillis() % 10000);
            String testObservationID = "OB" + (System.currentTimeMillis() % 10000);

            System.out.println("0. Setup: Creating patient, practitioner, and appointment...");
            Patient patient = patientRepo.createPatient(testPN, "Karin", "Nilsson", "KVINNA", "070-333333", null);
            Practitioner practitioner = practitionerRepo.createPractitioner(testDoctorID, "Anders", "Lindberg", "08-444444");

            LocalDateTime start = LocalDateTime.now().plusDays(1);
            LocalDateTime end = start.plusHours(1);
            Appointment appointment = appointmentRepo.createAppointment(
                    testAppointmentID,
                    testPN,
                    testDoctorID,
                    start,
                    end,
                    "Medical examination"
            );
            System.out.println("Setup complete");

            System.out.println("\n1. Creating observation...");
            LocalDateTime observedAt = LocalDateTime.now();
            Observation newObservation = observationRepo.createObservation(
                    testObservationID,
                    testPN,
                    observedAt,
                    "Patient reports mild headache and fatigue"
            );
            System.out.println("Created observation ID: " + newObservation.getIdElement().getIdPart());

            System.out.println("\n2. Finding observation by ID...");
            Observation foundObservation = observationRepo.findByID(testObservationID);
            System.out.println("Found observation: " + observationRepo.getObservationID(foundObservation));

            System.out.println("\n3. Getting observation details...");
            String details = observationRepo.getObservationDetails(testObservationID);
            System.out.println(details);

            System.out.println("\n4. Listing observations by patient...");
            List<Observation> patientObservations = observationRepo.listByPatient(testPN);
            System.out.println("Found " + patientObservations.size() + " observation(s) for patient");

            System.out.println("\n5. Updating observation...");
            LocalDateTime newObservedAt = observedAt.plusHours(1);
            observationRepo.updateObservation(testObservationID, newObservedAt, "Updated: Patient symptoms improved after rest");
            System.out.println("Updated observation text and time");

            System.out.println("\n6. Deleting observation...");
            String result = observationRepo.deleteObservation(testObservationID, true);
            System.out.println(result);

            System.out.println("\n7. Cleanup: Deleting appointment, patient, and practitioner...");
            appointmentRepo.deleteAppointment(testAppointmentID, true);
            patientRepo.deletePatient(testPN, true);
            practitionerRepo.deletePractitioner(testDoctorID, true);
            System.out.println("Cleanup complete");

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}