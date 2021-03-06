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

package org.opencb.opencga.storage.app.cli.client.options;

import com.beust.jcommander.*;
import com.beust.jcommander.converters.CommaParameterSplitter;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.metadata.Aggregation;
import org.opencb.opencga.storage.app.cli.GeneralCliOptions;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;
import org.opencb.opencga.storage.core.variant.VariantStorageOptions;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils;
import org.opencb.opencga.storage.core.variant.annotation.VariantAnnotationManager;
import org.opencb.opencga.storage.core.variant.annotation.annotators.VariantAnnotatorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencb.opencga.storage.app.cli.client.options.StorageVariantCommandOptions.GenericAnnotationCommandOptions.ANNOTATION_ID_DESCRIPTION;
import static org.opencb.opencga.storage.app.cli.client.options.StorageVariantCommandOptions.GenericAnnotationCommandOptions.ANNOTATION_ID_PARAM;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.*;

/**
 * Created by imedina on 22/01/17.
 */
@Parameters(commandNames = {"variant"}, commandDescription = "Variant management.")
public class StorageVariantCommandOptions {

    protected static final String DEPRECATED = "[DEPRECATED] ";
    public final VariantIndexCommandOptions indexVariantsCommandOptions;
    public final VariantDeleteCommandOptions variantDeleteCommandOptions;
    public final VariantQueryCommandOptions variantQueryCommandOptions;
    public final ImportVariantsCommandOptions importVariantsCommandOptions;
    public final VariantAnnotateCommandOptions annotateVariantsCommandOptions;
    public final AnnotationSaveCommandOptions annotationSaveCommandOptions;
    public final AnnotationDeleteCommandOptions annotationDeleteCommandOptions;
    public final AnnotationQueryCommandOptions annotationQueryCommandOptions;
    public final AnnotationMetadataCommandOptions annotationMetadataCommandOptions;
    public final VariantStatsCommandOptions statsVariantsCommandOptions;
    public final AggregateFamilyCommandOptions fillGapsCommandOptions;
    public final AggregateCommandOptions fillMissingCommandOptions;
    public final VariantExportCommandOptions exportVariantsCommandOptions;
    public final VariantSearchCommandOptions searchVariantsCommandOptions;

    public final JCommander jCommander;
    public final GeneralCliOptions.CommonOptions commonCommandOptions;
    public final GeneralCliOptions.IndexCommandOptions indexCommandOptions;
    public final GeneralCliOptions.QueryCommandOptions queryCommandOptions;

    public StorageVariantCommandOptions(GeneralCliOptions.CommonOptions commonOptions, GeneralCliOptions.IndexCommandOptions indexCommandOptions,
                                        GeneralCliOptions.QueryCommandOptions queryCommandOptions, JCommander jCommander) {
        this.commonCommandOptions = commonOptions;
        this.indexCommandOptions  = indexCommandOptions;
        this.queryCommandOptions = queryCommandOptions;
        this.jCommander = jCommander;

        this.indexVariantsCommandOptions = new VariantIndexCommandOptions();
        this.variantDeleteCommandOptions = new VariantDeleteCommandOptions();
        this.variantQueryCommandOptions = new VariantQueryCommandOptions();
        this.importVariantsCommandOptions = new ImportVariantsCommandOptions();
        this.annotateVariantsCommandOptions = new VariantAnnotateCommandOptions();
        this.annotationSaveCommandOptions = new AnnotationSaveCommandOptions();
        this.annotationDeleteCommandOptions = new AnnotationDeleteCommandOptions();
        this.annotationQueryCommandOptions = new AnnotationQueryCommandOptions();
        this.annotationMetadataCommandOptions = new AnnotationMetadataCommandOptions();
        this.statsVariantsCommandOptions = new VariantStatsCommandOptions();
        this.fillGapsCommandOptions = new AggregateFamilyCommandOptions();
        this.fillMissingCommandOptions = new AggregateCommandOptions();
        this.exportVariantsCommandOptions = new VariantExportCommandOptions();
        this.searchVariantsCommandOptions = new VariantSearchCommandOptions();
    }

    /**
     *  index: generic and specific options
     */
    public static class GenericVariantIndexOptions {

        @Parameter(names = {"--transform"}, description = "If present it only runs the transform stage, no load is executed")
        public boolean transform;

        @Parameter(names = {"--load"}, description = "If present only the load stage is executed, transformation is skipped")
        public boolean load;

        @Parameter(names = {"--merge"}, description = "Currently two levels of merge are supported: \"basic\" mode merge genotypes of the same variants while \"advanced\" merge multiallelic and overlapping variants.")
        public VariantStorageEngine.MergeMode merge = VariantStorageOptions.MERGE_MODE.defaultValue();

