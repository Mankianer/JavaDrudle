package de.mankianer.drudle;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/drudle")
class DrudleController {

    private final DrudleService drudleService;

    public DrudleController(DrudleService drudleService) {
        this.drudleService = drudleService;
    }

    @RequestMapping("/{drudle}")
    public String getDrudle(@PathVariable String drudle) {
        return drudleService.processDrudle(drudle);
    }
}
