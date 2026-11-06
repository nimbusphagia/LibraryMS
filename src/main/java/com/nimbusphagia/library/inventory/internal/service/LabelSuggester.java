package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelParts;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestionSource;
import com.nimbusphagia.library.inventory.internal.util.LabelTextComposer;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LabelSuggester {

  private static final Set<String> LEADING_ARTICLES =
      Set.of(
          "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en",
          "con", "por", "para", "the", "an");

  public LabelSuggestion suggest(BookLabelData book, int copyNumber) {
    LabelParts parts =
        new LabelParts(
            book.categoryCode(),
            deriveTitleCode(book.title()),
            book.deweyCode(),
            null,
            book.collectionVolume(),
            book.seriesVolume());

    return new LabelSuggestion(
        parts,
        copyNumber,
        LabelTextComposer.compose(parts, copyNumber),
        LabelSuggestionSource.DERIVED);
  }

  public LabelSuggestion fromExisting(LabelParts parts, int copyNumber) {
    return new LabelSuggestion(
        parts,
        copyNumber,
        LabelTextComposer.compose(parts, copyNumber),
        LabelSuggestionSource.EXISTING_LABEL);
  }

  private static String deriveTitleCode(String title) {
    if (title == null || title.isBlank()) return null;

    String normalized =
        Normalizer.normalize(title.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");

    StringBuilder allWords = new StringBuilder();

    for (String word : normalized.split("\\s+")) {
      String cleaned = word.replaceAll("[^\\p{L}\\p{N}]", "");
      if (cleaned.isEmpty()) continue;

      allWords.append(cleaned);

      if (!LEADING_ARTICLES.contains(cleaned.toLowerCase(Locale.ROOT))) {
        return firstThree(cleaned);
      }
    }

    return allWords.length() == 0 ? null : firstThree(allWords.toString());
  }

  private static String firstThree(String text) {
    String code = text.length() > 3 ? text.substring(0, 3) : text;
    return code.toUpperCase(Locale.ROOT);
  }
}
