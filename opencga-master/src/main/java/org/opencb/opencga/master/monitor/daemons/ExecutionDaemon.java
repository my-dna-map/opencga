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

package org.opencb.opencga.master.monitor.daemons;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.clinical.interpretation.CancerTieringInterpretationAnalysis;
import org.opencb.opencga.analysis.clinical.interpretation.CustomInterpretationAnalysis;
import org.opencb.opencga.analysis.clinical.interpretation.TeamInterpretationAnalysis;
import org.opencb.opencga.analysis.clinical.interpretation.TieringInterpretationAnalysis;
import org.opencb.opencga.analysis.file.FileDeleteAction;
import org.opencb.opencga.analysis.variant.gwas.GwasAnalysis;
import org.opencb.opencga.analysis.variant.operations.*;
import org.opencb.opencga.analysis.variant.stats.CohortVariantStatsAnalysis;
import org.opencb.opencga.analysis.variant.stats.SampleVariantStatsAnalysis;
import org.opencb.opencga.analysis.variant.stats.VariantStatsAnalysis;
import org.opencb.opencga.analysis.wrappers.*;
import org.opencb.opencga.catalog.db.api.DBIterator;
import org.opencb.opencga.catalog.db.api.JobDBAdaptor;
import org.opencb.opencga.catalog.db.api.ProjectDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.exceptions.CatalogIOException;
import org.opencb.opencga.catalog.io.CatalogIOManager;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.managers.FileManager;
import org.opencb.opencga.catalog.managers.JobManager;
import org.opencb.opencga.catalog.models.update.JobUpdateParams;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.Job;
import org.opencb.opencga.core.models.Study;
import org.opencb.opencga.core.models.acls.AclParams;
import org.opencb.opencga.core.models.acls.permissions.FileAclEntry;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.results.OpenCGAResult;
import org.opencb.opencga.core.tools.result.ExecutionResult;
import org.opencb.opencga.core.tools.result.ExecutorResultManager;
import org.opencb.opencga.core.tools.result.Status;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by imedina on 16/06/16.
 */
public class ExecutionDaemon extends MonitorParentDaemon {

    public static final String OUTDIR_PARAM = "outdir";
    private String internalCli;
    private JobManager jobManager;
    private FileManager fileManager;
    private CatalogIOManager catalogIOManager;
    private final Map<String, Long> jobsCountByType = new HashMap<>();
    private final Map<String, Long> retainedLogsTime = new HashMap<>();

    private Path defaultJobDir;

    private static final Map<String, String> TOOL_CLI_MAP;

    // Maximum number of jobs of each type (Pending, queued, running) that will be handled on each iteration.
    // Example: If there are 100 pending jobs, 15 queued, 70 running.
    // On first iteration, it will queue 50 out of the 100 pending jobs. It will check up to 50 queue-running changes out of the 65
    // (15 + 50 from pending), and it will check up to 50 finished jobs from the running ones.
    // On second iteration, it will queue the remaining 50 pending jobs, and so on...
    private static final int NUM_JOBS_HANDLED = 50;
    private final Query pendingJobsQuery;
    private final Query queuedJobsQuery;
    private final Query runningJobsQuery;
    private final QueryOptions queryOptions;

