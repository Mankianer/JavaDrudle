package de.mankianer.drudle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DrudleServiceImplSimpleRuleTest {

    private DrudleRule rule;
    private DrudleServiceImpl service;

    @BeforeEach
    void setUp() {
        // create simple rule that does apply only to "test"
        rule = spy(new RegexRule("testRule", "test", "{head}{tail}"));
        // create drudle service with that rule
        service = new DrudleServiceImpl();
        service.addRules(rule);
    }

    @Test
    void testNotApply() {
        // apply drudle to service that does not match
        String drudle = "noMatch";
        Set<String> result = service.processDrudle(drudle).stream().map(DrudleRuleResult::getOutput).collect(Collectors.toSet());
        // assert that no result is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // assert that the rule is called once
        verify(rule, times(1)).apply(drudle.toLowerCase());
    }

    @Test
    void testOneApply() {
        // apply drudle to service that does match
        String drudle = "1test2";
        Set<String> result = service.processDrudle(drudle).stream().map(DrudleRuleResult::getOutput).collect(Collectors.toSet());
        // assert that no result is returned
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // assert that the rule is called once
        verify(rule, times(1)).apply(drudle.toLowerCase());
        verify(rule, times(1)).apply("1");
        verify(rule, times(1)).apply("2");
    }

    @Test
    void testMultiApply() {
        // apply drudle to service that does match
        String drudle = "1test2test3";
        Set<String> result = service.processDrudle(drudle).stream().map(DrudleRuleResult::getOutput).collect(Collectors.toSet());
        // assert that no result is returned
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // assert that the rule is called once
        verify(rule, times(1)).apply(drudle.toLowerCase());
        verify(rule, times(1)).apply("1");
        verify(rule, times(1)).apply("2");
        verify(rule, times(1)).apply("3");
    }
}
