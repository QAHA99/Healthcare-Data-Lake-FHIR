package com.github.cm2027.lab3.dao.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.cm2027.lab3.ClientSingleton;
import com.github.cm2027.lab3.model.enums.Severity;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ConditionFhirRepository {

    private static final String CONDITION_ID_SYSTEM = "http://kth.se/clinic/condition-id";
    private final IGenericClient client;
    private final PatientFhirRepository patientRepo;

    public ConditionFhirRepository() {
        this.client = ClientSingleton.getInstance();
        this.patientRepo = new PatientFhirRepository();
    }

    public Condition createCondition(String conditionID, String patientPN, Severity severity, String details) throws Exception {

        if (conditionID == null || conditionID.isBlank()) {
            throw new IllegalArgumentException("conditionID must not be blank");
        }
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("details must not be blank");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        String patientFhirId = patient.getIdElement().getIdPart();

        Condition condition = new Condition();

        Identifier identifier = new Identifier();
        identifier.setSystem(CONDITION_ID_SYSTEM);
        identifier.setValue(conditionID);
        condition.addIdentifier(identifier);

        Reference patientRef = new Reference("Patient/" + patientFhirId);
        condition.setSubject(patientRef);

        CodeableConcept severityConcept = new CodeableConcept();
        Coding severityCoding = new Coding();
        severityCoding.setDisplay(severity.getLabel());
        severityConcept.addCoding(severityCoding);
        severityConcept.setText(severity.getLabel());
        condition.setSeverity(severityConcept);

        CodeableConcept code = new CodeableConcept();
        code.setText(details);
        condition.setCode(code);

        condition.setClinicalStatus(new CodeableConcept().setText("active"));

        MethodOutcome outcome = client.create().resource(condition).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create condition");
        }

        Condition createdCondition = (Condition) outcome.getResource();
        return createdCondition;
    }

    public Condition findByID(String conditionID) throws Exception {

        if (conditionID == null || conditionID.isBlank()) {
            throw new IllegalArgumentException("conditionID must not be blank");
        }

        Bundle results = client.search()
                .forResource(Condition.class)
                .where(Condition.IDENTIFIER.exactly().systemAndCode(CONDITION_ID_SYSTEM, conditionID))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No condition found with ID: " + conditionID);
        }

        Condition condition = (Condition) results.getEntry().get(0).getResource();
        return condition;
    }

    public List<Condition> listByPatient(String patientPN) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        String patientFhirId = patient.getIdElement().getIdPart();

        Bundle results = client.search()
                .forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientFhirId))
                .returnBundle(Bundle.class)
                .execute();

        List<Condition> conditions = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Condition condition = (Condition) entry.getResource();
            conditions.add(condition);
        }

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("No conditions found for patient: " + patientPN);
        }

        return conditions;
    }

    public List<Condition> listByPatientAndSeverity(String patientPN, Severity severity) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }

        List<Condition> allConditions = listByPatient(patientPN);
        List<Condition> filteredConditions = new ArrayList<>();

        for (Condition condition : allConditions) {
            if (condition.hasSeverity()) {
                String conditionSeverity = condition.getSeverity().getText();
                if (conditionSeverity != null && conditionSeverity.equals(severity.getLabel())) {
                    filteredConditions.add(condition);
                }
            }
        }

        if (filteredConditions.isEmpty()) {
            throw new IllegalArgumentException("No conditions found for patient with severity: " + severity.getLabel());
        }

        return filteredConditions;
    }

    public String getConditionDetails(String conditionID) throws Exception {

        Condition condition = findByID(conditionID);

        String id = getConditionID(condition);

        String severity = "N/A";
        if (condition.hasSeverity()) {
            severity = condition.getSeverity().getText();
        }

        String details = "N/A";
        if (condition.hasCode()) {
            details = condition.getCode().getText();
        }

        String status = "N/A";
        if (condition.hasClinicalStatus()) {
            status = condition.getClinicalStatus().getText();
        }

        String result = "Condition ID: " + id + "\n";
        result = result + "Severity: " + severity + "\n";
        result = result + "Details: " + details + "\n";
        result = result + "Status: " + status + "\n";

        if (condition.hasSubject()) {
            Reference subjectRef = condition.getSubject();
            String patientId = subjectRef.getReferenceElement().getIdPart();
            Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
            String patientName = patient.getNameFirstRep().getNameAsSingleString();
            result = result + "Patient: " + patientName + "\n";
        }

        return result;
    }

    public Condition updateCondition(String conditionID, Severity newSeverity, String newDetails) throws Exception {

        if (conditionID == null || conditionID.isBlank()) {
            throw new IllegalArgumentException("conditionID must not be blank");
        }

        Condition condition = findByID(conditionID);

        if (newSeverity != null) {
            CodeableConcept severityConcept = new CodeableConcept();
            Coding severityCoding = new Coding();
            severityCoding.setDisplay(newSeverity.getLabel());
            severityConcept.addCoding(severityCoding);
            severityConcept.setText(newSeverity.getLabel());
            condition.setSeverity(severityConcept);
        }

        if (newDetails != null && !newDetails.isBlank()) {
            CodeableConcept code = new CodeableConcept();
            code.setText(newDetails);
            condition.setCode(code);
        }

        client.update().resource(condition).execute();

        return condition;
    }

    public String deleteCondition(String conditionID, boolean confirmed) throws Exception {

        Condition condition = findByID(conditionID);
        String conditionFhirId = condition.getIdElement().getIdPart();
        String id = getConditionID(condition);

        if (!confirmed) {
            String warning = "WARNING: Deleting condition " + id + "\n\n";
            warning = warning + "This will delete the condition from FHIR.\n";
            warning = warning + "Call again with confirmed=true to proceed";
            return warning;
        }

        client.delete().resourceById("Condition", conditionFhirId).execute();

        String result = "Successfully deleted condition " + id;
        return result;
    }

    public String getConditionID(Condition condition) {

        List<Identifier> identifiers = condition.getIdentifier();

        for (Identifier id : identifiers) {
            if (CONDITION_ID_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}