    static {
        TOOL_CLI_MAP = new HashMap<String, String>(){{
            put("files-unlink", "files unlink");
            put(FileDeleteAction.ID, "files delete");

            put("alignment-index", "alignment index");
            put("alignment-coverage-run", "alignment coverage-run");
            put("alignment-stats-run", "alignment stats-run");
            put(BwaWrapperAnalysis.ID, "alignment " + BwaWrapperAnalysis.ID);
            put(SamtoolsWrapperAnalysis.ID, "alignment " + SamtoolsWrapperAnalysis.ID);
            put(DeeptoolsWrapperAnalysis.ID, "alignment " + DeeptoolsWrapperAnalysis.ID);

            put(VariantFileIndexerStorageOperation.ID, "variant index");
            put(VariantExportStorageOperation.ID, "variant export");
            put(VariantStatsAnalysis.ID, "variant stats-run");
            put("variant-stats-export", "variant stats-export");
            put(SampleVariantStatsAnalysis.ID, "variant sample-stats-run");
            put(CohortVariantStatsAnalysis.ID, "variant cohort-stats-run");
            put(GwasAnalysis.ID, "variant gwas-run");
            put(PlinkWrapperAnalysis.ID, "variant " + PlinkWrapperAnalysis.ID + "-run");
            put(RvtestsWrapperAnalysis.ID, "variant " + RvtestsWrapperAnalysis.ID + "-run");
            put(VariantRemoveStorageOperation.ID, "variant delete");
            put(VariantSecondaryIndexStorageOperation.ID, "variant secondary-index");
            put(VariantSecondaryIndexSamplesDeleteStorageOperation.ID, "variant secondary-index-delete");
            put("variant-score-delete", "variant score-delete");
            put("variant-score-index", "variant score-index");
            put(VariantSampleIndexStorageOperation.ID, "variant sample-index");
            put(VariantFamilyIndexStorageOperation.ID, "variant family-index");
            put(VariantAggregateFamilyStorageOperation.ID, "variant aggregate-family");
            put(VariantAggregateStorageOperation.ID, "variant aggregate");
            put(VariantAnnotationStorageOperation.ID, "variant annotation-index");
            put(VariantAnnotationDeleteStorageOperation.ID, "variant annotation-delete");
            put(VariantAnnotationSaveStorageOperation.ID, "variant annotation-save");

            put(TeamInterpretationAnalysis.ID, "interpretation " + TeamInterpretationAnalysis.ID);
            put(TieringInterpretationAnalysis.ID, "interpretation " + TieringInterpretationAnalysis.ID);
            put(CustomInterpretationAnalysis.ID, "interpretation " + CustomInterpretationAnalysis.ID);
            put(CancerTieringInterpretationAnalysis.ID, "interpretation " + CancerTieringInterpretationAnalysis.ID);
        }};
    }

    public ExecutionDaemon(int interval, String token, CatalogManager catalogManager, String appHome)
            throws CatalogDBException, CatalogIOException {
        super(interval, token, catalogManager);

        this.jobManager = catalogManager.getJobManager();
        this.fileManager = catalogManager.getFileManager();
        this.catalogIOManager = catalogManager.getCatalogIOManagerFactory().get("file");
        this.internalCli = appHome + "/bin/opencga-internal.sh";

        this.defaultJobDir = Paths.get(catalogManager.getConfiguration().getJobDir());

        pendingJobsQuery = new Query(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Enums.ExecutionStatus.PENDING);
        queuedJobsQuery = new Query(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Enums.ExecutionStatus.QUEUED);
        runningJobsQuery = new Query(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Enums.ExecutionStatus.RUNNING);
        // Sort jobs by priority and creation date
        queryOptions = new QueryOptions()
                .append(QueryOptions.SORT, Arrays.asList(JobDBAdaptor.QueryParams.PRIORITY.key(),
                        JobDBAdaptor.QueryParams.CREATION_DATE.key()))
                .append(QueryOptions.ORDER, QueryOptions.ASCENDING);
    }

    @Override
    public void run() {

        while (!exit) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                if (!exit) {
                    e.printStackTrace();
                }
            }

