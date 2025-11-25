package com.github.cm2027.lab3.dao.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.cm2027.lab3.ClientSingleton;
import org.hl7.fhir.r4.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationFhirRepository {

    private static final String OBSERVATION_ID_SYSTEM = "http://kth.se/clinic/observation-id";
    private final IGenericClient client;
    private final PatientFhirRepository patientRepo;

    public ObservationFhirRepository() {
        this.client = ClientSingleton.getInstance();
        this.patientRepo = new PatientFhirRepository();
    }

    public Observation createObservation(String observationID, String patientPN,
                                         LocalDateTime observedAt, String text) throws Exception {

        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt must not be null");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("observation text must not be blank");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        String patientFhirId = patient.getIdElement().getIdPart();

        Observation observation = new Observation();

        Identifier identifier = new Identifier();
        identifier.setSystem(OBSERVATION_ID_SYSTEM);
        identifier.setValue(observationID);
        observation.addIdentifier(identifier);

        observation.setStatus(Observation.ObservationStatus.FINAL);

        Reference patientRef = new Reference("Patient/" + patientFhirId);
        observation.setSubject(patientRef);

        Date observedDate = Date.from(observedAt.atZone(ZoneId.systemDefault()).toInstant());
        observation.setEffective(new DateTimeType(observedDate));

        observation.setValue(new StringType(text));

        MethodOutcome outcome = client.create().resource(observation).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create observation");
        }

        Observation createdObservation = (Observation) outcome.getResource();
        return createdObservation;
    }

    public Observation findByID(String observationID) throws Exception {

        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }

        Bundle results = client.search()
                .forResource(Observation.class)
                .where(Observation.IDENTIFIER.exactly().systemAndCode(OBSERVATION_ID_SYSTEM, observationID))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No observation found with ID: " + observationID);
        }

        Observation observation = (Observation) results.getEntry().get(0).getResource();
        return observation;
    }

    public List<Observation> listByPatient(String patientPN) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        String patientFhirId = patient.getIdElement().getIdPart();

        Bundle results = client.search()
                .forResource(Observation.class)
                .where(Observation.PATIENT.hasId(patientFhirId))
                .returnBundle(Bundle.class)
                .execute();

        List<Observation> observations = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Observation observation = (Observation) entry.getResource();
            observations.add(observation);
        }

        if (observations.isEmpty()) {
            throw new IllegalArgumentException("No observations found for patient: " + patientPN);
        }

        return observations;
    }

    public String getObservationDetails(String observationID) throws Exception {

        Observation observation = findByID(observationID);

        String id = getObservationID(observation);
        String status = observation.getStatus().getDisplay();

        LocalDateTime observedAt = null;
        if (observation.getEffective() instanceof DateTimeType) {
            DateTimeType dateTime = (DateTimeType) observation.getEffective();
            observedAt = LocalDateTime.ofInstant(
                    dateTime.getValue().toInstant(),
                    ZoneId.systemDefault()
            );
        }

        String text = "";
        if (observation.getValue() instanceof StringType) {
            StringType value = (StringType) observation.getValue();
            text = value.getValue();
        }

        String details = "Observation ID: " + id + "\n";
        details = details + "Status: " + status + "\n";
        details = details + "Observed At: " + observedAt + "\n";
        details = details + "Text: " + text + "\n";

        if (observation.hasSubject()) {
            Reference subjectRef = observation.getSubject();
            String patientId = subjectRef.getReferenceElement().getIdPart();
            Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
            String patientName = patient.getNameFirstRep().getNameAsSingleString();
            details = details + "Patient: " + patientName + "\n";
        }

        return details;
    }

    public Observation updateObservation(String observationID, LocalDateTime newObservedAt, String newText) throws Exception {

        if (observationID == null || observationID.isBlank()) {
            throw new IllegalArgumentException("observationID must not be blank");
        }

        Observation observation = findByID(observationID);

        if (newObservedAt != null) {
            Date observedDate = Date.from(newObservedAt.atZone(ZoneId.systemDefault()).toInstant());
            observation.setEffective(new DateTimeType(observedDate));
        }

        if (newText != null && !newText.isBlank()) {
            observation.setValue(new StringType(newText));
        }

        client.update().resource(observation).execute();

        return observation;
    }

    public String deleteObservation(String observationID, boolean confirmed) throws Exception {

        Observation observation = findByID(observationID);
        String observationFhirId = observation.getIdElement().getIdPart();
        String id = getObservationID(observation);

        if (!confirmed) {
            String warning = "WARNING: Deleting observation " + id + "\n\n";
            warning = warning + "This will delete the observation from FHIR.\n";
            warning = warning + "Call again with confirmed=true to proceed";
            return warning;
        }

        client.delete().resourceById("Observation", observationFhirId).execute();

        String result = "Successfully deleted observation " + id;
        return result;
    }

    public String getObservationID(Observation observation) {

        List<Identifier> identifiers = observation.getIdentifier();

        for (Identifier id : identifiers) {
            if (OBSERVATION_ID_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}