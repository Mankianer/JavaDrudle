package de.mankianer.drudle;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
@Service
class DrudleService {

  private final List<DrudleRule> rules;

  DrudleService() {
    this.rules = new ArrayList<>();
  }

  @PostConstruct
  private void init() {
    // Example rule
    rules.add(
        new RegexRule(
            "exampleRule",
            "(?<zwei>zwei)(?<x>\\w+)",
            "({x},{x})",
            "macht aus zwei-X, doppel paar."));
  }

  public String processDrudle(String drudle) {
    drudle = drudle.toLowerCase();
    HashMap<String, Set<String>> solved = new HashMap<>();
    Queue<DrudleRuleResult> waiting = new LinkedList<>();
  HashMap<String, List<Consumer<String>>> waitingForSolving = new HashMap<>();
    Process currentProcess = getProcess(waiting, solved, waitingForSolving);

    var result = currentProcess.addToWaitingQueue(drudle);
    if (!result) {
      return "No rule applied to drudle: %s".formatted(drudle);
    }
    while (!waiting.isEmpty()) {
      DrudleRuleResult current = waiting.poll();
      if (current.isSolved()) {
        currentProcess.addToSolved(current);
        continue;
      }
      // Not solved yet, try to fulfill parts
      // Find part and register solved consumers and add part to waiting queue
      currentProcess.processUsedParts(current);
    }

    return "Processed drudle: %s%nResult: %s".formatted(drudle, solved.get(drudle));
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
        var result = rule.apply(drudle);
        if (result != null && result.isValid()) {
          addWaiting.accept(result);
          added = true;
        } else if (result != null) {
          log.error("Rule {} did not use all parts. Drudle: '{}'", rule.getName(), drudle);
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
        log.info("Solved drudle '{}' to '{}' with rule {}", drudle, result, ruleName);
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
        // Apply to already solved parts
        for (var s : getSolved.apply(part.getKey())) {
          consumer.accept(s);
        }
        boolean isRuleApplied = addToWaitingQueue(part.getKey());
        if (!isRuleApplied) {
          addToSolved(part.getKey(), part.getKey(), "NoRule");
        }
      }
    }
  }
}
