package org.opencb.opencga.storage.core.variant.adaptors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.feature.Genotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created on 04/06/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public enum GenotypeClass {
    /**
     * Homozygous reference.
     * <p>
     * 0, 0/0, 0/0/0, ...
     */
    HOM_REF(gt -> StringUtils.containsOnly(gt, '0', '/', '|')),

    /**
     * Homozygous alternate.
     * <p>
     * 1, 1/1, 1|1, 2/2, 3/3, 1/1/1, ...
     */
    HOM_ALT(str -> {
        Genotype gt = parseGenotype(str);
        if (gt == null) {
            // Skip invalid genotypes
            return false;
        }
        int[] alleles = gt.getAllelesIdx();
        int firstAllele = alleles[0];
        if (firstAllele <= 0) {
            // Discard if first allele is reference or missing
            return false;
        }
        for (int i = 1; i < alleles.length; i++) {
            if (alleles[i] != firstAllele) {
                return false;
            }
        }
        return true;
    }),

    /**
     * Heterozygous.
     * <p>
     * 0/1, 1/2, 0/2, 2/4, 0|1, 1|0, 0/0/1, ...
     */
    HET(str -> {
        Genotype gt = parseGenotype(str);
        if (gt == null) {
            // Skip invalid genotypes
            return false;
        }
        int[] alleles = gt.getAllelesIdx();
        int firstAllele = alleles[0];
        if (firstAllele < 0 || gt.isHaploid()) {
            // Discard if first allele is missing, or if haploid
            return false;
        }
        for (int i = 1; i < alleles.length; i++) {
            int allele = alleles[i];
            if (allele == firstAllele || allele < 0) {
                return false;
            }
        }
        return true;
    }),

    /**
     * Heterozygous Reference.
     * <p>
     * 0/1, 0/2, 0/3, 0|1, ...
     */
    HET_REF(str -> {
        Genotype gt = parseGenotype(str);
        if (gt == null) {
            // Skip invalid genotypes
            return false;
        }
        if (gt.isHaploid()) {
            // Discard if haploid
            return false;
        }
        boolean hasReference = false;
        boolean hasAlternate = false;

        for (int allele : gt.getAllelesIdx()) {
            hasReference |= allele == 0;
            hasAlternate |= allele > 0; // Discard ref and missing
        }
        return hasReference && hasAlternate;
    }),

    /**
     * Heterozygous Alternate.
     * <p>
     * 1/2, 1/3, 2/4, 2|1, ...
     */
    HET_ALT(str -> {
        Genotype gt = parseGenotype(str);
        if (gt == null) {
            // Skip invalid genotypes
            return false;
        }
        int[] alleles = gt.getAllelesIdx();
        int firstAllele = alleles[0];
        if (firstAllele <= 0 || gt.isHaploid()) {
            // Discard if first allele is reference or missing, or if haploid
            return false;
        }
        for (int i = 1; i < alleles.length; i++) {
            int allele = alleles[i];
            if (allele == firstAllele || allele <= 0) {
                return false;
            }
        }
        return true;
    }),

    /**
     * Missing.
     * <p>
     * ., ./., ././., ...
     */
    MISS(str -> {
        Genotype gt = parseGenotype(str);
        if (gt == null) {
            // Skip invalid genotypes
            return false;
        }
        for (int allele : gt.getAllelesIdx()) {
            if (allele != -1) {
                return false;
            }
        }
        return true;
    });

    /**
     * Indicate that the genotype information is unknown.
     *
     * It could be any value: 0/0, ./., 2/2 ...
     */
    public static final String UNKNOWN_GENOTYPE = "?/?";
    /**
     * Indicate that the genotype value was not available in the input variant file.
     */
    public static final String NA_GT_VALUE = "NA";
    /**
     * Indicate that none genotype should match with this value.
     */
    public static final String NONE_GT_VALUE = "x/x";

    private final Predicate<String> predicate;

    private static final Logger logger = LoggerFactory.getLogger(GenotypeClass.class);

    GenotypeClass(Predicate<String> predicate) {
        Predicate<String> stringPredicate = gt -> !gt.equals(UNKNOWN_GENOTYPE);
        this.predicate = stringPredicate.and(predicate);
    }

    public Predicate<String> predicate() {
        return predicate;
    }

    public boolean test(String genotype) {
        return predicate.test(genotype);
    }

    public List<String> filter(String... gts) {
        return filter(Arrays.asList(gts));
    }

    public List<String> filter(List<String> gts) {
        return gts.stream().filter(predicate).collect(Collectors.toList());
    }

    public static List<String> filter(List<String> gts, List<String> loadedGts) {
        return filter(gts, loadedGts, Arrays.asList("0/0", "./."));
    }

    public static List<String> filter(List<String> gts, List<String> loadedGts, List<String> defaultGts) {
        Set<String> filteredGts = new LinkedHashSet<>(gts.size());
        for (String gt : gts) {
            GenotypeClass genotypeClass = GenotypeClass.from(gt);
            if (gt.equals(NONE_GT_VALUE) || gt.equals(NA_GT_VALUE) || gt.equals(UNKNOWN_GENOTYPE)) {
                filteredGts.add(gt);
            } else if (genotypeClass == null) {
                Genotype genotype = parseGenotype(gt);
                if (genotype == null) {
                    // Skip invalid genotypes
                    continue;
                }

                // Normalize if needed
                if (!genotype.isPhased()) {
                    genotype.normalizeAllelesIdx();
                }
                filteredGts.add(genotype.toString());

                // If unphased, add phased genotypes, if any
                filteredGts.addAll(getPhasedGenotypes(genotype, loadedGts));
            } else {
                filteredGts.addAll(genotypeClass.filter(loadedGts));
                filteredGts.addAll(genotypeClass.filter(defaultGts));
            }
        }
        return new ArrayList<>(filteredGts);
    }

    public static List<String> getPhasedGenotypes(Genotype genotype, List<String> loadedGts) {
        if (!genotype.isPhased()) {
            List<String> phasedGts = new ArrayList<>(2);
            genotype.setPhased(true);
            String phased = genotype.toString();
            if (loadedGts.contains(phased)) {
                phasedGts.add(phased);
            }
            int[] allelesIdx = genotype.getAllelesIdx();
            if (allelesIdx.length == 2) {
                int allelesIdx0 = allelesIdx[0];
                allelesIdx[0] = allelesIdx[1];
                allelesIdx[1] = allelesIdx0;
                phased = genotype.toString();
                if (loadedGts.contains(phased)) {
                    phasedGts.add(phased);
                }
            }
            return phasedGts;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets the GenotypeClass, returning {@code null} if not found.
     *
     * @param gt Genotype class name
     * @return the enum, null if not found
     */
    public static GenotypeClass from(String gt) {
        return EnumUtils.getEnum(GenotypeClass.class, gt.toUpperCase());
    }

    private static Genotype parseGenotype(String gt) {
        Genotype genotype;
        try {
            genotype = new Genotype(gt);
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid genotype " + gt, e);
            return null;
        }
        return genotype;
    }
}
