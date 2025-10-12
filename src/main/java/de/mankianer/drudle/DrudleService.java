package de.mankianer.drudle;

import java.util.Set;

public interface DrudleService {
    public Set<DrudleRuleResult> processDrudle(String drudle);
}
