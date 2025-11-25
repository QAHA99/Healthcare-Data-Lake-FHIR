package com.github.cm2027.lab3.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

public class MongoConfig {

    private static final String CONNECTION_STRING =
            "mongodb+srv://qusaihajali_db_user:DatabasesLab2@clinicdb.y1sitlw.mongodb.net/";

    private static final String DATABASE_NAME = "ClinicDB";
    private static final String GRIDFS_BUCKET_NAME = "message_attachments";

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static GridFSBucket gridFSBucket;

    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            ConnectionString connString = new ConnectionString(CONNECTION_STRING);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .retryWrites(true)
                    .build();
            mongoClient = MongoClients.create(settings);
        }
        return mongoClient;
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            database = getMongoClient().getDatabase(DATABASE_NAME);
        }
        return database;
    }

    public static GridFSBucket getGridFSBucket() {
        if (gridFSBucket == null) {
            gridFSBucket = GridFSBuckets.create(getDatabase(), GRIDFS_BUCKET_NAME);
        }
        return gridFSBucket;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            gridFSBucket = null;
        }
    }
}