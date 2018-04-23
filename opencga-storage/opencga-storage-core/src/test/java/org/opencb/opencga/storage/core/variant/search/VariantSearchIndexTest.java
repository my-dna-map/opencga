package org.opencb.opencga.storage.core.variant.search;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.exceptions.VariantSearchException;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.core.variant.VariantStorageBaseTest;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.core.variant.search.solr.VariantSearchLoadResult;
import org.opencb.opencga.storage.core.variant.solr.SolrExternalResource;
import org.opencb.opencga.storage.core.variant.stats.DefaultVariantStatisticsManager;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created on 19/04/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
@Ignore
public abstract class VariantSearchIndexTest extends VariantStorageBaseTest {

    @Rule
    public SolrExternalResource solr = new SolrExternalResource();

    @Before
    public void setUp() throws Exception {
        solr.configure(variantStorageEngine);
    }

    @Test
    public void testIncrementalIndex() throws Exception {
        VariantDBAdaptor dbAdaptor = variantStorageEngine.getDBAdaptor();

        VariantStorageEngine storageEngine = getVariantStorageEngine();
        QueryOptions options = new QueryOptions();

        int maxStudies = 2;
        int studyId = 1;
        int release = 1;
        List<URI> inputFiles = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        StudyConfiguration studyConfiguration = new StudyConfiguration(studyId, "S_" + studyId);
        for (int fileId = 12877; fileId <= 12893; fileId++) {
            String fileName = "1K.end.platinum-genomes-vcf-NA" + fileId + "_S1.genome.vcf.gz";
            URI inputFile = getResourceUri("platinum/" + fileName);
            fileNames.add(fileName);
            inputFiles.add(inputFile);
            studyConfiguration.getFileIds().put(fileName, fileId);
            studyConfiguration.getSampleIds().put("NA" + fileId, fileId);
            if (inputFiles.size() == 4) {
                dbAdaptor.getStudyConfigurationManager().updateStudyConfiguration(studyConfiguration, null);
                options.put(VariantStorageEngine.Options.STUDY_ID.key(), studyId);
                storageEngine.getOptions().putAll(options);
                storageEngine.getOptions().put(VariantStorageEngine.Options.RELEASE.key(), release++);
                storageEngine.index(inputFiles.subList(0, 2), outputUri, true, true, true);

                Query query = new Query(VariantQueryParam.STUDY.key(), studyId);
                long count = dbAdaptor.count(query).first();

                VariantSearchLoadResult loadResult = searchIndex();
                System.out.println("Load result after load 1,2 files: = " + loadResult);
                assertEquals(count, loadResult.getNumLoadedVariants());
                checkVariantSearchIndex(dbAdaptor);

                //////////////////////
                storageEngine.getOptions().putAll(options);
                storageEngine.getOptions().put(VariantStorageEngine.Options.RELEASE.key(), release++);
                storageEngine.index(inputFiles.subList(2, 4), outputUri, true, true, true);

                count = dbAdaptor.count(query.append(VariantQueryParam.FILE.key(),
                        "!" + fileNames.get(0) + ";!" + fileNames.get(1) + ";" + fileNames.get(2) + ";" + fileNames.get(3))).first();
                count += dbAdaptor.count(query.append(VariantQueryParam.FILE.key(),
                        "!" + fileNames.get(0) + ";!" + fileNames.get(1) + ";!" + fileNames.get(2) + ";" + fileNames.get(3))).first();
                count += dbAdaptor.count(query.append(VariantQueryParam.FILE.key(),
                        "!" + fileNames.get(0) + ";!" + fileNames.get(1) + ";" + fileNames.get(2) + ";!" + fileNames.get(3))).first();

                loadResult = searchIndex();
                System.out.println("Load result after load 3,4 files: = " + loadResult);
                assertEquals(count, loadResult.getNumLoadedVariants());
                checkVariantSearchIndex(dbAdaptor);


                //////////////////////
                QueryOptions statsOptions = new QueryOptions(VariantStorageEngine.Options.STUDY_ID.key(), STUDY_ID)
                        .append(VariantStorageEngine.Options.LOAD_BATCH_SIZE.key(), 100)
                        .append(DefaultVariantStatisticsManager.OUTPUT, outputUri)
                        .append(DefaultVariantStatisticsManager.OUTPUT_FILE_NAME, "stats");
                storageEngine.calculateStats(studyConfiguration.getStudyName(), Collections.singletonList("ALL"), statsOptions);

                query = new Query(VariantQueryParam.STUDY.key(), studyId);
                count = dbAdaptor.count(query).first();
                loadResult = searchIndex();
                System.out.println("Load result after stats calculate: = " + loadResult);
                assertEquals(count, loadResult.getNumLoadedVariants());
                checkVariantSearchIndex(dbAdaptor);

                //////////////////////
                count = dbAdaptor.count(null).first();
                loadResult = searchIndex(true);
                System.out.println("Load result overwrite: = " + loadResult);
                assertEquals(count, loadResult.getNumLoadedVariants());
                checkVariantSearchIndex(dbAdaptor);

                /////////// NEW STUDY ///////////
                studyId++;
                studyConfiguration = new StudyConfiguration(studyId, "S_" + studyId);
                inputFiles.clear();
                fileNames.clear();
                if (studyId > maxStudies) {
                    break;
                }
            }
        }

        checkVariantSearchIndex(dbAdaptor);
    }

    public void checkVariantSearchIndex(VariantDBAdaptor dbAdaptor) throws IOException, VariantSearchException, StorageEngineException {
        QueryOptions queryOptions = new QueryOptions(QueryOptions.LIMIT, 1000);
        Query query = new Query();

        TreeSet<Variant> variantsFromSearch = new TreeSet<>(Comparator.comparing(Variant::toString));
        TreeSet<Variant> variantsFromDB = new TreeSet<>(Comparator.comparing(Variant::toString));

        variantsFromSearch.addAll(variantStorageEngine.getVariantSearchManager().query(DB_NAME, query, queryOptions).getResult());
        variantsFromDB.addAll(dbAdaptor.get(query, queryOptions).getResult());

        assertEquals(variantsFromDB.size(), variantsFromSearch.size());

        Iterator<Variant> variantsFromSearchIterator = variantsFromSearch.iterator();
        Iterator<Variant> variantsFromDBIterator = variantsFromDB.iterator();
        for (int i = 0; i < variantsFromDB.size(); i++) {
            Variant variantFromDB = variantsFromSearchIterator.next();
            Set<String> studiesFromDB = variantFromDB.getStudies().stream().map(StudyEntry::getStudyId).collect(Collectors.toSet());
            Variant variantFromSearch = variantsFromDBIterator.next();
            Set<String> studiesFromSearch = variantFromSearch.getStudies().stream().map(StudyEntry::getStudyId).collect(Collectors.toSet());

            assertEquals(variantFromDB.toString(), variantFromSearch.toString());
            assertEquals(variantFromDB.toString(), studiesFromDB, studiesFromSearch);
        }
    }

    public final VariantSearchLoadResult searchIndex() throws Exception {
        return searchIndex(false);
    }

    public VariantSearchLoadResult searchIndex(boolean overwrite) throws Exception {
        return variantStorageEngine.searchIndex(new Query(), new QueryOptions(), overwrite);
    }
}