package com.github.cm2027.lab3.util;

import org.apache.spark.sql.SparkSession;

/**
 * Simple test to verify Spark connection works
 */
public class SparkConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Spark Connection ===\n");

        try {
            System.out.println("Step 1: Creating Spark session...");
            SparkSession spark = SparkSession.builder()
                    .appName("Connection Test")
                    .master("spark://sparkmaster:7077")
                    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    .getOrCreate();

            System.out.println("✓ Spark session created successfully!");
            System.out.println("Spark version: " + spark.version());
            System.out.println("Master: " + spark.sparkContext().master());

            System.out.println("\nStep 2: Testing simple operation...");
            long count = spark.range(0, 100).count();
            System.out.println("✓ Test operation successful! Count: " + count);

            System.out.println("\nStep 3: Stopping Spark session...");
            spark.stop();
            System.out.println("✓ Connection test completed successfully!");

            System.out.println("\n=== SUCCESS: Spark is working! ===");

        } catch (Exception e) {
            System.err.println("\n=== CONNECTION FAILED ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nPossible issues:");
            System.err.println("1. Docker containers not running (run 'make up' in lab3-datalake)");
            System.err.println("2. Spark master not accessible");
            System.err.println("3. Network configuration issue");
            e.printStackTrace();
        }
    }
}