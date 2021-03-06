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

import org.opencb.commons.datastore.core.*;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.core.models.Cohort;
import org.opencb.opencga.core.models.Sample;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Created by imedina on 24/05/16.
 */
public class CohortClient extends AnnotationClient<Cohort> {

    private static final String COHORT_URL = "cohorts";

    public CohortClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);

        this.category = COHORT_URL;
        this.clazz = Cohort.class;
    }

    public DataResponse<Cohort> create(String studyId, @Nullable String variableSetId, @Nullable String variable, ObjectMap bodyParams)
            throws IOException {
        ObjectMap params = new ObjectMap();
        params.putIfNotNull("body", bodyParams);
        params.putIfNotNull(STUDY, studyId);
        params.putIfNotEmpty("variableSet", variableSetId);
        params.putIfNotEmpty("variable", variable);
        return execute(COHORT_URL, "create", params, POST, Cohort.class);
    }

    public DataResponse<Sample> getSamples(String cohortId, Query query, QueryOptions options) throws IOException {
        ObjectMap params = new ObjectMap(query);
        params.putAll(options);
        return execute(COHORT_URL, cohortId, "samples", params, GET, Sample.class);
    }

    public DataResponse<ObjectMap> groupBy(String studyId, String fields, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "study", studyId, "fields", fields);
        return execute(COHORT_URL, "groupBy", params, GET, ObjectMap.class);
    }

    public DataResponse<FacetField> stats(String study, Query query, QueryOptions queryOptions) throws IOException {
        ObjectMap params = new ObjectMap(query);
        params.putAll(queryOptions);
        params.put("study", study);
        return execute(COHORT_URL, "stats", params, GET, FacetField.class);
    }

}
