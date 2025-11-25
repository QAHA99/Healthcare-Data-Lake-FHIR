package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.ClientSingleton;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FhirDataExport {

    public static void main(String[] args) throws IOException {
        System.out.println("=== FHIR Data Export ===\n");

        IGenericClient client = ClientSingleton.getInstance();
        FhirContext ctx = FhirContext.forR4();
        IParser jsonParser = ctx.newJsonParser().setPrettyPrint(true);

        System.out.println("Fetching patients from FHIR server...");
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .count(100)
                .returnBundle(Bundle.class)
                .execute();

        List<String> jsonLines = new ArrayList<>();
        int count = 0;

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            String json = jsonParser.encodeResourceToString(patient);
            jsonLines.add(json);
            count++;
        }

        System.out.println("Found " + count + " patients");
        System.out.println("Writing to patients.json...");

        try (FileWriter writer = new FileWriter("patients.json")) {
            for (String json : jsonLines) {
                writer.write(json + "\n");
            }
        }

        System.out.println("\n=== Export Complete! ===");
        System.out.println("File: patients.json");
        System.out.println("\nNext: Copy this file to the data lake and load it with Spark");
    }
}