        @Parameter(names = {"--exclude-genotypes"}, description = "Index excluding the genotype information")
        public boolean excludeGenotype;

        @Parameter(names = {"--include-extra-fields"}, description = "Index including other FORMAT fields." +
                " Use \"" + VariantQueryUtils.ALL + "\", \"" + VariantQueryUtils.NONE + "\", or CSV with the fields to load.")
        public String includeExtraFields = VariantQueryUtils.ALL;

        @Parameter(names = {"--aggregated"}, description = "Select the type of aggregated VCF file: none, basic, EVS or ExAC", arity = 1)
        public Aggregation aggregated = Aggregation.NONE;

        @Parameter(names = {"--aggregation-mapping-file"}, description = "File containing population names mapping in an aggregated VCF file")
        public String aggregationMappingFile;

        @Parameter(names = {"--gvcf"}, description = "The input file is in gvcf format")
        public boolean gvcf;

//        @Parameter(names = {"--bgzip"}, description = "[PENDING] The input file is in bgzip format")
//        public boolean bgzip;

        @Parameter(names = {"--calculate-stats"}, description = "Calculate indexed variants statistics after the load step")
        public boolean calculateStats;

        @Parameter(names = {"--annotate"}, description = "Annotate indexed variants after the load step")
        public boolean annotate;

        @Parameter(names = {"--index-search"}, description = "Add files to the secondary search index")
        public boolean indexSearch;

        @Parameter(names = {"--annotator"}, description = "Annotation source {cellbase_rest, cellbase_db_adaptor}", arity = 1)
        public VariantAnnotatorFactory.AnnotationEngine annotator;

        @Parameter(names = {"--overwrite-annotations"}, description = "Overwrite annotations in variants already present")
        public boolean overwriteAnnotations;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed indexation")
        public boolean resume;

        @Parameter(names = {"--load-split-data"}, description = "Indicate that the variants from a sample (or group of samples) split into different files (by chromosome, by type, ...)")
        public boolean loadSplitData;

        @Parameter(names = {"--skip-post-load-check"}, description = "Do not execute post load checks over the database")
        public boolean skipPostLoadCheck;
    }

    @Parameters(commandNames = {"index"}, commandDescription = "Index variants file")
    public class VariantIndexCommandOptions extends GenericVariantIndexOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public GeneralCliOptions.IndexCommandOptions commonIndexOptions = indexCommandOptions;


        @Parameter(names = {"-s", "--study"}, description = "Full name of the study where the file is classified", arity = 1)
        public String study;

        @Parameter(names = {"--stdin"}, description = "Read the variants file from the standard input")
        public boolean stdin;

