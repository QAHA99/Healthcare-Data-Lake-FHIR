package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.AppointmentFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PractitionerFhirRepository;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.time.LocalDateTime;
import java.util.List;

public class TestAppointmentFhir {

    public static void main(String[] args) {

        try {
            PatientFhirRepository patientRepo = new PatientFhirRepository();
            PractitionerFhirRepository practitionerRepo = new PractitionerFhirRepository();
            AppointmentFhirRepository appointmentRepo = new AppointmentFhirRepository();

            System.out.println("=== Testing Appointment FHIR Repository ===\n");

            String testPN = "19900101-" + (System.currentTimeMillis() % 10000);
            String testDoctorID = "DR" + (System.currentTimeMillis() % 10000);
            String testAppointmentID = "AP" + (System.currentTimeMillis() % 10000);

            System.out.println("0. Setup: Creating patient and practitioner...");
            Patient patient = patientRepo.createPatient(testPN, "Lars", "Berg", "MAN", "070-111111", null);
            Practitioner practitioner = practitionerRepo.createPractitioner(testDoctorID, "Maria", "Svensson", "08-222222");
            System.out.println("Created patient PN: " + testPN);
            System.out.println("Created practitioner ID: " + testDoctorID);

            System.out.println("\n1. Creating appointment...");
            LocalDateTime start = LocalDateTime.now().plusDays(1);
            LocalDateTime end = start.plusHours(1);
            Appointment newAppointment = appointmentRepo.createAppointment(
                    testAppointmentID,
                    testPN,
                    testDoctorID,
                    start,
                    end,
                    "Regular checkup"
            );
            System.out.println("Created appointment ID: " + newAppointment.getIdElement().getIdPart());

            System.out.println("\n2. Finding appointment by ID...");
            Appointment foundAppointment = appointmentRepo.findByID(testAppointmentID);
            System.out.println("Found appointment: " + appointmentRepo.getAppointmentID(foundAppointment));

            System.out.println("\n3. Getting appointment details...");
            String details = appointmentRepo.getAppointmentDetails(testAppointmentID);
            System.out.println(details);

            System.out.println("\n4. Listing appointments by patient...");
            List<Appointment> patientAppointments = appointmentRepo.listByPatient(testPN);
            System.out.println("Found " + patientAppointments.size() + " appointment(s) for patient");

            System.out.println("\n5. Listing appointments by practitioner...");
            List<Appointment> practitionerAppointments = appointmentRepo.listByPractitioner(testDoctorID);
            System.out.println("Found " + practitionerAppointments.size() + " appointment(s) for practitioner");

            System.out.println("\n6. Updating appointment...");
            LocalDateTime newStart = start.plusHours(2);
            LocalDateTime newEnd = newStart.plusHours(1);
            appointmentRepo.updateAppointment(testAppointmentID, newStart, newEnd, "Updated: Annual checkup");
            System.out.println("Updated appointment time and reason");

            System.out.println("\n7. Deleting appointment...");
            String result = appointmentRepo.deleteAppointment(testAppointmentID, true);
            System.out.println(result);

            System.out.println("\n8. Cleanup: Deleting patient and practitioner...");
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