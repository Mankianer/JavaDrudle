package de.mankianer.drudle;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
class DrudleServiceImpl implements DrudleService {

  private final List<DrudleRule> rules;

  DrudleServiceImpl() {
    this.rules = new ArrayList<>();
  }

  @PostConstruct
  private void init() {
    // Example rule
    rules.add(
        new RegexRule(
            "exampleRule",
            "zwei",
            "{head}({tail}*,{tail}*)",
            "macht aus zwei-X, doppel paar."));
    rules.add(
      new RegexRule(
              "exampleRule",
              "drei",
              "{head}({tail}*,{tail}*,{tail}*)",
              "macht aus zwei-X, doppel paar."));
  }

  public void addRules(DrudleRule ...rules) {
    this.rules.addAll(List.of(rules));
  }

  public Set<String> processDrudle(String drudle) {
    drudle = drudle.toLowerCase();
    HashMap<String, Set<String>> solved = new HashMap<>();
    Queue<DrudleRuleResult> waiting = new LinkedList<>();
    HashMap<String, List<Consumer<String>>> waitingForSolving = new HashMap<>();
    Process currentProcess = getProcess(waiting, solved, waitingForSolving);

    var result = currentProcess.addToWaitingQueue(drudle);
    if (!result) {
      return Set.of();
    }
    while (!waiting.isEmpty()) {
      DrudleRuleResult current = waiting.poll();
      if (current.isSolved()) {
        currentProcess.addToSolved(current);
        continue;
      }
      currentProcess.processUsedParts(current);
    }

    return solved.get(drudle);
  }

  private Process getProcess(Queue<DrudleRuleResult> waiting, HashMap<String, Set<String>> solved, HashMap<String, List<Consumer<String>>> waitingForSolving) {


    return new Process(
        waiting::add,
        (dr) -> solved.getOrDefault(dr, new HashSet<>()),
        (dr, output) -> {
          solved.computeIfAbsent(dr, k -> new HashSet<>()).add(output);
        },
        (part, consumers) -> {
          waitingForSolving.computeIfAbsent(part, k -> new ArrayList<>()).add(consumers);
        },
        (dr, output) -> {
          if (waitingForSolving.containsKey(dr)) {
            for (var consumer : waitingForSolving.get(dr)) {
              consumer.accept(output);
            }
          }
        });
  }

  @AllArgsConstructor
  @Data
  private class Process {
    private final Consumer<DrudleRuleResult> addWaiting;
    private final Function<String, Set<String>> getSolved;
    private final BiConsumer<String, String> addSolved;
    private final BiConsumer<String, Consumer<String>> addWaitingSolvedList;
    private final BiConsumer<String, String> alertSolvedConsumers;

    boolean addToWaitingQueue(String drudle) {
      boolean added = false;
      for (var rule : rules) {
        List<DrudleRuleResult> results = rule.apply(drudle);
        for (var result : results) {
            if (result.isValid()) {
              addWaiting.accept(result);
              added = true;
            } else {
              log.error("Rule {} did not use all parts. Drudle: '{}'", rule.getName(), drudle);
            }
        }
      }
      return added;
    }

    void addToSolved(DrudleRuleResult result) {
      addToSolved(result.getInput(), result.getOutput(), result.getRuleName());
    }

    void addToSolved(String drudle, String result, String ruleName) {
      var solved = getSolved.apply(drudle);
      if (!solved.contains(result)) {
        addSolved.accept(drudle, result);
        log.debug("Solved drudle '{}' to '{}' with rule {}", drudle, result, ruleName);
        alertSolvedConsumers.accept(drudle, result);
      }
    }

    void processUsedParts(DrudleRuleResult current) {
      for (var part : current.getUsedPartsFulfillmentConsumerMap().entrySet()) {
        // Create consumer
        Consumer<String> consumer =
            (s) -> {
              DrudleRuleResult applied = part.getValue().apply(s);
              if (applied != null) {
                addWaiting.accept(applied); // add solved drudle part
              }
            };
        // Register consumer
        addWaitingSolvedList.accept(part.getKey(), consumer);
        if (!getSolved.apply(part.getKey()).isEmpty()) {
            // Apply to already solved parts
            for (var s : getSolved.apply(part.getKey())) {
              consumer.accept(s);
            }
        } else {
          boolean isRuleApplied = addToWaitingQueue(part.getKey());
          if (!isRuleApplied) {
            addToSolved(part.getKey(), part.getKey(), "NoRule");
          }
        }
      }
    }
  }
}
