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

package org.opencb.opencga.analysis.clinical.interpretation;

import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.annotations.Tool;
import org.opencb.opencga.core.exception.ToolException;
import org.opencb.opencga.core.models.ClinicalAnalysis;
import org.opencb.opencga.core.results.OpenCGAResult;

import java.util.List;

@Tool(id = CancerTieringInterpretationAnalysis.ID, type = Tool.ToolType.CLINICAL)
public class CancerTieringInterpretationAnalysis extends InterpretationAnalysis {

    public final static String ID = "cancer-tiering-interpretation";

    private String studyId;
    private String clinicalAnalysisId;
    private List<String> variantIdsToDiscard;
    private CancerTieringInterpretationConfiguration config;

    private ClinicalAnalysis clinicalAnalysis;

    protected void check() throws Exception {
        super.check();

        // Check study
        if (StringUtils.isEmpty(studyId)) {
            // Missing study
            throw new ToolException("Missing study ID");
        }

        // Check clinical analysis
        if (StringUtils.isEmpty(clinicalAnalysisId)) {
            throw new ToolException("Missing clinical analysis ID");
        }

        // Get clinical analysis to ckeck proband sample ID, family ID
        OpenCGAResult<ClinicalAnalysis> clinicalAnalysisQueryResult;
        try {
            clinicalAnalysisQueryResult = catalogManager.getClinicalAnalysisManager().get(studyId, clinicalAnalysisId, QueryOptions.empty(),
                    token);
        } catch (CatalogException e) {
            throw new ToolException(e);
        }
        if (clinicalAnalysisQueryResult.getNumResults() != 1) {
            throw new ToolException("Clinical analysis " + clinicalAnalysisId + " not found in study " + studyId);
        }

        clinicalAnalysis = clinicalAnalysisQueryResult.first();

        // Update executor params with OpenCGA home and session ID
        setUpStorageEngineExecutor(studyId);
    }

    @Override
    protected void run() throws ToolException {
        step(() -> {
            getToolExecutor(CancerTieringInterpretationAnalysisExecutor.class)
                    .setStudyId(studyId)
                    .setClinicalAnalysisId(clinicalAnalysisId)
                    .setVariantIdsToDiscard(variantIdsToDiscard)
                    .setConfig(config)
                    .execute();

            saveInterpretation(studyId, clinicalAnalysis, null, null, config);
        });
    }

    public String getStudyId() {
        return studyId;
    }

    public CancerTieringInterpretationAnalysis setStudyId(String studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getClinicalAnalysisId() {
        return clinicalAnalysisId;
    }

    public CancerTieringInterpretationAnalysis setClinicalAnalysisId(String clinicalAnalysisId) {
        this.clinicalAnalysisId = clinicalAnalysisId;
        return this;
    }

    public List<String> getVariantIdsToDiscard() {
        return variantIdsToDiscard;
    }

    public CancerTieringInterpretationAnalysis setVariantIdsToDiscard(List<String> variantIdsToDiscard) {
        this.variantIdsToDiscard = variantIdsToDiscard;
        return this;
    }

    public CancerTieringInterpretationConfiguration getConfig() {
        return config;
    }

    public CancerTieringInterpretationAnalysis setConfig(CancerTieringInterpretationConfiguration config) {
        this.config = config;
        return this;
    }
}