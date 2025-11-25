package com.github.cm2027.lab3.dao.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.cm2027.lab3.ClientSingleton;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class PatientFhirRepository {

    private static final String PERSONNUMMER_SYSTEM = "http://electronichealth.se/identifier/personnummer";
    private final IGenericClient client;

    public PatientFhirRepository() {
        this.client = ClientSingleton.getInstance();
    }

    public Patient createPatient(String patientPN, String firstName, String lastName,
                                 String sex, String phoneNumber, String primaryDoctorId) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        Patient patient = new Patient();

        Identifier identifier = new Identifier();
        identifier.setSystem(PERSONNUMMER_SYSTEM);
        identifier.setValue(patientPN);
        patient.addIdentifier(identifier);

        HumanName name = new HumanName();
        name.setFamily(lastName);
        name.addGiven(firstName);
        patient.addName(name);

        Enumerations.AdministrativeGender gender = mapGender(sex);
        patient.setGender(gender);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(phoneNumber);
            phone.setUse(ContactPoint.ContactPointUse.MOBILE);
            patient.addTelecom(phone);
        }

        if (primaryDoctorId != null && !primaryDoctorId.isBlank()) {
            Reference doctorRef = new Reference("Practitioner/" + primaryDoctorId);
            patient.addGeneralPractitioner(doctorRef);
        }

        patient.setActive(true);

        MethodOutcome outcome = client.create().resource(patient).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create patient");
        }

        Patient createdPatient = (Patient) outcome.getResource();
        return createdPatient;
    }

    public Patient findByPN(String patientPN) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        Bundle results = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(PERSONNUMMER_SYSTEM, patientPN))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No patient found with PN: " + patientPN);
        }

        Patient patient = (Patient) results.getEntry().get(0).getResource();
        return patient;
    }

    public List<Patient> searchByName(String firstName, String lastName) throws Exception {

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Both firstName and lastName must be provided");
        }

        Bundle results = client.search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value(lastName))
                .and(Patient.GIVEN.matches().value(firstName))
                .returnBundle(Bundle.class)
                .execute();

        List<Patient> patients = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            patients.add(patient);
        }

        if (patients.isEmpty()) {
            throw new IllegalArgumentException("No patients found with name: " + firstName + " " + lastName);
        }

        return patients;
    }

    public String getPatientSummary(String patientPN) throws Exception {

        Patient patient = findByPN(patientPN);

        String firstName = patient.getNameFirstRep().getGivenAsSingleString();
        String lastName = patient.getNameFirstRep().getFamily();

        String gender = "Unknown";
        if (patient.getGender() != null) {
            gender = patient.getGender().getDisplay();
        }

        String pn = getPatientPN(patient);

        String doctorInfo = "No primary doctor assigned";
        if (!patient.getGeneralPractitioner().isEmpty()) {
            Reference doctorRef = patient.getGeneralPractitioner().get(0);
            String practitionerId = doctorRef.getReferenceElement().getIdPart();

            try {
                Practitioner practitioner = client.read()
                        .resource(Practitioner.class)
                        .withId(practitionerId)
                        .execute();

                String drFirstName = practitioner.getNameFirstRep().getGivenAsSingleString();
                String drLastName = practitioner.getNameFirstRep().getFamily();
                doctorInfo = "Dr. " + drFirstName + " " + drLastName + " (ID: " + practitionerId + ")";
            } catch (Exception e) {
                doctorInfo = "Doctor ID: " + practitionerId + " (details unavailable)";
            }
        }

        String summary = "Patient: " + firstName + " " + lastName + " (PN: " + pn + ", Sex: " + gender + ")\n";
        summary = summary + "Primary Doctor: " + doctorInfo;

        return summary;
    }

    public Patient updatePatient(String patientPN, String firstName, String lastName,
                                 String sex, String phoneNumber, String primaryDoctorId) throws Exception {

        Patient patient = findByPN(patientPN);

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            patient.getName().clear();
            HumanName name = new HumanName();
            name.setFamily(lastName);
            name.addGiven(firstName);
            patient.addName(name);
        }

        if (sex != null && !sex.isBlank()) {
            Enumerations.AdministrativeGender gender = mapGender(sex);
            patient.setGender(gender);
        }

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            patient.getTelecom().clear();
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(phoneNumber);
            phone.setUse(ContactPoint.ContactPointUse.MOBILE);
            patient.addTelecom(phone);
        }

        if (primaryDoctorId != null && !primaryDoctorId.isBlank()) {
            patient.getGeneralPractitioner().clear();
            Reference doctorRef = new Reference("Practitioner/" + primaryDoctorId);
            patient.addGeneralPractitioner(doctorRef);
        }

        client.update().resource(patient).execute();

        return patient;
    }

    public String deletePatient(String patientPN, boolean confirmed) throws Exception {

        Patient patient = findByPN(patientPN);
        String patientId = patient.getIdElement().getIdPart();

        String firstName = patient.getNameFirstRep().getGivenAsSingleString();
        String lastName = patient.getNameFirstRep().getFamily();

        if (!confirmed) {
            String warning = "WARNING: Deleting " + firstName + " " + lastName + " (PN: " + patientPN + ")\n\n";
            warning = warning + "This will delete the patient from FHIR.\n";
            warning = warning + "Call again with confirmed=true to proceed";
            return warning;
        }

        client.delete().resourceById("Patient", patientId).execute();

        String result = "Successfully deleted " + firstName + " " + lastName + " (PN: " + patientPN + ")";
        return result;
    }

    public String getPrimaryDoctorId(Patient patient) {
        if (patient.getGeneralPractitioner().isEmpty()) {
            return null;
        }

        Reference doctorRef = patient.getGeneralPractitioner().get(0);
        return doctorRef.getReferenceElement().getIdPart();
    }

    public String getPrimaryDoctorIdentifier(Patient patient) throws Exception {
        String doctorFhirId = getPrimaryDoctorId(patient);
        if (doctorFhirId == null) {
            return null;
        }

        try {
            Practitioner practitioner = client.read()
                    .resource(Practitioner.class)
                    .withId(doctorFhirId)
                    .execute();

            List<Identifier> identifiers = practitioner.getIdentifier();
            for (Identifier id : identifiers) {
                if ("http://kth.se/clinic/doctor-id".equals(id.getSystem())) {
                    return id.getValue();
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private Enumerations.AdministrativeGender mapGender(String sex) {

        if (sex == null) {
            return Enumerations.AdministrativeGender.UNKNOWN;
        }

        String sexUpper = sex.toUpperCase();

        if (sexUpper.equals("MAN") || sexUpper.equals("MALE") || sexUpper.equals("M")) {
            return Enumerations.AdministrativeGender.MALE;
        }

        if (sexUpper.equals("KVINNA") || sexUpper.equals("FEMALE") || sexUpper.equals("F")) {
            return Enumerations.AdministrativeGender.FEMALE;
        }

        if (sexUpper.equals("ÖVRIGT") || sexUpper.equals("OTHER") || sexUpper.equals("O")) {
            return Enumerations.AdministrativeGender.OTHER;
        }

        return Enumerations.AdministrativeGender.UNKNOWN;
    }

    public String getPatientPN(Patient patient) {

        List<Identifier> identifiers = patient.getIdentifier();

        for (Identifier id : identifiers) {
            if (PERSONNUMMER_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}