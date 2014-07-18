/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.async;

import com.mongodb.CommandFailureException;
import org.mongodb.Document;
import org.mongodb.MongoClientURI;
import org.mongodb.MongoNamespace;

/**
 * Helper class for asynchronous tests.
 */
public final class Fixture {
    public static final String DEFAULT_URI = "mongodb://localhost:27017";
    public static final String MONGODB_URI_SYSTEM_PROPERTY_NAME = "org.mongodb.test.uri";

    private static MongoClientURI mongoClientURI;
    private static MongoClientImpl mongoClient;
    private static MongoDatabase defaultDatabase;

    private Fixture() {
    }

    public static synchronized MongoClient getMongoClient() {
        if (mongoClient == null) {
            MongoClientURI mongoURI = getMongoClientURI();
            mongoClient = (MongoClientImpl) MongoClients.create(mongoURI, mongoURI.getOptions());
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        }
        return mongoClient;
    }

    public static synchronized MongoClientURI getMongoClientURI() {
        if (mongoClientURI == null) {
            String mongoURIProperty = System.getProperty(MONGODB_URI_SYSTEM_PROPERTY_NAME);
            String mongoURIString = mongoURIProperty == null || mongoURIProperty.isEmpty()
                                    ? DEFAULT_URI : mongoURIProperty;
            mongoClientURI = new MongoClientURI(mongoURIString);
        }
        return mongoClientURI;
    }

    public static synchronized MongoDatabase getDefaultDatabase() {
        if (defaultDatabase == null) {
            defaultDatabase = getMongoClient().getDatabase("DriverTest-" + System.nanoTime());
        }
        return defaultDatabase;
    }

    public static MongoCollection<Document> initializeCollection(final MongoNamespace namespace) {
        MongoDatabase database = getMongoClient().getDatabase(namespace.getDatabaseName());
        try {
            database.executeCommand(new Document("drop", namespace.getCollectionName())).get();
        } catch (CommandFailureException e) {
            if (!e.getErrorMessage().startsWith("ns not found")) {
                throw e;
            }
        }
        return database.getCollection(namespace.getCollectionName());
    }

    public static void dropCollection(final MongoNamespace namespace) {
        try {
            getMongoClient().getDatabase(namespace.getDatabaseName())
                            .executeCommand(new Document("drop", namespace.getCollectionName())).get();
        } catch (CommandFailureException e) {
            if (!e.getErrorMessage().startsWith("ns not found")) {
                throw e;
            }
        }
    }

    static class ShutdownHook extends Thread {
        @Override
        public void run() {
            if (mongoClient != null) {
                if (defaultDatabase != null) {
                    try {
                        defaultDatabase.executeCommand(new Document("dropDatabase", defaultDatabase.getName())).get();
                    } catch (CommandFailureException e) {
                        // ignore
                    }
                }
                mongoClient.close();
                mongoClient = null;
            }
        }
    }
}