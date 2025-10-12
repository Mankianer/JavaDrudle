package de.mankianer.drudle;

import de.mankianer.drudle.DrudleRuleResult.DrudleRuleResultSolved;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Log4j2
@Service
class DrudleServiceImpl implements DrudleService {

  private final List<DrudleRule> rules = new ArrayList<>();

  @PostConstruct
  private void init() throws IOException {
    loadYamlRules();
  }

  private void loadYamlRules() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:rules/*.yaml");
    for (Resource res : resources) {
      loadYamlRule(res)
          .forEach(
              (rule) -> {
                log.info("Found Rule: {}", rule.getName());
                rules.add(rule);
              });
    }
    log.info("Loaded {} rules", rules.size());
  }

  private List<RegexRule> loadYamlRule(Resource resource) throws IOException {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> load = loader.load(resource.getFilename(), resource);
    return load.stream()
        .map(
            (yp) -> {
              return new RegexRule(
                  resource.getFilename() + "-" + ((String) yp.getProperty("name")),
                  (String) yp.getProperty("pattern"),
                  (String) yp.getProperty("output"),
                  (String) yp.getProperty("description"));
            })
        .toList();
  }

  void addRules(DrudleRule... rules) {
    this.rules.addAll(List.of(rules));
  }

  public Set<DrudleRuleResult> processDrudle(String drudle) {
    drudle = drudle.toLowerCase();
    HashMap<String, Set<DrudleRuleResult>> solved = new HashMap<>();
    Queue<DrudleRuleResult> waiting = new LinkedList<>();
    HashMap<String, List<Consumer<DrudleRuleResult>>> waitingForSolving = new HashMap<>();
    Process currentProcess = getProcess(waiting, solved, waitingForSolving);

    var result = currentProcess.addToWaitingQueue(drudle);
    if (!result) {
      return Set.of();
    }
    while (!waiting.isEmpty()) {
      DrudleRuleResult current = waiting.poll();
      var currentSolved = current.getSolvedResult();
      if (currentSolved != null) {
        currentProcess.addToSolved(currentSolved);
        continue;
      }
      currentProcess.processUsedParts(current);
    }

    return solved.get(drudle);
  }

  private Process getProcess(
      Queue<DrudleRuleResult> waiting,
      HashMap<String, Set<DrudleRuleResult>> solved,
      HashMap<String, List<Consumer<DrudleRuleResult>>> waitingForSolving) {

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
    private final Function<String, Set<DrudleRuleResult>> getSolved;
    private final BiConsumer<String, DrudleRuleResultSolved> addSolved;
    private final BiConsumer<String, Consumer<DrudleRuleResult>> addWaitingSolvedList;
    private final BiConsumer<String, DrudleRuleResult> alertSolvedConsumers;

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

    void addToSolved(DrudleRuleResultSolved result) {
      var drudle = result.getInput();
      var solved = getSolved.apply(drudle);
      if (!solved.contains(result)) {
        addSolved.accept(drudle, result);
        log.debug("Solved drudle '{}' to '{}' with rule {}", drudle, result, result.getRuleName());
        alertSolvedConsumers.accept(drudle, result);
      }
    }

    void processUsedParts(DrudleRuleResult current) {
      for (var part : current.getUsedPartsFulfillmentConsumerMap().entrySet()) {
        // Create consumer
        Consumer<DrudleRuleResult> consumer =
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
          addToWaitingQueue(part.getKey());
          addToSolved(new DrudleRuleResultSolved(part.getKey()));
        }
      }
    }
  }
}
