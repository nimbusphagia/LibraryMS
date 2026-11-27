package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.catalog.LabelPart;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.stream.Stream;

/** Lines of a label, top to bottom; stored as jsonb in label_scheme. */
public record LabelLayout(@NotEmpty @Valid List<Line> lines) {

  public record Line(@Size(max = 5) String separator, @NotEmpty @Valid List<PartRule> parts) {
    public Line {
      separator = separator == null ? "" : separator;
    }
  }

  public record PartRule(
      @NotNull LabelPart part, boolean required, boolean editable, @Size(max = 10) String prefix) {
    public PartRule {
      prefix = prefix == null || prefix.isBlank() ? null : prefix.strip();
    }
  }

  public Stream<PartRule> rules() {
    return lines.stream().flatMap(line -> line.parts().stream());
  }
}
