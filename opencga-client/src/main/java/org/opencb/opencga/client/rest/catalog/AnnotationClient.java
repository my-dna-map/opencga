/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.client.rest.catalog;

import org.opencb.commons.datastore.core.DataResponse;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.core.models.AnnotationSet;

import java.io.IOException;

/**
 * Created by pfurio on 28/07/16.
 */
public abstract class AnnotationClient<T> extends CatalogClient<T> {

    protected AnnotationClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);
    }

    @Deprecated
    public DataResponse<AnnotationSet> createAnnotationSet(String studyId, String id, String variableSetId, String annotationSetId,
                                                            ObjectMap annotations) throws IOException {
        ObjectMap bodyParams = new ObjectMap();
        bodyParams.putIfNotEmpty("id", annotationSetId);
        bodyParams.putIfNotNull("annotations", annotations);

        ObjectMap params = new ObjectMap()
                .append("body", bodyParams)
                .append("study", studyId)
                .append("variableSet", variableSetId);
        return execute(category, id, "annotationsets", null, "create", params, POST, AnnotationSet.class);
    }

    public DataResponse<AnnotationSet> getAnnotationSets(String id, ObjectMap params) throws IOException {
        return execute(category, id, "annotationsets", params, GET, AnnotationSet.class);
    }

    public DataResponse<AnnotationSet> searchAnnotationSets(String id, ObjectMap params) throws IOException {
        return execute(category, id, "annotationsets", null, "search", params, GET, AnnotationSet.class);
    }

    public DataResponse<AnnotationSet> deleteAnnotationSet(String id, String annotationSetName, ObjectMap params) throws IOException {
        return execute(category, id, "annotationsets", annotationSetName, "delete", params, GET, AnnotationSet.class);
    }

    public DataResponse<AnnotationSet> updateAnnotationSet(String studyId, String id, String annotationSetId, ObjectMap queryParams,
                                                            ObjectMap annotations) throws IOException {
        ObjectMap params = new ObjectMap("body", annotations);
        queryParams.putIfNotEmpty("study", studyId);
        params.putAll(queryParams);
        return execute(category, id, "annotationsets", annotationSetId, "update", params, POST, AnnotationSet.class);
    }

}
