package de.mankianer.drudle;

import java.util.List;

public interface DrudleRule {

    /**
     * Applies the drudle rule to the given drudle string.<br>
     * Rule has to use all characters of the input string to match. Validated via class {@link DrudleRuleResult}.
     *
     * @param drudle the input drudle string
     * @return a List of {@link DrudleRuleResult} if the rule applies, otherwise an empty list
     */
    public List<DrudleRuleResult> apply(String drudle);

    public String getName();
    public default String getDescription() { return ""; }
}
