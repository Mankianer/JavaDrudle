package de.mankianer.drudle;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class DrudleRuleResult {
  @Getter private final String ruleName;
  @Getter private final String input;
  @Getter private final String output;

  /** List of parts that matched the input string */
  private final List<String> matchingParts;

  /** List of parts that are used to generate the output string */
  private final List<String> usedParts;

  private final Map<String, String> fulfilledParts;

  /** Map of functions to fulfill the used parts of the rule. Functions returns <br>null: if function is unsolved<br>DrudleRuleResult: if all functions are solved*/
  @Getter
  private final Map<String, Function<String, DrudleRuleResult>> usedPartsFulfillmentConsumerMap;

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
  private Map<String, Function<String, DrudleRuleResult>> getUsedPartsFulfillmentConsumerMap(
      Function<Map<String, String>, String> loadOutput) {
    return usedParts.stream()
        .distinct()
        .collect(
            toMap(
                part -> part,
                part ->
                    (String value) -> {
                      fulfilledParts.put(part, value);
                      if (fulfilledParts.size() == usedParts.size()) {
                        return new DrudleRuleResult(
                            ruleName,
                            input,
                            loadOutput.apply(fulfilledParts),
                            matchingParts,
                            usedParts);
                      }
                      return null;
                    }));
  }

  public boolean isSolved() {
    return output != null && !output.isEmpty();
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
}
