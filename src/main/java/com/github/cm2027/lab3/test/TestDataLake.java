package com.github.cm2027.lab3.test;

import com.github.cm2027.lab3.datalake.DataLakeService;

/**
 * Test class for Data Lake integration
 */
public class TestDataLake {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("Testing Data Lake Integration");
        System.out.println("=================================\n");

        try {
            // Initialize service
            DataLakeService service = new DataLakeService();

            // Test 1: Sync patients
            System.out.println("\n--- Test 1: Syncing Patients ---");
            service.syncPatients();

            // Test 2: Show patient statistics
            System.out.println("\n--- Test 2: Patient Statistics ---");
            service.showPatientStats();

            // Test 3: Sync appointments
            System.out.println("\n--- Test 3: Syncing Appointments ---");
            service.syncAppointments();

            // Test 4: Show appointment statistics
            System.out.println("\n--- Test 4: Appointment Statistics ---");
            service.showAppointmentStats();

            // Cleanup
            service.close();

            System.out.println("\n=================================");
            System.out.println("✓ All tests passed!");
            System.out.println("=================================");

        } catch (Exception e) {
            System.err.println("\n✗ Test failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}