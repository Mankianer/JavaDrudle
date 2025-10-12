package de.mankianer.drudle;

import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drudle")
class DrudleController {

    private final DrudleService drudleService;
    private final OutputRenderer outputRenderer;

    public DrudleController(DrudleService drudleService, OutputRenderer outputRenderer) {
        this.drudleService = drudleService;
        this.outputRenderer = outputRenderer;
    }

    @RequestMapping("/{drudle}")
    public String getDrudle(@PathVariable String drudle) {
        Set<DrudleRuleResult> result = drudleService.processDrudle(drudle);
        if (result.isEmpty()) {
            return "No rule applied to drudle: %s".formatted(drudle);
        }
        return outputRenderer.render(drudle, result);
    }
}
