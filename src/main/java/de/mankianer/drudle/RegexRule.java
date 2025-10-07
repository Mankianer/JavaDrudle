package de.mankianer.drudle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;

@Log4j2
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
  public List<DrudleRuleResult> apply(String drudle) {
    List<DrudleRuleResult> ret = new ArrayList<>();

    var regex = Pattern.compile(pattern);
    var matcher = regex.matcher(drudle);
    while (matcher.find()) {
      List<String> matchingParts = new ArrayList<>();
      List<String> usedParts = new ArrayList<>();
      final Map<String, String> valuesToParam = new HashMap<>();
      Set<String> groupNames = new HashSet<>(regex.namedGroups().keySet());
      String head = drudle.substring(0, matcher.start());
      String content = drudle.substring(matcher.start(), matcher.end());
      String tail = drudle.substring(matcher.end());

      groupNames.add("head");
      groupNames.add("content");
      groupNames.add("tail");
      Function<String, String> getValue =
          (String name) -> {
            if (name.equals("head")) return head;
            if (name.equals("content")) return content;
            if (name.equals("tail")) return tail;
            return matcher.group(name);
          };
      for (var groupName : groupNames) {
        String value = getValue.apply(groupName);
        if (output.contains("{" + groupName + "}")) {
          if (output.contains("{" + groupName + "}*") && (value == null || value.isEmpty())) {
              log.info(
                  "RegexRule '{}' drudle '{}': value is missing for group: {}",
                  name,
                  drudle,
                  groupName);
                break;
            }


          usedParts.add(value);
        }
        valuesToParam.put(value, groupName);
        matchingParts.add(value);
      }
      if (matchingParts.stream().mapToInt(String::length).sum() == drudle.length()) {
        DrudleRuleResult newDrudle =
            new DrudleRuleResult(
                name,
                drudle,
                matchingParts,
                usedParts,
                parts -> {
                  Map<String, String> mappedParts =
                      parts.entrySet().stream()
                          .collect(
                              toMap(
                                  entry -> valuesToParam.get(entry.getKey()), Map.Entry::getValue));
                  return getOutput(mappedParts);
                });
        ret.add(newDrudle);
      } else {
        log.info(
            "RegexRule '{}' did not use all parts. Drudle: '{}', matched parts: {}",
            name,
            drudle,
            matchingParts);
      }
    }
    return ret;
  }

  private String getOutput(Map<String, String> fulfilledParts) {
    var result = output.replace("*", "");
    for (var entry : fulfilledParts.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }
}
