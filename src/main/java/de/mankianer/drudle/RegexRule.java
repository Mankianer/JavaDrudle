package de.mankianer.drudle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@AllArgsConstructor
@RequiredArgsConstructor
public class RegexRule implements DrudleRule {

  @Getter private final String name;
  private final String pattern;
  private final String output;
  @Getter private String description;

  /**
   * Applies the regex rule to the given drudle string and replace the output vars with the matching
   * named capturing groups of the regex.<br>
   * The output vars are in the format {varName}.
   *
   * @param drudle the input drudle string
   * @return the transformed drudle string or null if the rule does not apply
   */
  @Override
  public DrudleRuleResult apply(String drudle) {
    List<String> matchingParts = new ArrayList<>();
    List<String> usedParts = new ArrayList<>();
    final Map<String, String> valuesToParam = new HashMap<>();

        var regex = java.util.regex.Pattern.compile(pattern);
        var matcher = regex.matcher(drudle);
        if (matcher.matches()) {
            for (var groupName : regex.namedGroups().keySet()) {
                valuesToParam.put(matcher.group(groupName), groupName);
                matchingParts.add(matcher.group(groupName));
                if (output.contains("{" + groupName + "}")) {
                    usedParts.add(matcher.group(groupName));
                }
            }
            return new DrudleRuleResult(name, drudle, matchingParts, usedParts, parts -> {
                Map<String, String> mappedParts = parts.entrySet().stream().collect(toMap(entry -> valuesToParam.get(entry.getKey()), Map.Entry::getValue));
                return getOutput(mappedParts);
            });
        }
        return null;
    }

  private String getOutput(Map<String, String> fulfilledParts) {
    var result = output;
    for (var entry : fulfilledParts.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }
}
