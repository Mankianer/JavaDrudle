package de.mankianer.drudle;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RegexRuleTest {

  @Test
  void singleMatchWithFillingTest() {
    RegexRule rule = new RegexRule("testRule", "test", "{head}-{tail}");
    List<DrudleRuleResult> results = rule.apply("1test2");
    assertNotNull(results);
    assertEquals(1, results.size());
    // full validation of DrudleRuleResult
    DrudleRuleResult result = results.get(0);
    assertTrue(result.isValid());
    assertFalse(result.isSolved());
    Map<String, Function<String, DrudleRuleResult>> usedPartsFulfillmentConsumerMap =
        result.getUsedPartsFulfillmentConsumerMap();
    assertNotNull(usedPartsFulfillmentConsumerMap);
    assertEquals(2, usedPartsFulfillmentConsumerMap.size());
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("1"));
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("2"));
    // fill used parts
    DrudleRuleResult headResult = usedPartsFulfillmentConsumerMap.get("1").apply("CLICK");
    assertNull(headResult);
    DrudleRuleResult tailResult = usedPartsFulfillmentConsumerMap.get("2").apply("CLACK");
    assertNotNull(tailResult);
    assertTrue(tailResult.isValid());
    assertTrue(tailResult.isSolved());
    assertEquals("CLICK-CLACK", tailResult.getOutput());
  }

  @Test
  void multiMatchTest() {
    RegexRule rule = new RegexRule("testRule", "test", "{head}-{tail}");
    List<DrudleRuleResult> results = rule.apply("test1test2test3test4test");
    assertNotNull(results);
    // full validation of DrudleRuleResult
    List<Set<String>> splitValues = new ArrayList<>();
    results.forEach(
        result -> {
          assertTrue(result.isValid());
          assertFalse(result.isSolved());
          Map<String, Function<String, DrudleRuleResult>> usedPartsFulfillmentConsumerMap =
              result.getUsedPartsFulfillmentConsumerMap();
          assertNotNull(usedPartsFulfillmentConsumerMap);
          assertEquals(2, usedPartsFulfillmentConsumerMap.size());
          // fill used parts
          splitValues.add(usedPartsFulfillmentConsumerMap.keySet());
        });
    assertThat(
            List.of(
                Set.of("", "1test2test3test4test"),
                Set.of("test1", "2test3test4test"),
                Set.of("test1test2", "3test4test"),
                Set.of("test1test2test3", "4test"),
                Set.of("test1test2test3test4", "")))
        .hasSameElementsAs(splitValues);
  }

  @Test
  void multiMatchNeededTest() {
    RegexRule rule = new RegexRule("testRule", "test", "{head}*-{tail}*");
    List<DrudleRuleResult> results = rule.apply("test1test2test3test4test");
    assertNotNull(results);
    // full validation of DrudleRuleResult
    List<Set<String>> splitValues = new ArrayList<>();
    results.forEach(
        result -> {
          assertTrue(result.isValid());
          assertFalse(result.isSolved());
          Map<String, Function<String, DrudleRuleResult>> usedPartsFulfillmentConsumerMap =
              result.getUsedPartsFulfillmentConsumerMap();
          assertNotNull(usedPartsFulfillmentConsumerMap);
          assertEquals(2, usedPartsFulfillmentConsumerMap.size());
          // fill used parts
          splitValues.add(usedPartsFulfillmentConsumerMap.keySet());
        });
    assertThat(
            List.of(
                Set.of("test1", "2test3test4test"),
                Set.of("test1test2", "3test4test"),
                Set.of("test1test2test3", "4test")))
        .hasSameElementsAs(splitValues);
  }

  @Test
  void noMatchTest() {
    RegexRule rule = new RegexRule("testRule", "test", "{head}-{tail}");
    List<DrudleRuleResult> results = rule.apply("noMatch");
    assertNotNull(results);
    assertEquals(0, results.size());
  }

  @Test
  void namedGroupTest() {
    RegexRule rule =
        new RegexRule("testRule", "id(?<key>\\w+):(?<val>\\w+)end", "{head}{val}-{key}{tail}");
    List<DrudleRuleResult> results = rule.apply("blaidtest:valenddawblu");
    assertNotNull(results);
    assertEquals(1, results.size());
    // full validation of DrudleRuleResult
    DrudleRuleResult result = results.get(0);
    assertTrue(result.isValid());
    assertFalse(result.isSolved());
    Map<String, Function<String, DrudleRuleResult>> usedPartsFulfillmentConsumerMap =
        result.getUsedPartsFulfillmentConsumerMap();
    assertNotNull(usedPartsFulfillmentConsumerMap);
    assertEquals(4, usedPartsFulfillmentConsumerMap.size());
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("bla"));
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("test"));
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("val"));
    assertTrue(usedPartsFulfillmentConsumerMap.containsKey("dawblu"));
    // fill used parts
    assertNull(usedPartsFulfillmentConsumerMap.get("bla").apply("1"));
    assertNull(usedPartsFulfillmentConsumerMap.get("test").apply("2"));
    assertNull(usedPartsFulfillmentConsumerMap.get("val").apply("3"));
    DrudleRuleResult tailResult = usedPartsFulfillmentConsumerMap.get("dawblu").apply("4");
    assertNotNull(tailResult);
    assertTrue(tailResult.isValid());
    assertTrue(tailResult.isSolved());
    assertEquals("13-24", tailResult.getOutput());
  }
}
