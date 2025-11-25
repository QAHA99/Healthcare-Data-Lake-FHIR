package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import org.hl7.fhir.r4.model.Patient;

public class TestPatientFhir {

    public static void main(String[] args) {

        try {
            PatientFhirRepository patientRepo = new PatientFhirRepository();

            System.out.println("=== Testing Patient FHIR Repository ===\n");

            String testPN = "19850615-" + (System.currentTimeMillis() % 10000);

            System.out.println("1. Creating patient with PN: " + testPN);
            Patient newPatient = patientRepo.createPatient(
                    testPN,
                    "Anna",
                    "Andersson",
                    "KVINNA",
                    "070-1234567",
                    null
            );
            System.out.println("Created patient ID: " + newPatient.getIdElement().getIdPart());

            System.out.println("\n2. Finding patient by PN...");
            Patient foundPatient = patientRepo.findByPN(testPN);
            System.out.println("Found patient: " + foundPatient.getNameFirstRep().getNameAsSingleString());

            System.out.println("\n3. Getting patient summary...");
            String summary = patientRepo.getPatientSummary(testPN);
            System.out.println(summary);

            System.out.println("\n4. Updating patient...");
            patientRepo.updatePatient(testPN, "Anna", "Svensson", "KVINNA", "070-9999999", null);
            System.out.println("Updated patient name to: Svensson");

            System.out.println("\n5. Searching by name...");
            var patients = patientRepo.searchByName("Anna", "Svensson");
            System.out.println("Found " + patients.size() + " patient(s)");

            System.out.println("\n6. Testing delete warning...");
            String warning = patientRepo.deletePatient(testPN, false);
            System.out.println(warning);

            System.out.println("\n7. Deleting patient...");
            String result = patientRepo.deletePatient(testPN, true);
            System.out.println(result);

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
