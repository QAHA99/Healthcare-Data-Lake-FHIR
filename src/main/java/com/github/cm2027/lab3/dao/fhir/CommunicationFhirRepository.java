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

public class CommunicationFhirRepository {

    private static final String COMMUNICATION_ID_SYSTEM = "http://kth.se/clinic/communication-id";
    private final IGenericClient client;
    private final PatientFhirRepository patientRepo;
    private final PractitionerFhirRepository practitionerRepo;

    public CommunicationFhirRepository() {
        this.client = ClientSingleton.getInstance();
        this.patientRepo = new PatientFhirRepository();
        this.practitionerRepo = new PractitionerFhirRepository();
    }

    public Communication sendMessage(String communicationID, String senderPN, String senderDoctorID,
                                     String recipientPN, String recipientDoctorID, String messageText) throws Exception {

        if (communicationID == null || communicationID.isBlank()) {
            throw new IllegalArgumentException("communicationID must not be blank");
        }
        if (messageText == null || messageText.isBlank()) {
            throw new IllegalArgumentException("messageText must not be blank");
        }

        Communication communication = new Communication();

        Identifier identifier = new Identifier();
        identifier.setSystem(COMMUNICATION_ID_SYSTEM);
        identifier.setValue(communicationID);
        communication.addIdentifier(identifier);

        communication.setStatus(Communication.CommunicationStatus.COMPLETED);

        if (senderPN != null && !senderPN.isBlank()) {
            Patient senderPatient = patientRepo.findByPN(senderPN);
            String senderFhirId = senderPatient.getIdElement().getIdPart();
            Reference senderRef = new Reference("Patient/" + senderFhirId);
            communication.setSender(senderRef);
        } else if (senderDoctorID != null && !senderDoctorID.isBlank()) {
            Practitioner senderPractitioner = practitionerRepo.findByID(senderDoctorID);
            String senderFhirId = senderPractitioner.getIdElement().getIdPart();
            Reference senderRef = new Reference("Practitioner/" + senderFhirId);
            communication.setSender(senderRef);
        } else {
            throw new IllegalArgumentException("Either senderPN or senderDoctorID must be provided");
        }

        if (recipientPN != null && !recipientPN.isBlank()) {
            Patient recipientPatient = patientRepo.findByPN(recipientPN);
            String recipientFhirId = recipientPatient.getIdElement().getIdPart();
            Reference recipientRef = new Reference("Patient/" + recipientFhirId);
            communication.addRecipient(recipientRef);
        } else if (recipientDoctorID != null && !recipientDoctorID.isBlank()) {
            Practitioner recipientPractitioner = practitionerRepo.findByID(recipientDoctorID);
            String recipientFhirId = recipientPractitioner.getIdElement().getIdPart();
            Reference recipientRef = new Reference("Practitioner/" + recipientFhirId);
            communication.addRecipient(recipientRef);
        } else {
            throw new IllegalArgumentException("Either recipientPN or recipientDoctorID must be provided");
        }

        Communication.CommunicationPayloadComponent payload = new Communication.CommunicationPayloadComponent();
        payload.setContent(new StringType(messageText));
        communication.addPayload(payload);

        Date sentDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        communication.setSent(sentDate);

        MethodOutcome outcome = client.create().resource(communication).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create communication");
        }

