package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.PractitionerFhirRepository;
import org.hl7.fhir.r4.model.Practitioner;

public class TestPractitionerFhir {

    public static void main(String[] args) {

        try {
            PractitionerFhirRepository practitionerRepo = new PractitionerFhirRepository();

            System.out.println("=== Testing Practitioner FHIR Repository ===\n");

            String testDoctorID = "DR" + (System.currentTimeMillis() % 10000);

            System.out.println("1. Creating practitioner with ID: " + testDoctorID);
            Practitioner newPractitioner = practitionerRepo.createPractitioner(
                    testDoctorID,
                    "Erik",
                    "Karlsson",
                    "08-123456"
            );
            System.out.println("Created practitioner ID: " + newPractitioner.getIdElement().getIdPart());

            System.out.println("\n2. Finding practitioner by ID...");
            Practitioner foundPractitioner = practitionerRepo.findByID(testDoctorID);
            System.out.println("Found practitioner: " + foundPractitioner.getNameFirstRep().getNameAsSingleString());

            System.out.println("\n3. Getting practitioner summary...");
            String summary = practitionerRepo.getPractitionerSummary(testDoctorID);
            System.out.println(summary);

            System.out.println("\n4. Updating practitioner...");
            practitionerRepo.updatePractitioner(testDoctorID, "Erik", "Andersson", "08-999999");
            System.out.println("Updated practitioner name to: Andersson");

            System.out.println("\n5. Searching by name...");
            var practitioners = practitionerRepo.searchByName("Erik", "Andersson");
            System.out.println("Found " + practitioners.size() + " practitioner(s)");

            System.out.println("\n6. Testing delete warning...");
            String warning = practitionerRepo.deletePractitioner(testDoctorID, false);
            System.out.println(warning);

            System.out.println("\n7. Deleting practitioner...");
            String result = practitionerRepo.deletePractitioner(testDoctorID, true);
            System.out.println(result);

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}