            try {
                checkJobs();
            } catch (Exception e) {
                logger.error("Catch exception " + e.getMessage(), e);
            }
        }
    }

    protected void checkJobs() {
        long pendingJobs = -1;
        long queuedJobs = -1;
        long runningJobs = -1;
        try {
            pendingJobs = jobManager.count(pendingJobsQuery, token).getNumMatches();
            queuedJobs = jobManager.count(queuedJobsQuery, token).getNumMatches();
            runningJobs = jobManager.count(runningJobsQuery, token).getNumMatches();
        } catch (CatalogException e) {
            logger.error("{}", e.getMessage(), e);
        }
        logger.info("----- EXECUTION DAEMON  ----- pending={}, queued={}, running={}", pendingJobs, queuedJobs, runningJobs);

            /*
            PENDING JOBS
             */
        checkPendingJobs();

            /*
            QUEUED JOBS
             */
        checkQueuedJobs();

            /*
            RUNNING JOBS
             */
        checkRunningJobs();
    }

    protected void checkRunningJobs() {
        int handledRunningJobs = 0;
        try (DBIterator<Job> iterator = jobManager.iterator(runningJobsQuery, queryOptions, token)) {
            while (handledRunningJobs < NUM_JOBS_HANDLED && iterator.hasNext()) {
                Job job = iterator.next();
                handledRunningJobs += checkRunningJob(job);
            }
        } catch (CatalogException e) {
            logger.error("{}", e.getMessage(), e);
        }
    }

    protected int checkRunningJob(Job job) {
        Enums.ExecutionStatus jobStatus = getCurrentStatus(job);

        switch (jobStatus.getName()) {
            case Enums.ExecutionStatus.RUNNING:
                ExecutionResult result = readAnalysisResult(job);
                if (result != null) {
                    // Update the result of the job
                    JobUpdateParams updateParams = new JobUpdateParams().setResult(result);
                    String study = String.valueOf(job.getAttributes().get(Job.OPENCGA_STUDY));
                    try {
                        jobManager.update(study, job.getId(), updateParams, QueryOptions.empty(), token);
                    } catch (CatalogException e) {
                        logger.error("{} - Could not update result information: {}", job.getId(), e.getMessage(), e);
                        return 0;
                    }
                }
                return 1;
            case Enums.ExecutionStatus.ABORTED:
            case Enums.ExecutionStatus.ERROR:
            case Enums.ExecutionStatus.DONE:
            case Enums.ExecutionStatus.READY:
                // Register job results
                return processFinishedJob(job);
            case Enums.ExecutionStatus.QUEUED:
                // Running job went back to Queued?
                logger.info("Running job '{}' went back to '{}' status", job.getId(), jobStatus.getName());
                return setStatus(job, new Enums.ExecutionStatus(Enums.ExecutionStatus.QUEUED));
            case Enums.ExecutionStatus.PENDING:
            case Enums.ExecutionStatus.UNKNOWN:
            default:
                logger.info("Unexpected status '{}' for job '{}'", jobStatus.getName(), job.getId());
                return 0;

        }
    }

    protected void checkQueuedJobs() {
        int handledQueuedJobs = 0;
        try (DBIterator<Job> iterator = jobManager.iterator(queuedJobsQuery, queryOptions, token)) {
            while (handledQueuedJobs < NUM_JOBS_HANDLED && iterator.hasNext()) {
                Job job = iterator.next();
                handledQueuedJobs += checkQueuedJob(job);
            }
        } catch (CatalogException e) {
            logger.error("{}", e.getMessage(), e);
        }
    }

    /**
     * Check if the job is still queued or it has changed to running or error.
     *
     * @param job Job object.
     * @return 1 if the job has changed the status, 0 otherwise.
     */
    protected int checkQueuedJob(Job job) {
        Enums.ExecutionStatus status = getCurrentStatus(job);

        switch (status.getName()) {
            case Enums.ExecutionStatus.QUEUED:
                // Job is still queued
                return 0;
            case Enums.ExecutionStatus.RUNNING:
                logger.info("Updating job {} from {} to {}", job.getId(), Enums.ExecutionStatus.QUEUED, Enums.ExecutionStatus.RUNNING);
                return setStatus(job, new Enums.ExecutionStatus(Enums.ExecutionStatus.RUNNING));
            case Enums.ExecutionStatus.ABORTED:
            case Enums.ExecutionStatus.ERROR:
            case Enums.ExecutionStatus.DONE:
            case Enums.ExecutionStatus.READY:
                // Job has finished the execution, so we need to register the job results
                return processFinishedJob(job);
            case Enums.ExecutionStatus.UNKNOWN:
                logger.info("Job '{}' in status {}", job.getId(), Enums.ExecutionStatus.UNKNOWN);
                return 0;
            default:
                logger.info("Unexpected status '{}' for job '{}'", status.getName(), job.getId());
                return 0;
        }
    }

    protected void checkPendingJobs() {
        // Clear job counts each cycle
        jobsCountByType.clear();

        int handledPendingJobs = 0;
        try (DBIterator<Job> iterator = jobManager.iterator(pendingJobsQuery, queryOptions, token)) {
            while (handledPendingJobs < NUM_JOBS_HANDLED && iterator.hasNext()) {
                Job job = iterator.next();
                handledPendingJobs += checkPendingJob(job);
            }
        } catch (CatalogException e) {
            logger.error("{}", e.getMessage(), e);
        }
    }

    /**
     * Check everything is correct and queues the job.
     *
     * @param job Job object.
     * @return 1 if the job has changed the status, 0 otherwise.
     */
    protected int checkPendingJob(Job job) {
        String study = String.valueOf(job.getAttributes().get(Job.OPENCGA_STUDY));
        if (StringUtils.isEmpty(study)) {
            return abortJob(job, "Missing mandatory '" + Job.OPENCGA_STUDY + "' field");
        }

        if (StringUtils.isEmpty(job.getToolId()) || !TOOL_CLI_MAP.containsKey(job.getToolId())) {
            return abortJob(job, "Tool id '" + job.getToolId() + "' not found.");
        }

        if (!canBeQueued(job)) {
            return 0;
        }

        String userToken;
        try {
            userToken = catalogManager.getUserManager().getSystemTokenForUser(job.getUserId(), token);
        } catch (CatalogException e) {
            return abortJob(job, "Internal error. Could not obtain token for user '" + job.getUserId() + "'");
        }

        JobUpdateParams updateParams = new JobUpdateParams();

        Map<String, Object> params = job.getParams();
        String outDirPathParam = (String) params.get(OUTDIR_PARAM);
        if (!StringUtils.isEmpty(outDirPathParam)) {
            try {
                // Any path the user has requested
                updateParams.setOutDir(getValidInternalOutDir(study, job, outDirPathParam, userToken));
            } catch (CatalogException e) {
                logger.error("Cannot create output directory. {}", e.getMessage(), e);
                return abortJob(job, "Cannot create output directory. " + e.getMessage());
            }
        } else {
            try {
                // JOBS/user/job_id/
                updateParams.setOutDir(getValidDefaultOutDir(study, job, userToken));
            } catch (CatalogException e) {
                logger.error("Cannot create output directory. {}", e.getMessage(), e);
                return abortJob(job, "Cannot create output directory. " + e.getMessage());
            }
        }

        Path outDirPath = Paths.get(updateParams.getOutDir().getUri());
        params.put(OUTDIR_PARAM, outDirPath.toAbsolutePath().toString());

        // Define where the stdout and stderr will be stored
        Path stderr = outDirPath.resolve(getErrorLogFileName(job));
        Path stdout = outDirPath.resolve(getLogFileName(job));

        // Create cli
        String commandLine = buildCli(internalCli, job.getToolId(), params);
        String authenticatedCommandLine = commandLine + " --token " + userToken;
        String shadedCommandLine = commandLine + " --token xxxxxxxxxxxxxxxxxxxxx";

        updateParams.setCommandLine(shadedCommandLine);

        logger.info("Updating job {} from {} to {}", job.getId(), Enums.ExecutionStatus.PENDING, Enums.ExecutionStatus.QUEUED);
        updateParams.setStatus(new Enums.ExecutionStatus(Enums.ExecutionStatus.QUEUED));
        try {
            jobManager.update(study, job.getId(), updateParams, QueryOptions.empty(), token);
        } catch (CatalogException e) {
            logger.error("Could not update job {}. {}", job.getId(), e.getMessage(), e);
            return 0;
        }

        try {
            batchExecutor.execute(job.getId(), authenticatedCommandLine, stdout, stderr);
        } catch (Exception e) {
            logger.error("Error executing job {}.", job.getId(), e);
            return abortJob(job, "Error executing job. " + e.getMessage());
        }
        return 1;
    }

    private File getValidInternalOutDir(String study, Job job, String outDirPath, String userToken) throws CatalogException {
        // TODO: Remove this line when we stop passing the outdir as a query param in the URL
        outDirPath = outDirPath.replace(":", "/");
        if (!outDirPath.endsWith("/")) {
            outDirPath += "/";
        }
        File outDir;
        try {
            outDir = fileManager.get(study, outDirPath, FileManager.INCLUDE_FILE_URI_PATH, token).first();
        } catch (CatalogException e) {
            // Directory not found. Will try to create using user's token
            boolean parents = (boolean) job.getAttributes().getOrDefault(Job.OPENCGA_PARENTS, false);
            try {
                outDir = fileManager.createFolder(study, outDirPath, new File.FileStatus(), parents, "", FileManager.INCLUDE_FILE_URI_PATH,
                        userToken).first();
                CatalogIOManager ioManager = catalogManager.getCatalogIOManagerFactory().get(outDir.getUri());
                ioManager.createDirectory(outDir.getUri(), true);
            } catch (CatalogException e1) {
                throw new CatalogException("Cannot create output directory. " + e1.getMessage(), e1.getCause());
            }
        }

        // Ensure the directory is empty
        CatalogIOManager ioManager = catalogManager.getCatalogIOManagerFactory().get(outDir.getUri());
        if (!ioManager.isDirectory(outDir.getUri())) {
            throw new CatalogException(OUTDIR_PARAM + " seems not to be a directory");
        }
        if (!ioManager.listFiles(outDir.getUri()).isEmpty()) {
            throw new CatalogException(OUTDIR_PARAM + " " + outDirPath + " is not an empty directory");
        }

        return outDir;
    }

    private File getValidDefaultOutDir(String studyStr, Job job, String userToken) throws CatalogException {
        OpenCGAResult<File> fileOpenCGAResult;
        try {
            fileOpenCGAResult = fileManager.get(studyStr, "JOBS/", FileManager.INCLUDE_FILE_URI_PATH, token);
        } catch (CatalogException e) {
            logger.info("JOBS/ directory does not exist, registering for the first time");

            // Create main JOBS directory for the study
            Study study = catalogManager.getStudyManager().resolveId(studyStr, "admin");
            long projectUid = catalogManager.getProjectManager().get(study.getFqn().split(":")[0], new QueryOptions(QueryOptions.INCLUDE,
                    ProjectDBAdaptor.QueryParams.UID.key()), token).first().getUid();

            URI uri = Paths.get(catalogManager.getConfiguration().getJobDir())
                    .resolve(study.getFqn().split("@")[0]) // user
                    .resolve(Long.toString(projectUid))
                    .resolve(Long.toString(study.getUid()))
                    .resolve("JOBS")
                    .toUri();

            // Create the directory in the file system
            catalogIOManager.createDirectory(uri, true);
            // And link it to OpenCGA
            fileOpenCGAResult = fileManager.link(studyStr, uri, "/", new ObjectMap("parents", true), token);
        }

        // Check we can write in the folder
        catalogIOManager.checkWritableUri(fileOpenCGAResult.first().getUri());

        // Check if the default jobOutDirPath of the user already exists
        OpenCGAResult<File> result;
        try {
            result = fileManager.get(studyStr, "JOBS/" + job.getUserId() + "/", FileManager.INCLUDE_FILE_URI_PATH, userToken);
        } catch (CatalogException e) {
            // We first need to create the main directory that will contain all the jobs of the user
            result = fileManager.createFolder(studyStr, "JOBS/" + job.getUserId() + "/", new File.FileStatus(), false,
                    "Directory containing the jobs of " + job.getUserId(), FileManager.INCLUDE_FILE_URI_PATH, token);

            // Add permissions to do anything under that path to the user launching the job
            String allFilePermissions = EnumSet.allOf(FileAclEntry.FilePermissions.class)
                    .stream()
                    .map(FileAclEntry.FilePermissions::toString)
                    .collect(Collectors.joining(","));
            fileManager.updateAcl(studyStr, Collections.singletonList("JOBS/" + job.getUserId() + "/"), job.getUserId(),
                    new File.FileAclParams(allFilePermissions, AclParams.Action.SET, null), token);
            // Remove permissions to any other user that is not the one launching the job
            fileManager.updateAcl(studyStr, Collections.singletonList("JOBS/" + job.getUserId() + "/"), FileAclEntry.USER_OTHERS_ID,
                    new File.FileAclParams("", AclParams.Action.SET, null), token);
        }

        // Now we create a new directory where the job will be actually executed
        File userFolder = result.first();

        File outDirFile = fileManager.createFolder(studyStr, userFolder.getPath() + job.getId(), new File.FileStatus(), false,
                "Directory containing the results of the execution of job " + job.getId(), FileManager.INCLUDE_FILE_URI_PATH, token)
                .first();

        // Create the physical directories in disk
        try {
            catalogIOManager.createDirectory(outDirFile.getUri(), true);
        } catch (CatalogIOException e) {
            throw new CatalogException("Cannot create job directories '" + outDirFile.getUri() + "' for path '" + outDirFile.getPath()
                    + "'");
        }

        return outDirFile;
    }

    public static String buildCli(String internalCli, String toolId, Map<String, Object> params) {
        StringBuilder cliBuilder = new StringBuilder()
                .append(internalCli)
                .append(" ").append(TOOL_CLI_MAP.get(toolId));
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, String> dynamicParams = (Map<String, String>) entry.getValue();
                for (Map.Entry<String, String> dynamicEntry : dynamicParams.entrySet()) {
                    cliBuilder
                            .append(" ").append("-D").append(dynamicEntry.getKey())
                            .append("=").append(dynamicEntry.getValue());
                }
            } else {
                cliBuilder
                        .append(" --").append(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, entry.getKey()))
                        .append(" ").append(entry.getValue());
            }
        }
        return cliBuilder.toString();
    }

    private boolean canBeQueued(Job job) {
        if ("variant-index".equals(job.getToolId())) {
            int maxIndexJobs = catalogManager.getConfiguration().getAnalysis().getIndex().getVariant().getMaxConcurrentJobs();
            return canBeQueued("variant-index", maxIndexJobs);
        }
        return true;
    }

    private boolean canBeQueued(String toolId, int maxJobs) {
        Query query = new Query()
                .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Enums.ExecutionStatus.QUEUED + "," + Enums.ExecutionStatus.RUNNING)
                .append(JobDBAdaptor.QueryParams.TOOL_ID.key(), toolId);
        long currentJobs = jobsCountByType.computeIfAbsent(toolId, k -> {
            try {
                return catalogManager.getJobManager().count(query, token).getNumMatches();
            } catch (CatalogException e) {
                logger.error("Error counting the current number of running and queued \"" + toolId + "\" jobs", e);
                return 0L;
            }
        });
        if (currentJobs >= maxJobs) {
            long now = System.currentTimeMillis();
            Long lastTimeLog = retainedLogsTime.getOrDefault(toolId, 0L);
            if (now - lastTimeLog > 60000) {
                logger.info("There are {} " + toolId + " jobs running or queued already. "
                        + "Current limit is {}. "
                        + "Halt new " + toolId + " jobs.", currentJobs, maxJobs);
                retainedLogsTime.put(toolId, now);
            }
            return false;
        } else {
            jobsCountByType.remove(toolId);
            retainedLogsTime.put(toolId, 0L);
            return true;
        }
    }

    private int abortJob(Job job, String description) {
        logger.info("Aborting job: {} - Reason: '{}'", job.getId(), description);
        return setStatus(job, new Enums.ExecutionStatus(Enums.ExecutionStatus.ABORTED, description));
    }

    private int setStatus(Job job, Enums.ExecutionStatus status) {
        JobUpdateParams updateParams = new JobUpdateParams().setStatus(status);

        String study = String.valueOf(job.getAttributes().get(Job.OPENCGA_STUDY));
        if (StringUtils.isEmpty(study)) {
            try {
                study = jobManager.getStudy(job, token).getFqn();
            } catch (CatalogException e) {
                logger.error("Unexpected error. Unknown study of job '{}'. {}", job.getId(), e.getMessage(), e);
                return 0;
            }
        }

        try {
            jobManager.update(study, job.getId(), updateParams, QueryOptions.empty(), token);
        } catch (CatalogException e) {
            logger.error("Unexpected error. Cannot update job '{}' to status '{}'. {}", job.getId(), updateParams.getStatus().getName(),
                    e.getMessage(), e);
            return 0;
        }

        return 1;
    }

    private Enums.ExecutionStatus getCurrentStatus(Job job) {

        Path resultJson = getAnalysisResultPath(job);

        // Check if analysis result file is there
        if (resultJson != null && Files.exists(resultJson)) {
            ExecutionResult execution = readAnalysisResult(resultJson);
            if (execution != null) {
                return new Enums.ExecutionStatus(execution.getStatus().getName().name());
            } else {
                if (Files.exists(resultJson)) {
                    logger.warn("File '" + resultJson + "' seems corrupted.");
                } else {
                    logger.warn("Could not find file '" + resultJson + "'.");
                }
            }
        }

        String status = batchExecutor.getStatus(job.getId());
        if (!StringUtils.isEmpty(status) && !status.equals(Enums.ExecutionStatus.UNKNOWN)) {
            return new Enums.ExecutionStatus(status);
        } else {
            Path tmpOutdirPath = Paths.get(job.getOutDir().getUri());
            // Check if the error file is present
            Path errorLog = tmpOutdirPath.resolve(getErrorLogFileName(job));

            if (Files.exists(errorLog)) {
                // FIXME: This may not be true. There is a delay between job starts (i.e. error log appears) and
                //  the analysis result creation

                // There must be some command line error. The job started running but did not finish well, otherwise we would find the
                // analysis-result.yml file
                return new Enums.ExecutionStatus(Enums.ExecutionStatus.ERROR, "Command line error");
            } else {
                return new Enums.ExecutionStatus(Enums.ExecutionStatus.QUEUED);
            }
        }

    }

    private Path getAnalysisResultPath(Job job) {
        Path resultJson = null;
        try (Stream<Path> stream = Files.list(Paths.get(job.getOutDir().getUri()))) {
            resultJson = stream
                    .filter(path -> {
                        String str = path.toString();
                        return str.endsWith(ExecutorResultManager.FILE_EXTENSION)
                                && !str.endsWith(ExecutorResultManager.SWAP_FILE_EXTENSION);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            logger.warn("Could not find AnalysisResult file", e);
        }
        return resultJson;
    }

    private ExecutionResult readAnalysisResult(Job job) {
        Path resultJson = getAnalysisResultPath(job);
        if (resultJson != null) {
            return readAnalysisResult(resultJson);
        }
        return null;
    }

    private ExecutionResult readAnalysisResult(Path file) {
        if (file == null) {
            return null;
        }
        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                try (InputStream is = new BufferedInputStream(new FileInputStream(file.toFile()))) {
                    return JacksonUtils.getDefaultObjectMapper().readValue(is, ExecutionResult.class);
                }
            } catch (IOException e) {
                if (attempts == maxAttempts) {
                    logger.error("Could not load AnalysisResult file: " + file.toAbsolutePath(), e);
                } else {
                    logger.warn("Could not load AnalysisResult file: " + file.toAbsolutePath()
                            + ". Retry " + attempts + "/" + maxAttempts
                            + ". " + e.getMessage()
                    );
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException interruption) {
                        // Ignore interruption
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return null;
    }

    private int processFinishedJob(Job job) {
        logger.info("{} - Processing finished job...", job.getId());

        Path outDirUri = Paths.get(job.getOutDir().getUri());
        Path analysisResultPath = getAnalysisResultPath(job);

        String study = String.valueOf(job.getAttributes().get(Job.OPENCGA_STUDY));

        logger.info("{} - Registering job results from '{}'", job.getId(), outDirUri);

        ExecutionResult execution;
        if (analysisResultPath != null) {
            execution = readAnalysisResult(analysisResultPath);
            if (execution != null) {
                JobUpdateParams updateParams = new JobUpdateParams().setResult(execution);
                try {
                    jobManager.update(study, job.getId(), updateParams, QueryOptions.empty(), token);
                } catch (CatalogException e) {
                    logger.error("{} - Catastrophic error. Could not update job information with final result {}: {}", job.getId(),
                            updateParams.toString(), e.getMessage(), e);
                    return 0;
                }
            }
        } else {
            execution = null;
        }

        List<File> registeredFiles;
        try {
            Predicate<URI> uriPredicate = uri -> !uri.getPath().endsWith(ExecutorResultManager.FILE_EXTENSION)
                    && !uri.getPath().endsWith(ExecutorResultManager.SWAP_FILE_EXTENSION)
                    && !uri.getPath().contains("/scratch_");
            registeredFiles = fileManager.syncUntrackedFiles(study, job.getOutDir().getPath(), uriPredicate, token).getResults();
        } catch (CatalogException e) {
            logger.error("Could not registered files in Catalog: {}", e.getMessage(), e);
            return 0;
        }

        // Register the job information
        JobUpdateParams updateParams = new JobUpdateParams();

        // Process output and log files
        List<File> outputFiles = new ArrayList<>(registeredFiles.size());
        String logFileName = getLogFileName(job);
        String errorLogFileName = getErrorLogFileName(job);
        for (File registeredFile : registeredFiles) {
            if (registeredFile.getName().equals(logFileName)) {
                updateParams.setLog(registeredFile);
            } else if (registeredFile.getName().equals(errorLogFileName)) {
                updateParams.setErrorLog(registeredFile);
            } else {
                outputFiles.add(registeredFile);
            }
        }
        updateParams.setOutput(outputFiles);


        // Check status of analysis result or if there are files that could not be moved to outdir to decide the final result
        if (execution == null) {
            updateParams.setStatus(new Enums.ExecutionStatus(Enums.ExecutionStatus.ERROR, "Job could not finish successfully. "
                    + "Missing analysis result"));
        } else if (execution.getStatus().getName().equals(Status.Type.ERROR)) {
            updateParams.setStatus(new Enums.ExecutionStatus(Enums.ExecutionStatus.ERROR, "Job could not finish successfully"));
        } else {
            updateParams.setStatus(new Enums.ExecutionStatus(Enums.ExecutionStatus.DONE));
        }

        logger.info("{} - Updating job information", job.getId());
        // We update the job information
        try {
            jobManager.update(study, job.getId(), updateParams, QueryOptions.empty(), token);
        } catch (CatalogException e) {
            logger.error("{} - Catastrophic error. Could not update job information with final result {}: {}", job.getId(),
                    updateParams.toString(), e.getMessage(), e);
            return 0;
        }

        return 1;
    }

    private String getErrorLogFileName(Job job) {
        return job.getId() + ".err";
    }

    private String getLogFileName(Job job) {
        return job.getId() + ".log";
    }

}
