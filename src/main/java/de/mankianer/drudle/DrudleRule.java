package de.mankianer.drudle;

public interface DrudleRule {

    /**
     * Applies the drudle rule to the given drudle string.
     * Rule has to use all characters of the input string to match.
     *
     * @param drudle the input drudle string
     * @return the transformed drudle string or null if the rule does not apply
     */
    public DrudleRuleResult apply(String drudle);

    public String getName();
    public default String getDescription() { return ""; }
}