        @Parameter(names = {"--stdout"}, description = "Write the transformed variants file to the standard output")
        public boolean stdout;

//        @Deprecated
//        @Parameter(names = {"-p", "--pedigree"}, description = "File containing pedigree information (in PED format, optional)", arity = 1)
//        public String pedigree;

    }

    public static class GenericVariantDeleteOptions {

        @Parameter(names = {"--file"}, description = "CSV of files to be removed from storage. Type 'all' to remove the whole study",
                splitter = CommaParameterSplitter.class, required = true)
        public List<String> files = null;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed indexation")
        public boolean resume;
    }

    @Parameters(commandNames = {VariantDeleteCommandOptions.VARIANT_DELETE_COMMAND}, commandDescription = VariantDeleteCommandOptions.VARIANT_DELETE_COMMAND_DESCRIPTION)
    public class VariantDeleteCommandOptions extends GenericVariantDeleteOptions {

        public static final String VARIANT_DELETE_COMMAND = "delete";
        public static final String VARIANT_DELETE_COMMAND_DESCRIPTION = "Remove variants from storage";

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-s", "--study"}, description = "Full name of the study where the file is classified", arity = 1, required = true)
        public String study;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name to load the data", arity = 1)
        public String dbName;

    }

    /**
     *  query: basic, generic and specific options
     *
     * @see org.opencb.opencga.storage.app.cli.client.executors.VariantQueryCommandUtils#parseBasicVariantQuery
     */
    public static class BasicVariantQueryOptions {

        @Parameter(names = {"--id"}, description = ID_DESCR, variableArity = true)
        public List<String> id;

        @Parameter(names = {"-r", "--region"}, description = REGION_DESCR)
        public String region;

        @Parameter(names = {"--region-file"}, description = "GFF File with regions")
        public String regionFile;

        @Parameter(names = {"-g", "--gene"}, description = GENE_DESCR)
        public String gene;

        @Parameter(names = {"-t", "--type"}, description = TYPE_DESCR)
        public String type;

        @Parameter(names = {"--ct", "--consequence-type"}, description = ANNOT_CONSEQUENCE_TYPE_DESCR)
        public String consequenceType;

        @Parameter(names = {"-c", "--conservation"}, description = ANNOT_CONSERVATION_DESCR)
        public String conservation;

        @Parameter(names = {"--ps", "--protein-substitution"}, description = ANNOT_PROTEIN_SUBSTITUTION_DESCR)
        public String proteinSubstitution;

        @Parameter(names = {"--fs", "--functional-score"}, description = ANNOT_FUNCTIONAL_SCORE_DESCR)
        public String functionalScore;

        @Deprecated
        @Parameter(names = {"--cadd"}, hidden = true, description = DEPRECATED + "use --fs or --functional-score")
        void setDeprecatedFunctionalScore(String cadd) {
            this.functionalScore = cadd;
        }

        @Deprecated
        @Parameter(names = {"--apf", "--alt-population-frequency"}, hidden = true, description = DEPRECATED + "use --pf or --population-frequency-alt")
        void setDeprecatedPopulationFreqAlternate(String populationFreqAlt) {
            this.populationFreqAlt = populationFreqAlt;
        }

        @Parameter(names = {"--pf", "--population-frequency-alt"}, description = ANNOT_POPULATION_ALTERNATE_FREQUENCY_DESCR)
        public String populationFreqAlt;

        @Parameter(names = {"--cohort-stats-ref"}, description = STATS_REF_DESCR)
        public String rf;

        @Parameter(names = {"--cohort-stats-alt"}, description = STATS_ALT_DESCR)
        public String af;

        @Parameter(names = {"--maf", "--cohort-stats-maf"}, description = STATS_MAF_DESCR)
        public String maf;

    }

    /**
     * @see org.opencb.opencga.storage.app.cli.client.executors.VariantQueryCommandUtils#parseGenericVariantQuery
     */
    public static class GenericVariantQueryOptions extends BasicVariantQueryOptions {

        @Parameter(names = {"--group-by"}, description = "Group by gene, ensembl gene or consequence_type")
        public String groupBy;

        @Parameter(names = {"--rank"}, description = "Rank variants by gene, ensemblGene or consequence_type")
        public String rank;

//        @Parameter(names = {"-s", "--study"}, description = "A comma separated list of studies to be used as filter")
//        public String study;

        @Parameter(names = {"--gt", "--genotype"}, description = GENOTYPE_DESCR)
        public String sampleGenotype;

        @Parameter(names = {"--sample"}, description = SAMPLE_DESCR)
        public String samples;

        @Parameter(names = {"--format"}, description = FORMAT_DESCR)
        public String format;

        @Parameter(names = {"-f", "--file"}, description = FILE_DESCR)
        public String file;

        @Parameter(names = {"--info"}, description = INFO_DESCR)
        public String info;

        @Parameter(names = {"--filter"}, description = FILTER_DESCR)
        public String filter;

        @Parameter(names = {"--qual"}, description = QUAL_DESCR)
        public String qual;

        @Parameter(names = {"--score"}, description = SCORE_DESCR)
        public String score;

        @Parameter(names = {"--biotype"}, description = ANNOT_BIOTYPE_DESCR)
        public String geneBiotype;

        @Parameter(names = {"--population-maf"}, hidden = true, description = DEPRECATED + "use --pmaf or --population-frequency-maf")
        void setDeprecatedPopulationFreqMaf(String populationFreqMaf) {
            this.populationFreqMaf = populationFreqMaf;
        }

        @Parameter(names = {"--pmaf", "--population-frequency-maf"}, description = ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY_DESCR)
        public String populationFreqMaf;

        @Parameter(names = {"--population-frequency-ref"}, description = ANNOT_POPULATION_REFERENCE_FREQUENCY_DESCR)
        public String populationFreqRef;

        @Parameter(names = {"--transcript-flag"}, description = ANNOT_TRANSCRIPT_FLAG_DESCR)
        public String flags;

        @Parameter(names = {"--gene-trait-id"}, description = ANNOT_GENE_TRAIT_ID_DESCR)
        public String geneTraitId;

        @Deprecated
        @Parameter(names = {"--gene-trait-name"}, hidden = true, description = DEPRECATED + "use --trait")
        public String geneTraitName;

        @Parameter(names = {"--go", "--gene-ontology"}, description = ANNOT_GO_DESCR)
        public String go;

        @Parameter(names = {"--expression"}, description = ANNOT_EXPRESSION_DESCR)
        public String expression;

        @Parameter(names = {"--protein-keywords"}, description = ANNOT_PROTEIN_KEYWORD_DESCR)
        public String proteinKeywords;

        @Parameter(names = {"--drug"}, description = ANNOT_DRUG_DESCR)
        public String drugs;

        @Deprecated
        @Parameter(names = {"--cosmic"}, hidden = true, description = DEPRECATED + "use --xref")
        void setCosmic(String cosmic) {
            setXref(cosmic);
        }

        @Deprecated
        @Parameter(names = {"--clinvar"}, hidden = true, description = DEPRECATED + "use --xref")
        void setClinvar(String clinvar) {
            setXref(clinvar);
        }

        @Parameter(names = {"--trait"}, description = ANNOT_TRAIT_DESCR)
        void setTrait(String trait) {
            this.trait = this.trait == null ? trait : this.trait + ',' + trait;
        }

        public String trait;

        @Parameter(names = {"--mgf", "--cohort-stats-mgf"}, description = STATS_MGF_DESCR)
        public String mgf;

        @Parameter(names = {"--stats-missing-allele"}, description = MISSING_ALLELES_DESCR)
        public String missingAlleleCount;

        @Parameter(names = {"--stats-missing-genotype"}, description = MISSING_GENOTYPES_DESCR)
        public String missingGenotypeCount;

//        @Parameter(names = {"--dominant"}, description = "[PENDING] Take a family in the form of: FATHER,MOTHER,CHILD and specifies if is" +
//                " affected or not to filter by dominant segregation, example: 1000g:NA001:aff,1000g:NA002:unaff,1000g:NA003:aff",
//                required = false)
//        public String dominant;
//
//        @Parameter(names = {"--recessive"}, description = "[PENDING] Take a family in the form of: FATHER,MOTHER,CHILD and specifies if " +
//                "is affected or not to filter by recessive segregation, example: 1000g:NA001:aff,1000g:NA002:unaff,1000g:NA003:aff",
//                required = false)
//        public String recessive;
//
//        @Parameter(names = {"--ch", "--compound-heterozygous"}, description = "[PENDING] Take a family in the form of: FATHER,MOTHER," +
//                "CHILD and specifies if is affected or not to filter by compound heterozygous, example: 1000g:NA001:aff," +
//                "1000g:NA002:unaff,1000g:NA003:aff")
//        public String compoundHeterozygous;


        @Parameter(names = {"--include-study"}, description = INCLUDE_STUDY_DESCR)
        public String includeStudy;

        @Deprecated
        @Parameter(names = {"--output-study"}, hidden = true, description = DEPRECATED + "use --include-study")
        void setOutputStudy(String outputStudy) {
            includeStudy = outputStudy;
        }

        @Parameter(names = {"--include-file"}, description = INCLUDE_FILE_DESCR)
        public String includeFile;

        @Deprecated
        @Parameter(names = {"--output-file"}, hidden = true, description = DEPRECATED + "use --include-file")
        void setOutputFile(String outputFile) {
            includeFile = outputFile;
        }

        @Parameter(names = {"--include-sample"}, description = INCLUDE_SAMPLE_DESCR)
        public String includeSample;

        @Deprecated
        @Parameter(names = {"--output-sample"}, hidden = true, description = DEPRECATED + "use --include-sample")
        void setOutputSample(String outputSample) {
            includeSample = outputSample;
        }

        @Parameter(names = {"--include-format"}, description = INCLUDE_FORMAT_DESCR)
        public String includeFormat;

        @Parameter(names = {"--include-genotype"}, description = INCLUDE_GENOTYPE_DESCR)
        public boolean includeGenotype;

        @Parameter(names = {"--annotations", "--output-vcf-info"}, description = "Set variant annotation to return in the INFO column. " +
                "Accepted values include 'all', 'default' or a comma-separated list such as 'gene,biotype,consequenceType'", arity = 1)
        public String annotations;

        @Deprecated
        @Parameter(names = {"--output-unknown-genotype"}, hidden = true, description = DEPRECATED + "use --unknown-genotype")
        void setOutputUnknownGenotype(String outputUnknownGenotype) {
            this.unknownGenotype = outputUnknownGenotype;
        }

        @Parameter(names = {"--unknown-genotype"}, description = UNKNOWN_GENOTYPE_DESCR)
        public String unknownGenotype = "./.";

        @Deprecated
        @Parameter(names = {"--output-histogram"}, hidden = true, description = DEPRECATED + "use --histogram")
        void setOutputHistogram(boolean histogram) {
            this.histogram = histogram;
        }

        @Parameter(names = {"--histogram"}, description = "Calculate histogram. Requires --region.")
        public boolean histogram;

        @Parameter(names = {"--histogram-interval"}, description = "Histogram interval size. Default:2000", arity = 1)
        public int interval;

        @Deprecated
        @Parameter(names = {"--hpo"}, hidden = true, description = DEPRECATED + "use --trait", arity = 1)
        void setHpo(String hpo) {
            setTrait(hpo);
        }

        @Deprecated
        @Parameter(names = {"--annot-xref"}, hidden = true, description = DEPRECATED + "use --xref")
        void setAnnotXref(String annotXref) {
            setXref(annotXref);
        }

        @Parameter(names = {"--xref"}, description = ANNOT_XREF_DESCR)
        void setXref(String xref) {
            this.xref = this.xref == null ? xref : this.xref + ',' + xref;
        }

        public String xref;

        @Parameter(names = {"--clinical-significance"}, description = ANNOT_CLINICAL_SIGNIFICANCE_DESCR)
        public String clinicalSignificance;

        @Parameter(names = {"--sample-metadata"}, description = SAMPLE_METADATA_DESCR)
        public boolean samplesMetadata;

        @Parameter(names = {"--sample-limit"}, description = SAMPLE_LIMIT_DESCR)
        public int sampleLimit;

        @Parameter(names = {"--sample-skip"}, description = SAMPLE_SKIP_DESCR)
        public int sampleSkip;

        @Parameter(names = {"--summary"}, description = "Fast fetch of main variant parameters")
        public boolean summary;

        @Parameter(names = {"--sort"}, description = "Sort the output elements.")
        public boolean sort;
    }

    @Parameters(commandNames = {"query"}, commandDescription = "Search over indexed variants")
    public class VariantQueryCommandOptions extends GenericVariantQueryOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public GeneralCliOptions.QueryCommandOptions commonQueryOptions = queryCommandOptions;

        @Parameter(names = {"-s", "--study"}, description = STUDY_DESCR)
        public String study;

        @Parameter(names = {"--of", "--output-format"}, description = "Output format: vcf, vcf.gz, json or json.gz", arity = 1)
        public String outputFormat = "vcf";

        @Parameter(names = {"--variants-file"}, description = "GFF File with regions")
        public String variantsFile;

    }

    /**
     *  import: specific options
     */
    @Parameters(commandNames = {"import"}, commandDescription = "Import a variants dataset into an empty database")
    public class ImportVariantsCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-i", "--input"}, description = "File to import in the selected backend", required = true)
        public String input;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name to load the data", arity = 1)
        public String dbName;

    }

    /**
     *  annotate: generic and specific options
     */
    public static class GenericVariantAnnotateOptions {
        public static final String ANNOTATE_DESCRIPTION = "Create and load variant annotations into the database";

        @Parameter(names = {"--create"}, description = "Run only the creation of the annotations to a file (specified by --output-filename)")
        public boolean create;

        @Parameter(names = {"--load"}, description = "Run only the load of the annotations into the DB from FILE")
        public String load;

        @Parameter(names = {"--custom-name"}, description = "Provide a name to the custom annotation")
        public String customAnnotationKey = null;

        @Parameter(names = {"--annotator"}, description = "Annotation source {cellbase_rest, cellbase_db_adaptor}")
        public VariantAnnotatorFactory.AnnotationEngine annotator;

        @Parameter(names = {"--overwrite-annotations"}, description = "Overwrite annotations in variants already present")
        public boolean overwriteAnnotations;

        @Parameter(names = {"--output-filename"}, description = "Output file name. Default: dbName", arity = 1)
        public String fileName;

        @Parameter(names = {"--region"}, description = "Comma separated region filters", splitter = CommaParameterSplitter.class)
        public String region;

        @Parameter(names = {"--filter-region"}, hidden = true)
        @Deprecated
        public void setRegion(String r) {
            region = r;
        }
    }

    @Parameters(commandNames = {"annotate"}, commandDescription = GenericVariantAnnotateOptions.ANNOTATE_DESCRIPTION)
    public class VariantAnnotateCommandOptions extends GenericVariantAnnotateOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--species"}, description = "Species. Default hsapiens", arity = 1)
        public String species = "hsapiens";

        @Parameter(names = {"--assembly"}, description = "Assembly. Default GRch37", arity = 1)
        public String assembly = "GRCh37";

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;

        @Parameter(names = {"-o", "--outdir"}, description = "Output directory.", arity = 1)
        public String outdir;
    }

    public static class GenericAnnotationCommandOptions {
        public static final String ANNOTATION_ID_PARAM = "--annotation-id";
        public static final String ANNOTATION_ID_DESCRIPTION = "Annotation identifier";

        @Parameter(names = {ANNOTATION_ID_PARAM}, description = ANNOTATION_ID_DESCRIPTION, required = true, arity = 1)
        public String annotationId;
    }

    public static class GenericAnnotationSaveCommandOptions extends GenericAnnotationCommandOptions {
        public static final String ANNOTATION_SAVE_COMMAND = "annotation-save";
        public static final String ANNOTATION_SAVE_COMMAND_DESCRIPTION = "Save a copy of the current variant annotation at the database";
    }

    @Parameters(commandNames = {GenericAnnotationSaveCommandOptions.ANNOTATION_SAVE_COMMAND}, commandDescription = GenericAnnotationSaveCommandOptions.ANNOTATION_SAVE_COMMAND_DESCRIPTION)
    public class AnnotationSaveCommandOptions extends GenericAnnotationSaveCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;

    }

    public static class GenericAnnotationDeleteCommandOptions extends GenericAnnotationCommandOptions {
        public static final String ANNOTATION_DELETE_COMMAND = "annotation-delete";
        public static final String ANNOTATION_DELETE_COMMAND_DESCRIPTION = "Deletes a saved copy of variant annotation";
    }

    @Parameters(commandNames = {GenericAnnotationDeleteCommandOptions.ANNOTATION_DELETE_COMMAND}, commandDescription = GenericAnnotationDeleteCommandOptions.ANNOTATION_DELETE_COMMAND_DESCRIPTION)
    public class AnnotationDeleteCommandOptions extends GenericAnnotationDeleteCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;

    }

    public static class GenericAnnotationQueryCommandOptions {
        public static final String ANNOTATION_QUERY_COMMAND = "annotation-query";
        public static final String ANNOTATION_QUERY_COMMAND_DESCRIPTION = "Query variant annotations from any saved versions";

        @Parameter(names = ANNOTATION_ID_PARAM, description = ANNOTATION_ID_DESCRIPTION)
        public String annotationId = VariantAnnotationManager.CURRENT;

        @Parameter(names = {"--id"}, description = ID_DESCR, variableArity = true)
        public List<String> id;

        @Parameter(names = {"-r", "--region"}, description = REGION_DESCR)
        public String region;

    }

    @Parameters(commandNames = {GenericAnnotationQueryCommandOptions.ANNOTATION_QUERY_COMMAND}, commandDescription = GenericAnnotationQueryCommandOptions.ANNOTATION_QUERY_COMMAND_DESCRIPTION)
    public class AnnotationQueryCommandOptions extends GenericAnnotationQueryCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public GeneralCliOptions.DataModelOptions dataModelOptions = new GeneralCliOptions.DataModelOptions();

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;

        @Parameter(names = {"--skip"}, description = "Skip some number of elements.", arity = 1)
        public int skip;

        @Parameter(names = {"--limit"}, description = "Limit the number of returned elements.", arity = 1)
        public int limit;
    }

    public static class GenericAnnotationMetadataCommandOptions {
        public static final String ANNOTATION_METADATA_COMMAND = "annotation-metadata";
        public static final String ANNOTATION_METADATA_COMMAND_DESCRIPTION = "Read variant annotations metadata from any saved versions";

        @Parameter(names = ANNOTATION_ID_PARAM, description = ANNOTATION_ID_DESCRIPTION)
        public String annotationId = VariantQueryUtils.ALL;
    }

    @Parameters(commandNames = {GenericAnnotationMetadataCommandOptions.ANNOTATION_METADATA_COMMAND}, commandDescription = GenericAnnotationMetadataCommandOptions.ANNOTATION_METADATA_COMMAND_DESCRIPTION)
    public class AnnotationMetadataCommandOptions extends GenericAnnotationMetadataCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;
    }


    public static class GenericVariantScoreIndexCommandOptions {
        public static final String SCORE_INDEX_COMMAND = "score-index";
        public static final String SCORE_INDEX_COMMAND_DESCRIPTION = "Index a variant score in the database.";

        @Parameter(names = {"--name"}, description = "Unique name of the score within the study", required = true)
        public String scoreName;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed indexation", arity = 0)
        public boolean resume;

        @Parameter(names = {"--cohort1"}, description = "Cohort used to compute the score. "
                + "Use the cohort '" + StudyEntry.DEFAULT_COHORT + "' if all samples from the study where used to compute the score", required = true)
        public String cohort1;

        @Parameter(names = {"--cohort2"}, description = "Second cohort used to compute the score, typically to compare against the first cohort. "
                + "If only one cohort was used to compute the score, leave empty")
        public String cohort2;

        @Parameter(names = {"-i", "--input-file"}, description = "Input file to load", required = true)
        public String input;

        @Parameter(names = {"--input-columns"}, description = "Indicate which columns to load from the input file. "
                + "Provide the column position (starting in 0) for the column with the score with 'SCORE=n'. "
                + "Optionally, the PValue column with 'PVALUE=n'. "
                + "The, to indicate the variant associated with the score, provide either the columns ['CHROM', 'POS', 'REF', 'ALT'], "
                + "or the column 'VAR' containing a variant representation with format 'chr:start:ref:alt'. "
                + "e.g. 'CHROM=0,POS=1,REF=3,ALT=4,SCORE=5,PVALUE=6' or 'VAR=0,SCORE=1,PVALUE=2'", required = true)
        public String columns;
    }

    public static class GenericVariantScoreDeleteCommandOptions {
        public static final String SCORE_DELETE_COMMAND = "score-delete";
        public static final String SCORE_DELETE_COMMAND_DESCRIPTION = "Remove a variant score from the database.";

        @Parameter(names = {"--name"}, description = "Unique name of the score within the study", required = true)
        public String scoreName;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed remove", arity = 0)
        public boolean resume;

        @Parameter(names = {"--force"}, description = "Force remove of partially indexed scores", arity = 0)
        public boolean force;
    }

    /**
     *  annotate: generic and specific options
     */
    public static class GenericAggregateFamilyOptions {

        @Parameter(names = {"--samples"}, description = "Samples within the same study to fill", required = true)
        public List<String> samples;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed operation")
        public boolean resume;

//        @Parameter(names = {"--exclude-hom-ref"}, description = "Do not fill gaps of samples with HOM-REF genotype (0/0)", arity = 0)
//        public boolean excludeHomRef;
    }

    @Parameters(commandNames = {AggregateFamilyCommandOptions.AGGREGATE_FAMILY_COMMAND}, commandDescription = AggregateFamilyCommandOptions.AGGREGATE_FAMILY_COMMAND_DESCRIPTION)
    public class AggregateFamilyCommandOptions extends GenericAggregateFamilyOptions {

        public static final String AGGREGATE_FAMILY_COMMAND = "aggregate-family";
        public static final String AGGREGATE_FAMILY_COMMAND_DESCRIPTION = "Find variants where not all the samples are present, and fill the empty values.";

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--study"}, description = "Study", arity = 1)
        public String study;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;
    }

    @Parameters(commandNames = {AggregateCommandOptions.AGGREGATE_COMMAND}, commandDescription = AggregateCommandOptions.AGGREGATE_COMMAND_DESCRIPTION)
    public class AggregateCommandOptions extends GenericAggregateCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--study"}, description = "Study", arity = 1)
        public String study;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", required = true, arity = 1)
        public String dbName;
    }

    public static class GenericAggregateCommandOptions {
        public static final String AGGREGATE_COMMAND = "aggregate";
        public static final String AGGREGATE_COMMAND_DESCRIPTION = "Find variants where not all the samples are present, and fill the empty values, excluding HOM-REF (0/0) values.";

        @Parameter(names = {"--resume"}, description = "Resume a previously failed operation")
        public boolean resume;

        @Parameter(names = {"--overwrite"}, description = "Overwrite gaps for all files and variants. Repeat operation for already processed variants.")
        public boolean overwrite;
    }

    /**
     *  benchmark: specific options
     */
    @Parameters(commandNames = {"benchmark"}, commandDescription = "[PENDING] Benchmark load and fetch variants with different databases")
    public class BenchmarkCommandOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;


        @Parameter(names = {"--num-repetition"}, description = "Number of repetition", arity = 1)
        public int repetition = 3;

        @Parameter(names = {"--load"}, description = "File name with absolute path", arity = 1)
        public String load;

        @Parameter(names = {"--queries"}, description = "Queries to fetch the data from tables", arity = 1)
        public String queries;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", arity = 1)
        public String database;

        @Parameter(names = {"-t", "--table"}, description = "Benchmark variants", arity = 1)
        public String table;

        @Parameter(names = {"--host"}, description = "DataBase name", arity = 1)
        public String host;

        @Parameter(names = {"--concurrency"}, description = "Number of threads to run in parallel", arity = 1)
        public int concurrency = 1;

    }

    /**
     *  stats: generic and specific options
     */
    public static class GenericVariantStatsOptions {

        @Parameter(names = {"--overwrite-stats"}, description = "Overwrite stats in variants already present")
        public boolean overwriteStats = false;

        @Parameter(names = {"--region"}, description = "Region to calculate.")
        public String region;

        @Parameter(names = {"--gene"}, description = "List of genes.")
        public String gene;

        @Parameter(names = {"--update-stats"}, description = "Calculate stats just for missing positions. Assumes that existing stats are" +
                " correct")
        public boolean updateStats = false;

//        @Parameter(names = {"-s", "--study-id"}, description = "Unique ID for the study where the file is classified", required = true,
//                arity = 1)
//        public String studyId;

//        @Parameter(names = {"-f", "--file"}, description = "Calculate stats only for the selected file", arity = 1)
//        public String file;

        @Parameter(names = {"--output-filename"}, description = "Output file name. Default: database name", arity = 1)
        public String fileName;

        @Parameter(names = {"--aggregated"}, description = "Select the type of aggregated VCF file: none, basic, EVS or ExAC", arity = 1)
        public Aggregation aggregated = Aggregation.NONE;

        @Parameter(names = {"--aggregation-mapping-file"}, description = "File containing population names mapping in an aggregated VCF file")
        public String aggregationMappingFile;

        @Parameter(names = {"--resume"}, description = "Resume a previously failed stats calculation", arity = 0)
        public boolean resume;
    }

    @Parameters(commandNames = {"stats"}, commandDescription = "Create and load stats into a database.")
    public class VariantStatsCommandOptions extends GenericVariantStatsOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-s", "--study"}, description = "Unique ID for the study where the file is classified", required = true,
                arity = 1)
        public String study;

        @Parameter(names = {"-d", "--database"}, description = "DataBase name", arity = 1)
        public String dbName;

        @Parameter(names = {"-o", "--outdir"}, description = "Output directory.", arity = 1)
        public String outdir = ".";

        @DynamicParameter(names = {"--cohort"}, description = "Cohort definition with the schema -> <cohort>:<sample>" +
                "(,<sample>)* ", descriptionKey = "CohortName", assignment = ":")
        public Map<String, String> cohort = new HashMap<>();

