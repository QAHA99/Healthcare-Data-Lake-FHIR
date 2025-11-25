package com.github.cm2027.lab3.dao.mongodb;

import com.github.cm2027.lab3.config.MongoConfig;
import com.github.cm2027.lab3.model.mongo.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserRepository {

    private final MongoCollection<Document> collection;

    public UserRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("users");
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        Document doc = collection.find(Filters.eq("username", username)).first();
        if (doc == null) {
            return null;
        }
        return documentToUser(doc);
    }

    /**
     * Authenticate user (check username and password)
     */
    public User authenticate(String username, String password) {
        Document doc = collection.find(
                Filters.and(
                        Filters.eq("username", username),
                        Filters.eq("password", password)
                )
        ).first();

        if (doc == null) {
            return null;
        }
        return documentToUser(doc);
    }

    /**
     * Find user by personRef (Neo4j ID)
     */
    public User findByPersonRef(String personRef) {
        Document doc = collection.find(Filters.eq("personRef", personRef)).first();
        if (doc == null) {
            return null;
        }
        return documentToUser(doc);
    }

    /**
     * Get all users
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (Document doc : collection.find()) {
            users.add(documentToUser(doc));
        }
        return users;
    }

    /**
     * Convert MongoDB Document to User object
     */
    private User documentToUser(Document doc) {
        User user = new User();
        user.setId(doc.getObjectId("_id"));
        user.setUsername(doc.getString("username"));
        user.setPassword(doc.getString("password"));
        user.setRole(User.Role.valueOf(doc.getString("role")));
        user.setPersonRef(doc.getString("personRef"));
        user.setFirstName(doc.getString("firstName"));
        user.setLastName(doc.getString("lastName"));
        user.setName(doc.getString("Name")); // For receptionist
        user.setCreatedAt(doc.getDate("createdAt"));
        return user;
    }

    /**
     * Convert User object to MongoDB Document
     */
    private Document userToDocument(User user) {
        Document doc = new Document();
        doc.append("username", user.getUsername());
        doc.append("password", user.getPassword());
        doc.append("role", user.getRole().toString());
        doc.append("personRef", user.getPersonRef());
        doc.append("createdAt", user.getCreatedAt() != null ? user.getCreatedAt() : new Date());

        if (user.getFirstName() != null) {
            doc.append("firstName", user.getFirstName());
        }
        if (user.getLastName() != null) {
            doc.append("lastName", user.getLastName());
        }
        if (user.getName() != null) {
            doc.append("Name", user.getName());
        }

        return doc;
    }
}