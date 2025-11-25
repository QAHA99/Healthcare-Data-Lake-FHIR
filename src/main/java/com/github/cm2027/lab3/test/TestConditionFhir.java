package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.dao.fhir.ConditionFhirRepository;
import com.github.cm2027.lab3.dao.fhir.PatientFhirRepository;
import com.github.cm2027.lab3.model.enums.Severity;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public class TestConditionFhir {

    public static void main(String[] args) {

        try {
            PatientFhirRepository patientRepo = new PatientFhirRepository();
            ConditionFhirRepository conditionRepo = new ConditionFhirRepository();

            System.out.println("=== Testing Condition FHIR Repository ===\n");

            String testPN = "19750303-" + (System.currentTimeMillis() % 10000);
            String testConditionID1 = "CD" + (System.currentTimeMillis() % 10000);
            String testConditionID2 = "CD" + ((System.currentTimeMillis() + 1) % 10000);

            System.out.println("0. Setup: Creating patient...");
            Patient patient = patientRepo.createPatient(testPN, "Sofia", "Andersson", "KVINNA", "070-555555", null);
            System.out.println("Created patient PN: " + testPN);

            System.out.println("\n1. Creating condition with HIGH severity...");
            Condition newCondition1 = conditionRepo.createCondition(
                    testConditionID1,
                    testPN,
                    Severity.HÖG,
                    "Acute bronchitis requiring immediate attention"
            );
            System.out.println("Created condition ID: " + newCondition1.getIdElement().getIdPart());

            System.out.println("\n2. Creating condition with LOW severity...");
            Condition newCondition2 = conditionRepo.createCondition(
                    testConditionID2,
                    testPN,
                    Severity.LÅG,
                    "Mild seasonal allergies"
            );
            System.out.println("Created condition ID: " + newCondition2.getIdElement().getIdPart());

            System.out.println("\n3. Finding condition by ID...");
            Condition foundCondition = conditionRepo.findByID(testConditionID1);
            System.out.println("Found condition: " + conditionRepo.getConditionID(foundCondition));

            System.out.println("\n4. Getting condition details...");
            String details = conditionRepo.getConditionDetails(testConditionID1);
            System.out.println(details);

            System.out.println("\n5. Listing all conditions by patient...");
            List<Condition> patientConditions = conditionRepo.listByPatient(testPN);
            System.out.println("Found " + patientConditions.size() + " condition(s) for patient");

            System.out.println("\n6. Listing conditions by patient and severity (HIGH)...");
            List<Condition> highSeverityConditions = conditionRepo.listByPatientAndSeverity(testPN, Severity.HÖG);
            System.out.println("Found " + highSeverityConditions.size() + " HIGH severity condition(s)");

            System.out.println("\n7. Updating condition...");
            conditionRepo.updateCondition(testConditionID1, Severity.MEDEL, "Updated: Bronchitis improving with treatment");
            System.out.println("Updated condition severity and details");

            System.out.println("\n8. Deleting conditions...");
            String result1 = conditionRepo.deleteCondition(testConditionID1, true);
            System.out.println(result1);
            String result2 = conditionRepo.deleteCondition(testConditionID2, true);
            System.out.println(result2);

            System.out.println("\n9. Cleanup: Deleting patient...");
            patientRepo.deletePatient(testPN, true);
            System.out.println("Cleanup complete");

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}