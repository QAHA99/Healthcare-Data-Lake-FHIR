package com.github.cm2027.lab3.datalake;

import com.github.cm2027.lab3.ClientSingleton;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for syncing FHIR data to the data lake using Apache Hudi
 */
public class DataLakeService {

    private final SparkSession spark;
    private final IGenericClient fhirClient;
    private static final String HUDI_BASE_PATH = "/tmp/datalake/hudi";

    public DataLakeService() {
        // Initialize Spark session in LOCAL mode (no Docker cluster needed)
        this.spark = SparkSession.builder()
                .appName("Lab3-FHIR-DataLake")
                .master("local[*]")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.sql.extensions", "org.apache.spark.sql.hudi.HoodieSparkSessionExtension")
                .config("spark.driver.host", "localhost")
                .getOrCreate();

        // Get FHIR client
        this.fhirClient = ClientSingleton.getInstance();
    }

    /**
     * Sync all patients from FHIR to data lake
     */
    public void syncPatients() {
        System.out.println("Syncing patients from FHIR to data lake...");

        // Extract patients from FHIR
        Bundle bundle = fhirClient.search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();

        // Transform to rows
        List<Row> rows = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Patient patient = (Patient) entry.getResource();

            String id = patient.getIdElement().getIdPart();
            String firstName = patient.getNameFirstRep().getGivenAsSingleString();
            String lastName = patient.getNameFirstRep().getFamily();
            String gender = patient.hasGender() ? patient.getGender().toString() : "UNKNOWN";
            Long timestamp = System.currentTimeMillis();

            rows.add(RowFactory.create(id, firstName, lastName, gender, timestamp));
        }

        // Define schema
        StructType schema = new StructType()
                .add("id", DataTypes.StringType, false)
                .add("firstName", DataTypes.StringType, true)
                .add("lastName", DataTypes.StringType, true)
                .add("gender", DataTypes.StringType, true)
                .add("syncTimestamp", DataTypes.LongType, false);

        // Create DataFrame
        Dataset<Row> df = spark.createDataFrame(rows, schema);

        // Write to Hudi table
        df.write()
                .format("hudi")
                .option("hoodie.table.name", "patients")
                .option("hoodie.datasource.write.recordkey.field", "id")
                .option("hoodie.datasource.write.precombine.field", "syncTimestamp")
                .option("hoodie.datasource.write.operation", "upsert")
                .mode(SaveMode.Append)
                .save(HUDI_BASE_PATH + "/patients");

        System.out.println("✓ Synced " + rows.size() + " patients to data lake");
    }

    /**
     * Sync all appointments from FHIR to data lake
     */
    public void syncAppointments() {
        System.out.println("Syncing appointments from FHIR to data lake...");

        // Extract appointments from FHIR
        Bundle bundle = fhirClient.search()
                .forResource(Appointment.class)
                .returnBundle(Bundle.class)
                .execute();

        // Transform to rows
        List<Row> rows = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Appointment appointment = (Appointment) entry.getResource();

            String id = appointment.getIdElement().getIdPart();
            Long startTime = appointment.hasStart() ? appointment.getStart().getTime() : 0L;
            String description = appointment.hasDescription() ? appointment.getDescription() : "";
            String status = appointment.hasStatus() ? appointment.getStatus().toString() : "UNKNOWN";
            Long timestamp = System.currentTimeMillis();

            rows.add(RowFactory.create(id, startTime, description, status, timestamp));
        }

        // Define schema
        StructType schema = new StructType()
                .add("id", DataTypes.StringType, false)
                .add("startTime", DataTypes.LongType, true)
                .add("description", DataTypes.StringType, true)
                .add("status", DataTypes.StringType, true)
                .add("syncTimestamp", DataTypes.LongType, false);

        // Create DataFrame
        Dataset<Row> df = spark.createDataFrame(rows, schema);

        // Write to Hudi table
        df.write()
                .format("hudi")
                .option("hoodie.table.name", "appointments")
                .option("hoodie.datasource.write.recordkey.field", "id")
                .option("hoodie.datasource.write.precombine.field", "syncTimestamp")
                .option("hoodie.datasource.write.operation", "upsert")
                .mode(SaveMode.Append)
                .save(HUDI_BASE_PATH + "/appointments");

        System.out.println("✓ Synced " + rows.size() + " appointments to data lake");
    }

    /**
     * Show patient statistics from data lake
     */
    public void showPatientStats() {
        System.out.println("\n=== PATIENT STATISTICS ===");

        Dataset<Row> patients = spark.read()
                .format("hudi")
                .load(HUDI_BASE_PATH + "/patients");

        System.out.println("\nTotal patients: " + patients.count());

        System.out.println("\nPatients by gender:");
        patients.groupBy("gender").count().show();

        System.out.println("\nSample patients:");
        patients.select("firstName", "lastName", "gender").show(10);
    }

    /**
     * Show appointment statistics from data lake
     */
    public void showAppointmentStats() {
        System.out.println("\n=== APPOINTMENT STATISTICS ===");

        Dataset<Row> appointments = spark.read()
                .format("hudi")
                .load(HUDI_BASE_PATH + "/appointments");

        System.out.println("\nTotal appointments: " + appointments.count());

        System.out.println("\nAppointments by status:");
        appointments.groupBy("status").count().show();

        System.out.println("\nRecent appointments:");
        appointments.select("description", "status").show(10);
    }

    /**
     * Close Spark session
     */
    public void close() {
        if (spark != null) {
            spark.close();
        }
    }
}