package com.github.cm2027.lab3.dao.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.cm2027.lab3.ClientSingleton;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class PractitionerFhirRepository {

    private static final String DOCTOR_ID_SYSTEM = "http://kth.se/clinic/doctor-id";
    private final IGenericClient client;

    public PractitionerFhirRepository() {
        this.client = ClientSingleton.getInstance();
    }

    public Practitioner createPractitioner(String doctorID, String firstName, String lastName, String phoneNumber) throws Exception {

        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        Practitioner practitioner = new Practitioner();

        Identifier identifier = new Identifier();
        identifier.setSystem(DOCTOR_ID_SYSTEM);
        identifier.setValue(doctorID);
        practitioner.addIdentifier(identifier);

        HumanName name = new HumanName();
        name.setFamily(lastName);
        name.addGiven(firstName);
        practitioner.addName(name);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(phoneNumber);
            phone.setUse(ContactPoint.ContactPointUse.WORK);
            practitioner.addTelecom(phone);
        }

        practitioner.setActive(true);

        MethodOutcome outcome = client.create().resource(practitioner).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create practitioner");
        }

        Practitioner createdPractitioner = (Practitioner) outcome.getResource();
        return createdPractitioner;
    }

    public Practitioner findByID(String doctorID) throws Exception {

        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        Bundle results = client.search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().systemAndCode(DOCTOR_ID_SYSTEM, doctorID))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No practitioner found with ID: " + doctorID);
        }

        Practitioner practitioner = (Practitioner) results.getEntry().get(0).getResource();
        return practitioner;
    }

    public List<Practitioner> searchByName(String firstName, String lastName) throws Exception {

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Both firstName and lastName must be provided");
        }

        Bundle results = client.search()
                .forResource(Practitioner.class)
                .where(Practitioner.FAMILY.matches().value(lastName))
                .and(Practitioner.GIVEN.matches().value(firstName))
                .returnBundle(Bundle.class)
                .execute();

        List<Practitioner> practitioners = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Practitioner practitioner = (Practitioner) entry.getResource();
            practitioners.add(practitioner);
        }

        if (practitioners.isEmpty()) {
            throw new IllegalArgumentException("No practitioners found with name: " + firstName + " " + lastName);
        }

        return practitioners;
    }

    public String getPractitionerSummary(String doctorID) throws Exception {

        Practitioner practitioner = findByID(doctorID);

        String firstName = practitioner.getNameFirstRep().getGivenAsSingleString();
        String lastName = practitioner.getNameFirstRep().getFamily();
        String id = getDoctorID(practitioner);

        String phone = "N/A";
        if (!practitioner.getTelecom().isEmpty()) {
            phone = practitioner.getTelecom().get(0).getValue();
        }

        String summary = "Practitioner: Dr. " + firstName + " " + lastName + " (ID: " + id + ")\n";
        summary = summary + "Phone: " + phone;

        return summary;
    }

    public Practitioner updatePractitioner(String doctorID, String firstName, String lastName, String phoneNumber) throws Exception {

        Practitioner practitioner = findByID(doctorID);

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            practitioner.getName().clear();
            HumanName name = new HumanName();
            name.setFamily(lastName);
            name.addGiven(firstName);
            practitioner.addName(name);
        }

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            practitioner.getTelecom().clear();
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(phoneNumber);
            phone.setUse(ContactPoint.ContactPointUse.WORK);
            practitioner.addTelecom(phone);
        }

        client.update().resource(practitioner).execute();

        return practitioner;
    }

    public String deletePractitioner(String doctorID, boolean confirmed) throws Exception {

        Practitioner practitioner = findByID(doctorID);
        String practitionerId = practitioner.getIdElement().getIdPart();

        String firstName = practitioner.getNameFirstRep().getGivenAsSingleString();
        String lastName = practitioner.getNameFirstRep().getFamily();

        if (!confirmed) {
            String warning = "WARNING: Deleting Dr. " + firstName + " " + lastName + " (ID: " + doctorID + ")\n\n";
            warning = warning + "This will delete the practitioner from FHIR.\n";
            warning = warning + "Call again with confirmed=true to proceed";
            return warning;
        }

        client.delete().resourceById("Practitioner", practitionerId).execute();

        String result = "Successfully deleted Dr. " + firstName + " " + lastName + " (ID: " + doctorID + ")";
        return result;
    }

    public String getDoctorID(Practitioner practitioner) {

        List<Identifier> identifiers = practitioner.getIdentifier();

        for (Identifier id : identifiers) {
            if (DOCTOR_ID_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}