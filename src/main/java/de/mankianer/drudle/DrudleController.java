package de.mankianer.drudle;

import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drudle")
class DrudleController {

    private final DrudleService drudleService;

    public DrudleController(DrudleService drudleService) {
        this.drudleService = drudleService;
    }

    @RequestMapping("/{drudle}")
    public String getDrudle(@PathVariable String drudle) {
        String ret;
        Set<String> result = drudleService.processDrudle(drudle);
        if (result.isEmpty()) {
            ret = "No rule applied to drudle: %s".formatted(drudle);
        } else {
            ret = "Processed drudle: %s%nResult: %n[%s]".formatted(drudle, String.join("|\n", result ));
        }
        return ret;
    }
}
