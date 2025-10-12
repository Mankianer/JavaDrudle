package de.mankianer.drudle;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OutputRenderer {
  private String htmlTemplate =
"""

<div id="canvas" style="aspect-ratio: 1 / 1; width: 25em; background: #ffffff; border: 0.2em solid #000000;
            display: grid; place-items: center; container-type: size; overflow: hidden;">
    <div id="canvas-content" style="
      /* font-size shrink/grow with canvas width */
      font-size: 4em;
      font-family: Arial;
      /* Row layout, no wrapping */
      display: flex;
      flex-wrap: nowrap;
      align-items: center;
      justify-content: center;
      /* Keep all in one line */
      white-space: nowrap;
      /* Prevent overflow drawing */
      overflow: hidden;">
        %s
    </div>
<script>
    (function() {
        const canvas = document.getElementById("canvas");
        const content = document.getElementById("canvas-content");
        canvas.id = "";
        content.id = "";

        function fitContent() {
            if (!canvas || !content) return;

            // Reset font-size to a maximum starting value
            let match = content.style.fontSize.match(/^([\\d.]+)(em)?$/);

            let fontSize;
            if (match && match[2] === "em") {
                fontSize = parseFloat(match[1]);
            } else {
                fontSize = 4;
                content.style.fontSize = fontSize + "em";
            }

            // Shrink until it fits inside the canvas
            while (
                (content.scrollWidth > canvas.clientWidth ||
                    content.scrollHeight > canvas.clientHeight) &&
                fontSize > 0.5
                ) {
                fontSize -= 0.1;
                content.style.fontSize = fontSize + "em";
            }
        }

        // Initial fit
        fitContent();

        // Re-fit when window resizes
        //window.addEventListener("resize", fitContent);
    })();
</script>
</div>

    """;

  public String render(String drudle, Set<String> result) {


    StringBuilder ret = new StringBuilder();
      ret.append("<h2>Input: %s</h2>".formatted(drudle));
    for (String res : result) {
        ret.append(htmlTemplate.formatted(res));
    }

    return ret.toString();
  }
}
