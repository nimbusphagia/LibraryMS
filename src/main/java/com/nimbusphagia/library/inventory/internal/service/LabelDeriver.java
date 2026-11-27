package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.LabelPart;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Derives each part's value from catalog data, plus alternatives for when two labels collide. */
@Component
public class LabelDeriver {

  // Union of the articles used by NativeLMS and the former SpringLMS rule
  private static final Set<String> ARTICLES =
      Set.of(
          "el", "la", "los", "las", "lo", "un", "una", "unos", "unas", "de", "del", "al", "a", "en",
          "y", "e", "o", "u", "con", "por", "para", "the", "an");

  public record Options(String base, List<String> alternatives) {
    static final Options NONE = new Options(null, List.of());

    public boolean derives(String value) {
      return value.equalsIgnoreCase(base)
          || alternatives.stream().anyMatch(value::equalsIgnoreCase);
    }

    // The derived spelling of a value that matches, so case differences don't count as manual
    public String canonical(String value) {
      return value.equalsIgnoreCase(base)
          ? base
          : alternatives.stream().filter(value::equalsIgnoreCase).findFirst().orElse(value);
    }
  }

  // Every part except COPY_NUMBER, which belongs to the copy
  public Map<LabelPart, Options> derive(BookLabelData book) {
    Map<LabelPart, Options> options = new EnumMap<>(LabelPart.class);
    options.put(LabelPart.CATEGORY, only(book.categoryCode()));
    options.put(LabelPart.SUBCATEGORY, only(book.subcategoryCode()));
    options.put(LabelPart.DEWEY, only(book.deweyNumber()));
    options.put(LabelPart.AUTHOR_LETTERS, authorLetters(book));
    options.put(LabelPart.AUTHOR_MALAGA, Options.NONE);
    options.put(LabelPart.TITLE, title(book.title()));
    options.put(LabelPart.COLLECTION_VOLUME, only(book.collectionVolume()));
    options.put(LabelPart.SERIES_VOLUME, only(book.seriesVolume()));
    return options;
  }

  // The author's surname, or the title without its leading articles when there is no author
  public String malagaName(BookLabelData book) {
    if (book.authorId() != null) {
      return book.authorLastName() != null ? book.authorLastName() : book.authorFirstName();
    }
    if (book.title() == null) {
      return null;
    }
    String[] words = book.title().strip().split("\\s+");
    int start = 0;
    while (start < words.length - 1
        && ARTICLES.contains(words[start].toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", ""))) {
      start++;
    }
    return String.join(" ", Arrays.copyOfRange(words, start, words.length));
  }

  private static Options only(Object value) {
    return value == null ? Options.NONE : new Options(value.toString(), List.of());
  }

  private static Options authorLetters(BookLabelData book) {
    String name = book.authorLastName() != null ? book.authorLastName() : book.authorFirstName();
    List<String> words = significantWords(name);
    if (words.isEmpty()) {
      return Options.NONE;
    }
    String joined = String.join("", words);
    return new Options(letters(joined, 3), distinctFrom(letters(joined, 3), letters(joined, 4)));
  }

  // "Yo, robot" -> YOR; alternatives start at the 2nd or 3rd significant word
  private static Options title(String title) {
    List<String> words = significantWords(title);
    if (words.isEmpty()) {
      return Options.NONE;
    }
    String base = letters(String.join("", words), 3);
    List<String> alternatives = new ArrayList<>();
    for (int start = 1; start <= 2 && start < words.size(); start++) {
      alternatives.addAll(
          distinctFrom(base, letters(String.join("", words.subList(start, words.size())), 3)));
    }
    return new Options(base, alternatives.stream().distinct().toList());
  }

  private static List<String> distinctFrom(String base, String candidate) {
    return candidate.equals(base) ? List.of() : List.of(candidate);
  }

  // Lowercase words without accents; all of them if every word is an article
  private static List<String> significantWords(String text) {
    if (text == null) {
      return List.of();
    }
    String plain =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    List<String> all = Arrays.stream(plain.split("[^a-z0-9]+")).filter(w -> !w.isEmpty()).toList();
    List<String> significant = all.stream().filter(w -> !ARTICLES.contains(w)).toList();
    return significant.isEmpty() ? all : significant;
  }

  private static String letters(String text, int length) {
    return text.substring(0, Math.min(length, text.length())).toUpperCase(Locale.ROOT);
  }
}