//        @Parameter(names = {"--create"}, description = "Run only the creation of the stats to a file")
//        public boolean create = false;

        @Parameter(names = {"--load"}, description = "Load the stats from an already existing FILE directly into the database")
        public String load = null;
    }

    /**
     * export: generic and specific options
     */

    public static class GenericVariantExportOptions {

        @Parameter(names = {"--output-filename"}, description = "Output filename.", arity = 1)
        public String outFilename = ".";

//        @Parameter(names = {"--region"}, description = "Variant region to export.")
//        public String region;

//        @Parameter(names = {"--study-configuration-file"}, description = "File with the study configuration. org.opencb.opencga.storage" +
//                ".core.StudyConfiguration", arity = 1)
//        public String studyConfigurationFile;
    }


    @Parameters(commandNames = {"export"}, commandDescription = "Export variants into a VCF file.")
    public class VariantExportCommandOptions extends GenericVariantExportOptions {

//        @ParametersDelegate
//        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public VariantQueryCommandOptions queryOptions = new VariantQueryCommandOptions();

//
//        @Parameter(names = {"-d", "--database"}, description = "DataBase name", arity = 1)
//        public String dbName;
    }

    /**
     * export: specific options
     */

    public static class GenericVariantSearchOptions extends BasicVariantQueryOptions {

        // TODO: both clinvar and cosmic should be moved to basic variant query options
        @Parameter(names = {"---clinvar"}, description = "List of ClinVar accessions or traits.", arity = 1)
        public String clinvar;

        @Parameter(names = {"---cosmic"}, description = "List of COSMIC mutation IDs, primary histologies"
                + " or histology subtypes.", arity = 1)
        public String cosmic;

        @Parameter(names = {"--facet"}, description = "Facet search.", arity = 1)
        public String facet;

        @Deprecated
        @Parameter(names = {"--facetRange"}, description = "Facet range (DEPRECATED)", arity = 1)
        public String facetRange;
    }

    @Parameters(commandNames = {"search"}, commandDescription = "Solr support.")
    public class VariantSearchCommandOptions extends GenericVariantSearchOptions {

        @ParametersDelegate
        public GeneralCliOptions.CommonOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--index"}, description = "Index a file into core/collection Solr.", arity = 0)
        public boolean index;

        @Parameter(names = {"--overwrite"}, description = "Overwrite search index for all files and variants. Repeat operation for already processed variants.")
        public boolean overwrite;

        @Parameter(names = {"-i", "--input"}, description = "Path to the file to index. Valid formats: AVRO and JSON.", arity = 1)
        public String inputFilename;

//        @Parameter(names = {"-f", "--file-id"}, description = "Calculate stats only for the selected file", arity = 1)
//        public String fileId;
//
//        @Parameter(names = {"-s", "--study-id"}, description = "Unique ID for the study where the file is classified", required = true,
//                arity = 1)
//        public String studyId;
//

//        @Parameter(names = {"--mode"}, description = "Search mode. Valid values: core, collection.", arity = 1)
//        public String mode = "core";

        @Parameter(names = {"--create"}, description = "Create a new core/collection.", arity = 0)
        public boolean create;

        @Parameter(names = {"--solr-url"}, description = "Url to Solr server, e.g.: http://localhost:8983/solr/", arity = 1)
        public String solrUrl;

        @Parameter(names = {"--solr-config"}, description = "Solr configuration name.", arity = 1)
        public String solrConfig;

//        @Parameter(names = {"--solr-num-shards"}, description = "Number of Solr collection shards (only for a Solr cluster mode).", arity = 1)
//        public int numShards = 2;
//
//        @Parameter(names = {"--solr-num-replicas"}, description = "Number of Solr collection replicas (only for a Solr cluster mode).", arity = 1)
//        public int numReplicas = 2;

        @Parameter(names = {"-d", "--database"}, description = "Name of the target core ore collection.", arity = 1)
        public String dbName;
    }
}
