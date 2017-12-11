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

package org.opencb.opencga.core.results;

import org.opencb.commons.datastore.core.QueryResult;

import java.util.List;
import java.util.Map;

/**
 * Created on 07/02/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class VariantQueryResult<T> extends QueryResult<T> {

    private Map<String, List<String>> samples;
    private Boolean approximateCount;
    private String source;

    public VariantQueryResult() {
        this.samples = null;
    }


    public VariantQueryResult(String id, int dbTime, int numResults, long numTotalResults, String warningMsg, String errorMsg,
                              List<T> result, Map<String, List<String>> samples, String source) {
        this(id, dbTime, numResults, numTotalResults, warningMsg, errorMsg, result, samples, source, null);
    }

    public VariantQueryResult(String id, int dbTime, int numResults, long numTotalResults, String warningMsg, String errorMsg,
                              List<T> result, Map<String, List<String>> samples, String source, Boolean approximateCount) {
        super(id, dbTime, numResults, numTotalResults, warningMsg, errorMsg, result);
        this.samples = samples;
        this.source = source;
        this.approximateCount = approximateCount;
    }

    public VariantQueryResult(QueryResult<T> queryResult, Map<String, List<String>> samples) {
        super(queryResult.getId(),
                queryResult.getDbTime(),
                queryResult.getNumResults(),
                queryResult.getNumTotalResults(),
                queryResult.getWarningMsg(),
                queryResult.getErrorMsg(),
                queryResult.getResult());
        this.samples = samples;
        if (numTotalResults >= 0) {
            approximateCount = false;
        }
    }

    public Map<String, List<String>> getSamples() {
        return samples;
    }

    public VariantQueryResult<T> setSamples(Map<String, List<String>> samples) {
        this.samples = samples;
        return this;
    }

    public Boolean getApproximateCount() {
        return approximateCount;
    }

    public VariantQueryResult<T> setApproximateCount(Boolean approximateCount) {
        this.approximateCount = approximateCount;
        return this;
    }

    public String getSource() {
        return source;
    }

    public VariantQueryResult<T> setSource(String source) {
        this.source = source;
        return this;
    }
}
