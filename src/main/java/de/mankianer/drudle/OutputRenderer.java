package de.mankianer.drudle;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OutputRenderer {

  public String render(String drudle, Set<String> result) {

    String htmlTemplate = """
    
    <div style="background-color: white; border: 0.4em solid black; aspect-ratio: 1 / 1; width: 25em; display: flex; align-items: center; justify-content: center; font-family: Arial;">
        <div style="display: flex; align-items: center; justify-content: center; font-size: 4em;">
            %s
        </div>
    </div>
    
    """;

    StringBuilder ret = new StringBuilder();
      ret.append("<h2>Results: %s</h2>".formatted(drudle));
    for (String res : result) {
        ret.append(htmlTemplate.formatted(res));
    }

    return ret.toString();
  }
}
