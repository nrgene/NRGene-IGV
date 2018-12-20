package org.broad.tribble.annotation;


/**
 * 
 * @author aaron 
 * 
 * Class DiploidGenotype
 *
 * A descriptions should go here. Blame aaron if it's missing.
 */
public enum DiploidGenotype {
    AA, AC, AG, AT, CC, CG, CT, GG, GT, TT;

    public static DiploidGenotype toDiploidGenotype(String genotype) {
        if (genotype.length() != 2)
            throw new DiploidGenotypeException("Genotype string for conversion should be of length 2, we were passed = " + genotype);
        genotype = genotype.toUpperCase();
        for (DiploidGenotype g: DiploidGenotype.values())
            if (g.toString().equals(genotype)) return g;
        throw new DiploidGenotypeException("Unable to find genotype matching " + genotype);
    }

    public boolean isHet() {
        return toString().toCharArray()[0] != toString().toCharArray()[1];
    }

    public boolean containsBase(char base) {
        return (toString().charAt(0) == base || toString().charAt(1) == base);
    }
}

class DiploidGenotypeException extends RuntimeException {
    DiploidGenotypeException(String s) {
        super(s);
    }

    DiploidGenotypeException(String s, Throwable throwable) {
        super(s, throwable);
    }
}