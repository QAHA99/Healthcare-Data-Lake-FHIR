package com.github.cm2027.lab3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

import com.github.cm2027.lab3.util.StreamUtil;
import com.google.common.base.Optional;

import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

public class Example {

    public static void main(String[] args) {
        getPatientById("dd256214-a911-bbc9-bc56-2976d2336c93");

        getPatients();

        getPatientsPaginated();

        getPatientsPaginated(2, 10);

        countPatients();

        queryEncountersByObservationCode("http://loinc.org", "4548-4");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse("2019-06-03", formatter);
        LocalDate end = LocalDate.parse("2019-06-03", formatter);

        queryPatientsByEncountersInDateRange(start, end);

        Patient patient = new Patient();
        Identifier identifier = new Identifier()
                .setSystem("http://electronichealth.se/identifier/personnummer")
                .setValue("19710930-7905");
        identifier.getType()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("PN")
                        .setDisplay("Personnummer"))
                .setText("Personnummer");
        patient.addIdentifier(identifier);
        patient.addName(new HumanName()
                .setFamily("Doe")
                .addGiven("John"));
        patient.setGender(AdministrativeGender.MALE);
        patient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType("1970-01-01"));

        var created = createPatient(patient);
        if (created.isPresent()) {
            deletePatient(created.get().getIdPart());
        }
    }

    /**
     * Simple get by id for Patients
     * 
     * @param id the id of the patient, note that this will search for the id field
     *           and not the fhir identifiers.
     */
    static void getPatientById(String id) {
        var patient = ClientSingleton.getInstance()
                .read()
                .resource(Patient.class)
                .withId(id)
                .execute();
        System.out.printf("Got Patient with id: %s, Patient: %s\n", id, patient.toString());
    }

    /**
     * Get multiple patients limited by count
     * 
     * @param count the maximum resources to return.
     */
    static void getPatients(int count) {
        ClientSingleton.getInstance()
                .search()
                .forResource(Patient.class)
                .count(count)
                .returnBundle(Bundle.class)
                .execute()
                .getEntry()
                .stream()
                .forEach(entry -> {
                    System.out.printf("Patient ID: %s\n", entry.getResource().getIdPart());
                });
    }

    /**
     * Wrapper for getPatients, just applies default max value of 10 resources.
     */
    static void getPatients() {
        getPatients(10);
    }

    /**
     * Pagination query of patients
     * 
     * @param page The page to retrieve.
     * @param size The size of each page.
     */
    static void getPatientsPaginated(int page, int size) {
        ClientSingleton.getInstance()
                .search()
                .forResource(Patient.class)
                .count(size)
                .offset(size * page)
                .returnBundle(Bundle.class)
                .execute()
                .getEntry()
                .stream()
                .forEach(entry -> {
                    System.out.printf("Patient ID: %s\n", entry.getResource().getIdPart());
                });
    }

    /**
     * Wrapper for getPatientsPaginated, just applies defualt values of first page
     * with pageSize 10.
     */
    static void getPatientsPaginated() {
        getPatientsPaginated(0, 10);
    }

    /**
     * Counts the total number of patients.
     */
    static void countPatients() {
        System.out.printf("Total patients: %d\n", ClientSingleton.getInstance()
                .search()
                .forResource(Patient.class)
                .summaryMode(SummaryEnum.COUNT)
                .returnBundle(Bundle.class)
                .execute()
                // total is not populated in searches where the resources are returned
                .getTotal());
    }

    /**
     * Queries the encounters that have observations linked with the provided system
     * and code.
     * 
     * @param observationSystem The system used for the code (most often
     *                          http://loinc.org).
     * @param observationCode   The code in the specified system.
     */
    static void queryEncountersByObservationCode(String observationSystem, String observationCode) {
        // StreamUtil is just a utility that iterates through all pages returned
        // implementing java Streaming
        new StreamUtil<Encounter>(ClientSingleton.getInstance(), Encounter.class)
                .streamAll(client -> client
                        .search()
                        .forResource(Encounter.class)
                        .where(new TokenClientParam("_has:Observation:encounter:code")
                                .exactly()
                                .systemAndCode(observationSystem, observationCode))
                        .returnBundle(Bundle.class)
                        .execute())
                .forEach(encounter -> {
                    System.out.println("Encounter ID: " + encounter.getIdElement().getIdPart());
                });
    }

    /**
     * Queries the patients that have had encounters that overlap the specified
     * range.
     * 
     * @param start Start date.
     * @param end   End date.
     */
    static void queryPatientsByEncountersInDateRange(LocalDate start, LocalDate end) {
        DateTimeFormatter fhirFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        System.out.printf("Patients with encounters in the date range: %s <=> %s\n", start, end);
        new StreamUtil<Patient>(ClientSingleton.getInstance(), Patient.class)
                .streamAll(client -> client
                        .search()
                        .forResource(Patient.class)
                        .where(new DateClientParam("_has:Encounter:patient:date")
                                .afterOrEquals()
                                .day(start.format(fhirFormatter)))
                        // .and or where is the same here so it doesnt matter if .and or .where is used
                        .and(new DateClientParam("_has:Encounter:patient:date")
                                .beforeOrEquals()
                                .day(end.format(fhirFormatter)))
                        .returnBundle(Bundle.class)
                        .execute())
                .forEach(patitent -> {
                    System.out.printf("Patient ID: %s\n", patitent.getIdPart());
                });
    }

    /**
     * Creates the given patient.
     */
    static Optional<Patient> createPatient(Patient patient) {
        var outcome = ClientSingleton.getInstance()
                .create()
                .resource(patient)
                .prefer(PreferReturnEnum.REPRESENTATION)
                .execute();
        if (outcome.getCreated()) {
            var created = outcome.getResource();
            System.out.printf("Created patient: %s\n", created.getIdElement().getIdPart());
            return Optional.of((Patient) created);
        }
        System.out.printf("Could not create patient, code: %d\n", outcome.getResponseStatusCode());
        return Optional.absent();
    }

    /**
     * Deletes the patient with the given id.
     */
    static void deletePatient(String id) {
        var outcome = ClientSingleton.getInstance()
                .delete()
                .resourceById(Patient.class.getSimpleName(), id)
                .execute();
        if (outcome.getResponseStatusCode() >= 200 && outcome.getResponseStatusCode() < 300) {
            System.out.printf("Deleted patient with id: %s\n", id);
        }
    }

}