        Communication createdCommunication = (Communication) outcome.getResource();
        return createdCommunication;
    }

    public Communication findByID(String communicationID) throws Exception {

        if (communicationID == null || communicationID.isBlank()) {
            throw new IllegalArgumentException("communicationID must not be blank");
        }

        Bundle results = client.search()
                .forResource(Communication.class)
                .where(Communication.IDENTIFIER.exactly().systemAndCode(COMMUNICATION_ID_SYSTEM, communicationID))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No communication found with ID: " + communicationID);
        }

        Communication communication = (Communication) results.getEntry().get(0).getResource();
        return communication;
    }

    public List<Communication> getMessagesBetween(String personPN1, String doctorID1,
                                                  String personPN2, String doctorID2) throws Exception {

        String fhirId1 = null;
        String fhirId2 = null;

        if (personPN1 != null && !personPN1.isBlank()) {
            Patient patient = patientRepo.findByPN(personPN1);
            fhirId1 = patient.getIdElement().getIdPart();
        } else if (doctorID1 != null && !doctorID1.isBlank()) {
            Practitioner practitioner = practitionerRepo.findByID(doctorID1);
            fhirId1 = practitioner.getIdElement().getIdPart();
        }

        if (personPN2 != null && !personPN2.isBlank()) {
            Patient patient = patientRepo.findByPN(personPN2);
            fhirId2 = patient.getIdElement().getIdPart();
        } else if (doctorID2 != null && !doctorID2.isBlank()) {
            Practitioner practitioner = practitionerRepo.findByID(doctorID2);
            fhirId2 = practitioner.getIdElement().getIdPart();
        }

        if (fhirId1 == null || fhirId2 == null) {
            throw new IllegalArgumentException("Could not find both participants");
        }

        Bundle results = client.search()
                .forResource(Communication.class)
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> conversations = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Communication comm = (Communication) entry.getResource();

            String senderId = null;
            if (comm.hasSender()) {
                senderId = comm.getSender().getReferenceElement().getIdPart();
            }

            String recipientId = null;
            if (!comm.getRecipient().isEmpty()) {
                recipientId = comm.getRecipient().get(0).getReferenceElement().getIdPart();
            }

            boolean matches = false;
            if (senderId != null && recipientId != null) {
                if ((senderId.equals(fhirId1) && recipientId.equals(fhirId2)) ||
                        (senderId.equals(fhirId2) && recipientId.equals(fhirId1))) {
                    matches = true;
                }
            }

            if (matches) {
                conversations.add(comm);
            }
        }

        return conversations;
    }

    public List<Communication> getMyMessages(String personPN, String doctorID) throws Exception {

        String fhirId = null;

        if (personPN != null && !personPN.isBlank()) {
            Patient patient = patientRepo.findByPN(personPN);
            fhirId = patient.getIdElement().getIdPart();
        } else if (doctorID != null && !doctorID.isBlank()) {
            Practitioner practitioner = practitionerRepo.findByID(doctorID);
            fhirId = practitioner.getIdElement().getIdPart();
        } else {
            throw new IllegalArgumentException("Either personPN or doctorID must be provided");
        }

        Bundle results = client.search()
                .forResource(Communication.class)
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> myMessages = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Communication comm = (Communication) entry.getResource();

            boolean isInvolved = false;

            if (comm.hasSender()) {
                String senderId = comm.getSender().getReferenceElement().getIdPart();
                if (senderId.equals(fhirId)) {
                    isInvolved = true;
                }
            }

            if (!comm.getRecipient().isEmpty()) {
                String recipientId = comm.getRecipient().get(0).getReferenceElement().getIdPart();
                if (recipientId.equals(fhirId)) {
                    isInvolved = true;
                }
            }

            if (isInvolved) {
                myMessages.add(comm);
            }
        }

        return myMessages;
    }

    public String getCommunicationDetails(String communicationID) throws Exception {

        Communication communication = findByID(communicationID);

        String id = getCommunicationID(communication);
        String status = communication.getStatus().getDisplay();

        LocalDateTime sent = null;
        if (communication.hasSent()) {
            sent = LocalDateTime.ofInstant(
                    communication.getSent().toInstant(),
                    ZoneId.systemDefault()
            );
        }

        String messageText = "";
        if (!communication.getPayload().isEmpty()) {
            Communication.CommunicationPayloadComponent payload = communication.getPayload().get(0);
            if (payload.getContent() instanceof StringType) {
                StringType content = (StringType) payload.getContent();
                messageText = content.getValue();
            }
        }

        String details = "Communication ID: " + id + "\n";
        details = details + "Status: " + status + "\n";
        details = details + "Sent: " + sent + "\n";
        details = details + "Message: " + messageText + "\n";

        if (communication.hasSender()) {
            Reference senderRef = communication.getSender();
            String senderType = senderRef.getReferenceElement().getResourceType();
            String senderId = senderRef.getReferenceElement().getIdPart();

            if (senderType.equals("Patient")) {
                Patient patient = client.read().resource(Patient.class).withId(senderId).execute();
                String name = patient.getNameFirstRep().getNameAsSingleString();
                details = details + "Sender: Patient " + name + "\n";
            } else if (senderType.equals("Practitioner")) {
                Practitioner practitioner = client.read().resource(Practitioner.class).withId(senderId).execute();
                String name = practitioner.getNameFirstRep().getNameAsSingleString();
                details = details + "Sender: Dr. " + name + "\n";
            }
        }

        if (!communication.getRecipient().isEmpty()) {
            Reference recipientRef = communication.getRecipient().get(0);
            String recipientType = recipientRef.getReferenceElement().getResourceType();
            String recipientId = recipientRef.getReferenceElement().getIdPart();

            if (recipientType.equals("Patient")) {
                Patient patient = client.read().resource(Patient.class).withId(recipientId).execute();
                String name = patient.getNameFirstRep().getNameAsSingleString();
                details = details + "Recipient: Patient " + name + "\n";
            } else if (recipientType.equals("Practitioner")) {
                Practitioner practitioner = client.read().resource(Practitioner.class).withId(recipientId).execute();
                String name = practitioner.getNameFirstRep().getNameAsSingleString();
                details = details + "Recipient: Dr. " + name + "\n";
            }
        }

        return details;
    }

    public String getCommunicationID(Communication communication) {

        List<Identifier> identifiers = communication.getIdentifier();

        for (Identifier id : identifiers) {
            if (COMMUNICATION_ID_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}