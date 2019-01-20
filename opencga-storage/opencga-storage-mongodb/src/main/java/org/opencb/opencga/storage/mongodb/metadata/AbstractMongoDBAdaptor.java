package org.opencb.opencga.storage.mongodb.metadata;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.Iterators;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.commons.datastore.mongodb.GenericDocumentComplexConverter;
import org.opencb.commons.datastore.mongodb.MongoDBCollection;
import org.opencb.commons.datastore.mongodb.MongoDataStore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static org.opencb.commons.datastore.mongodb.MongoDBCollection.UPSERT;

/**
 * Created by jacobo on 20/01/19.
 */
public class AbstractMongoDBAdaptor<T> {

    protected final MongoDataStore db;
    protected final MongoDBCollection collection;
    protected final GenericDocumentComplexConverter<T> converter;

    protected static final String SEPARATOR = "_";

    public AbstractMongoDBAdaptor(MongoDataStore db, String collectionName, Class<T> clazz) {
        this.db = db;
        this.collection = db.getCollection(collectionName)
                .withReadPreference(ReadPreference.primary())
                .withWriteConcern(WriteConcern.ACKNOWLEDGED);
        converter = new GenericDocumentComplexConverter<>(clazz);
        converter.getObjectMapper().configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
    }

    protected QueryResult createIdNameIndex() {
        return collection.createIndex(new Document("studyId", 1).append("name", 1).append("id", 1),
                new ObjectMap(MongoDBCollection.UNIQUE, true));
    }

    protected T get(int studyId, int id, QueryOptions options) {
        return get(buildPrivateId(studyId, id), options);
    }

    protected T get(Object privateId, QueryOptions options) {
        return get(eq("_id", privateId), options);
    }

    protected T get(Bson query, QueryOptions options) {
        return collection.find(query, converter, options).first();
    }

    protected T getId(Bson query) {
        return collection.find(query, fields(excludeId(), include("id")), converter, null).first();
    }

    protected void update(int studyId, int id, T object) {
        update(buildPrivateId(studyId, id), object);
    }

    protected void update(Object privateId, T object) {
        Document document = converter.convertToStorageType(object);
        Document query = new Document("_id", privateId);
        List<Bson> updates = new ArrayList<>(document.size());
        document.forEach((s, o) -> updates.add(new Document("$set", new Document(s, o))));
        collection.update(query, Updates.combine(updates), new QueryOptions(UPSERT, true));
    }

    protected Iterator<T> iterator(Bson query, QueryOptions options) {
        return Iterators.transform(
                collection.nativeQuery().find(query, options).iterator(),
                converter::convertToDataModelType);
    }

    protected static String buildPrivateId(int studyId, int id) {
        return studyId + SEPARATOR + id;
    }

    protected static Bson buildQuery(int studyId) {
        return eq("studyId", studyId);
    }

    protected static Bson buildQuery(int studyId, String name) {
        return and(eq("studyId", studyId), eq("name", name)
        );
    }
}