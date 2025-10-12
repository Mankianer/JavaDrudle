package de.mankianer.drudle;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

public class DrudleRuleResult {
  @Getter private final String ruleName;
  @Getter private final String input;
  @Getter private final String output;
  @Getter
  protected List<DrudleRuleResult> previousResults;

  /** List of parts that matched the input string */
  private final List<String> matchingParts;

  /** List of parts that are used to generate the output string */
  private final List<String> usedParts;

  private final Map<String, String> fulfilledParts;

  /** Map of functions to fulfill the used parts of the rule. Functions returns <br>null: if function is unsolved<br>DrudleRuleResult: if all functions are solved*/
  @Getter
  private final Map<String, Function<DrudleRuleResult, DrudleRuleResultSolved>> usedPartsFulfillmentConsumerMap;

  /**
   * Constructor for an unsolved DrudleRuleResult. The output is generated when all used parts are
   * fulfilled.
   *
   * @param ruleName the name of the rule
   * @param input the input string
   * @param matchingParts the parts that matched the input string
   * @param usedParts the parts that would be used to generate the output string
   * @param loadOutput a function that takes a map of fulfilled parts and returns the output string
   */
  public DrudleRuleResult(
      String ruleName,
      String input,
      List<String> matchingParts,
      List<String> usedParts,
      Function<Map<String, String>, String> loadOutput) {
    this.ruleName = ruleName;
    this.input = input;
    this.output = null;
    this.matchingParts = matchingParts;
    this.usedParts = usedParts;
    fulfilledParts = new HashMap<>();
    usedPartsFulfillmentConsumerMap = getUsedPartsFulfillmentConsumerMap(loadOutput);
  }

  private DrudleRuleResult(
      String ruleName,
      String input,
      String output,
      List<String> matchingParts,
      List<String> usedParts) {
    this.ruleName = ruleName;
    this.input = input;
    this.output = output;
    this.matchingParts = matchingParts;
    this.usedParts = usedParts;
    fulfilledParts = Map.of();
    usedPartsFulfillmentConsumerMap = Map.of();
  }

  /**
   * Returns a map of functions to fulfill the used parts of the rule. Each function takes a String
   * value and returns a DrudleRuleResult if all parts are fulfilled, otherwise returns null.
   *
   * @return
   */
  private Map<String, Function<DrudleRuleResult, DrudleRuleResultSolved>> getUsedPartsFulfillmentConsumerMap(
      Function<Map<String, String>, String> loadOutput) {
    return usedParts.stream()
        .distinct()
        .collect(
            toMap(
                part -> part,
                part ->
                    (DrudleRuleResult solved) -> {
                      fulfilledParts.put(part, solved.getOutput());
                      if (fulfilledParts.size() == usedParts.size()) {
                          var previous = new ArrayList<>(solved.getPreviousResults());
                          previous.add(this);
                        return new DrudleRuleResultSolved(
                            ruleName,
                            input,
                            loadOutput.apply(fulfilledParts),
                            matchingParts,
                            usedParts, previous);
                      }
                      return null;
                    }));
  }

  public boolean isSolved() {
    return output != null && !output.isEmpty();
  }

  public DrudleRuleResultSolved getSolvedResult() {
    if (fulfilledParts.size() >= usedParts.size()) {
      return new DrudleRuleResultSolved(
          ruleName, input, output, matchingParts, usedParts, previousResults == null ? List.of() : previousResults);
    }
    return null;
  }

  public boolean isValid() {
    if (ruleName == null || ruleName.isEmpty()) return false;
    if (input == null || input.isEmpty()) return false;
    if (usedParts == null || usedParts.isEmpty()) return false;
    if (matchingParts == null || matchingParts.isEmpty()) return false;
    if (matchingParts.size() < usedParts.size()) return false;
    // Check if all characters from input are used in matching parts
    var o = input;
    matchingParts.sort((a, b) -> b.length() - a.length());
    for (String part : matchingParts) {
      if (part == null || o.length() < part.length()) return false;
      o = o.replaceFirst(part, "");
    }
    return o.isEmpty();
  }

  public static class DrudleRuleResultSolved extends DrudleRuleResult {
    protected DrudleRuleResultSolved(
        String ruleName,
        String input,
        String output,
        List<String> matchingParts,
        List<String> usedParts,
        List<DrudleRuleResult> previousResults) {
      super(ruleName, input, output, matchingParts, usedParts);
        this.previousResults = List.copyOf(previousResults);
    }

    public DrudleRuleResultSolved(String unsolvedValue) {
        super("NoRule", unsolvedValue, unsolvedValue, List.of(unsolvedValue), List.of(unsolvedValue));
        this.previousResults = List.of();
    }

  }
}
