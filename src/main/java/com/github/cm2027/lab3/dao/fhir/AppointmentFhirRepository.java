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

public class AppointmentFhirRepository {

    private static final String APPOINTMENT_ID_SYSTEM = "http://kth.se/clinic/appointment-id";
    private final IGenericClient client;
    private final PatientFhirRepository patientRepo;
    private final PractitionerFhirRepository practitionerRepo;

    public AppointmentFhirRepository() {
        this.client = ClientSingleton.getInstance();
        this.patientRepo = new PatientFhirRepository();
        this.practitionerRepo = new PractitionerFhirRepository();
    }

    public Appointment createAppointment(String appointmentID, String patientPN, String doctorID,
                                         LocalDateTime starts, LocalDateTime ends, String reason) throws Exception {

        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }
        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }
        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }
        if (starts == null || ends == null) {
            throw new IllegalArgumentException("starts and ends must not be null");
        }
        if (ends.isBefore(starts) || ends.equals(starts)) {
            throw new IllegalArgumentException("end time must be after start time");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        Practitioner practitioner = practitionerRepo.findByID(doctorID);

        String patientFhirId = patient.getIdElement().getIdPart();
        String practitionerFhirId = practitioner.getIdElement().getIdPart();

        Appointment appointment = new Appointment();

        Identifier identifier = new Identifier();
        identifier.setSystem(APPOINTMENT_ID_SYSTEM);
        identifier.setValue(appointmentID);
        appointment.addIdentifier(identifier);

        appointment.setStatus(Appointment.AppointmentStatus.BOOKED);

        Date startDate = Date.from(starts.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(ends.atZone(ZoneId.systemDefault()).toInstant());
        appointment.setStart(startDate);
        appointment.setEnd(endDate);

        if (reason != null && !reason.isBlank()) {
            appointment.setDescription(reason);
        }

        Reference patientRef = new Reference("Patient/" + patientFhirId);
        Appointment.AppointmentParticipantComponent patientParticipant = new Appointment.AppointmentParticipantComponent();
        patientParticipant.setActor(patientRef);
        patientParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        appointment.addParticipant(patientParticipant);

        Reference practitionerRef = new Reference("Practitioner/" + practitionerFhirId);
        Appointment.AppointmentParticipantComponent practitionerParticipant = new Appointment.AppointmentParticipantComponent();
        practitionerParticipant.setActor(practitionerRef);
        practitionerParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        appointment.addParticipant(practitionerParticipant);

        MethodOutcome outcome = client.create().resource(appointment).execute();

        if (!outcome.getCreated()) {
            throw new RuntimeException("Failed to create appointment");
        }

        Appointment createdAppointment = (Appointment) outcome.getResource();
        return createdAppointment;
    }

    public Appointment findByID(String appointmentID) throws Exception {

        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }

        Bundle results = client.search()
                .forResource(Appointment.class)
                .where(Appointment.IDENTIFIER.exactly().systemAndCode(APPOINTMENT_ID_SYSTEM, appointmentID))
                .returnBundle(Bundle.class)
                .execute();

        if (results.getEntry().isEmpty()) {
            throw new IllegalArgumentException("No appointment found with ID: " + appointmentID);
        }

        Appointment appointment = (Appointment) results.getEntry().get(0).getResource();
        return appointment;
    }

    public List<Appointment> listByPatient(String patientPN) throws Exception {

        if (patientPN == null || patientPN.isBlank()) {
            throw new IllegalArgumentException("patientPN must not be blank");
        }

        Patient patient = patientRepo.findByPN(patientPN);
        String patientFhirId = patient.getIdElement().getIdPart();

        Bundle results = client.search()
                .forResource(Appointment.class)
                .where(Appointment.PATIENT.hasId(patientFhirId))
                .returnBundle(Bundle.class)
                .execute();

        List<Appointment> appointments = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Appointment appointment = (Appointment) entry.getResource();
            appointments.add(appointment);
        }

        if (appointments.isEmpty()) {
            throw new IllegalArgumentException("No appointments found for patient: " + patientPN);
        }

        return appointments;
    }

    public List<Appointment> listByPractitioner(String doctorID) throws Exception {

        if (doctorID == null || doctorID.isBlank()) {
            throw new IllegalArgumentException("doctorID must not be blank");
        }

        Practitioner practitioner = practitionerRepo.findByID(doctorID);
        String practitionerFhirId = practitioner.getIdElement().getIdPart();

        Bundle results = client.search()
                .forResource(Appointment.class)
                .where(Appointment.PRACTITIONER.hasId(practitionerFhirId))
                .returnBundle(Bundle.class)
                .execute();

        List<Appointment> appointments = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            Appointment appointment = (Appointment) entry.getResource();
            appointments.add(appointment);
        }

        if (appointments.isEmpty()) {
            throw new IllegalArgumentException("No appointments found for practitioner: " + doctorID);
        }

        return appointments;
    }

    public String getAppointmentDetails(String appointmentID) throws Exception {

        Appointment appointment = findByID(appointmentID);

        String id = getAppointmentID(appointment);
        String status = appointment.getStatus().getDisplay();
        String reason = appointment.getDescription();
        if (reason == null || reason.isBlank()) {
            reason = "N/A";
        }

        LocalDateTime start = LocalDateTime.ofInstant(
                appointment.getStart().toInstant(),
                ZoneId.systemDefault()
        );
        LocalDateTime end = LocalDateTime.ofInstant(
                appointment.getEnd().toInstant(),
                ZoneId.systemDefault()
        );

        String details = "Appointment ID: " + id + "\n";
        details = details + "Status: " + status + "\n";
        details = details + "Start: " + start + "\n";
        details = details + "End: " + end + "\n";
        details = details + "Reason: " + reason + "\n";
        details = details + "Participants:\n";

        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            Reference actorRef = participant.getActor();
            String actorType = actorRef.getReferenceElement().getResourceType();
            String actorId = actorRef.getReferenceElement().getIdPart();

            if (actorType.equals("Patient")) {
                Patient patient = client.read().resource(Patient.class).withId(actorId).execute();
                String name = patient.getNameFirstRep().getNameAsSingleString();
                details = details + "  - Patient: " + name + "\n";
            }

            if (actorType.equals("Practitioner")) {
                Practitioner practitioner = client.read().resource(Practitioner.class).withId(actorId).execute();
                String name = practitioner.getNameFirstRep().getNameAsSingleString();
                details = details + "  - Practitioner: Dr. " + name + "\n";
            }
        }

        return details;
    }

    public Appointment updateAppointment(String appointmentID, LocalDateTime newStarts,
                                         LocalDateTime newEnds, String newReason) throws Exception {

        if (appointmentID == null || appointmentID.isBlank()) {
            throw new IllegalArgumentException("appointmentID must not be blank");
        }

        Appointment appointment = findByID(appointmentID);

        if (newStarts != null) {
            Date startDate = Date.from(newStarts.atZone(ZoneId.systemDefault()).toInstant());
            appointment.setStart(startDate);
        }

        if (newEnds != null) {
            Date endDate = Date.from(newEnds.atZone(ZoneId.systemDefault()).toInstant());
            appointment.setEnd(endDate);
        }

        if (newReason != null && !newReason.isBlank()) {
            appointment.setDescription(newReason);
        }

        LocalDateTime finalStart = LocalDateTime.ofInstant(
                appointment.getStart().toInstant(),
                ZoneId.systemDefault()
        );
        LocalDateTime finalEnd = LocalDateTime.ofInstant(
                appointment.getEnd().toInstant(),
                ZoneId.systemDefault()
        );

        if (finalEnd.isBefore(finalStart) || finalEnd.equals(finalStart)) {
            throw new IllegalArgumentException("end time must be after start time");
        }

        client.update().resource(appointment).execute();

        return appointment;
    }

    public String deleteAppointment(String appointmentID, boolean confirmed) throws Exception {

        Appointment appointment = findByID(appointmentID);
        String appointmentFhirId = appointment.getIdElement().getIdPart();
        String id = getAppointmentID(appointment);

        if (!confirmed) {
            String warning = "WARNING: Deleting appointment " + id + "\n\n";
            warning = warning + "This will delete the appointment from FHIR.\n";
            warning = warning + "Call again with confirmed=true to proceed";
            return warning;
        }

        client.delete().resourceById("Appointment", appointmentFhirId).execute();

        String result = "Successfully deleted appointment " + id;
        return result;
    }

    public String getAppointmentID(Appointment appointment) {

        List<Identifier> identifiers = appointment.getIdentifier();

        for (Identifier id : identifiers) {
            if (APPOINTMENT_ID_SYSTEM.equals(id.getSystem())) {
                return id.getValue();
            }
        }

        return "N/A";
    }
}