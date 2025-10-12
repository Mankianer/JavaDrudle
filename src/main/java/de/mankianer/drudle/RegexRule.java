package de.mankianer.drudle;

import static java.util.stream.Collectors.toMap;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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
      final Map<String, Set<String>> valuesToParam = new HashMap<>();
      List<String> groupNames =
          new ArrayList<>(
              regex.namedGroups().keySet().stream()
                  .sorted(Comparator.comparingInt(matcher::start))
                  .toList());
      boolean hasSubGroups = !groupNames.isEmpty();
      String head = drudle.substring(0, matcher.start());
      String content = drudle.substring(matcher.start(), matcher.end());
      List<String> subContents = new ArrayList<>();
      String tail = drudle.substring(matcher.end());
      // fill regex groups and head/tail/content
      groupNames.add("head");
      groupNames.add("content");
      groupNames.add("tail");
      String[] contentAfter = {content};
      UnaryOperator<String> getValue =
          (String name) -> {
            if (name.equals("head")) return head;
            if (name.equals("content")) return content;
            if (name.equals("tail")) return tail;
            String value = matcher.group(name);
            int val_start = matcher.start(name);
            int val_end = matcher.end(name);
            String contentBefore =
                contentAfter[0].substring(
                    0, val_start - matcher.start() - (content.length() - contentAfter[0].length()));
            contentAfter[0] = contentAfter[0].substring(contentBefore.length() + value.length());
            subContents.add(contentBefore);
            return value;
          };
      boolean missingGroup = false;
      for (var groupName : groupNames) {
        String value = getValue.apply(groupName);
        if (output.contains("{" + groupName + "}")) {
          if (output.contains("{" + groupName + "}*") && value.isEmpty()) {
            log.info(
                "RegexRule '{}' drudle '{}': value is missing for group: {}",
                name,
                drudle,
                groupName);
            missingGroup = true;
            break;
          }

          usedParts.add(value);
          Set<String> set = valuesToParam.getOrDefault(value, new HashSet<>());
          set.add(groupName);
          valuesToParam.put(value, set);
        }
        matchingParts.add(value);
      }
      // split content by subgroups if needed
      if (hasSubGroups) {
        matchingParts.remove(content);
        subContents.add(contentAfter[0]);
        matchingParts.addAll(subContents);
      }
      // precheck if all parts are used
      if (matchingParts.stream().mapToInt(String::length).sum() == drudle.length() && !missingGroup) {
        DrudleRuleResult newDrudle =
            new DrudleRuleResult(
                name,
                drudle,
                matchingParts,
                usedParts,
                parts -> {
                  Map<String, String> mappedParts =
                      parts.entrySet().stream()
                          .filter(entry -> valuesToParam.containsKey(entry.getKey()))
                          .flatMap(
                              entry ->
                                  valuesToParam.get(entry.getKey()).stream()
                                      .map(v -> Map.entry(v, entry.getValue())))
                          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
                  return getOutput(mappedParts);
                });
        ret.add(newDrudle);
      } else {
        log.info(
            "RegexRule '{}' did not use all parts with pattern '{}'. Drudle: '{}', matched parts: {}",
            name,
            pattern,
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
