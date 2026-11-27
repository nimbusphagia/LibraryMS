package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.MalagaCode;
import com.nimbusphagia.library.catalog.internal.entity.MalagaEntry;
import com.nimbusphagia.library.catalog.internal.repository.MalagaEntryRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Finds the Malaga codes for a surname. The printed table mixes orders: most entries follow the
 * modern one, but some (Ach, All, Echa, Gall, Oll, Valla) treat ch and ll as letters and particles
 * (De la, De L) follow none. So it returns both the greatest entry not after the name and the
 * longest entry that is a prefix of it, with every row of each.
 */
@Component
@RequiredArgsConstructor
class MalagaLookup {
  private final MalagaEntryRepository repository;

  List<MalagaCode> codesFor(String name) {
    String key = normalize(name);
    if (key.isEmpty()) {
      return List.of();
    }
    String letter = blockOf(key);
    List<MalagaEntry> block = repository.findByLetterOrderById(letter);

    String floor = null;
    String prefix = null;
    for (MalagaEntry row : block) {
      String entry = normalize(row.getEntry());
      if (compare(entry, key) <= 0 && (floor == null || compare(entry, floor) > 0)) {
        floor = entry;
      }
      if (key.startsWith(entry) && (prefix == null || entry.length() > prefix.length())) {
        prefix = entry;
      }
    }

    List<MalagaCode> codes = new ArrayList<>();
    for (String chosen : Stream.of(floor, prefix).filter(Objects::nonNull).distinct().toList()) {
      for (MalagaEntry row : block) {
        if (normalize(row.getEntry()).equals(chosen)) {
          codes.add(new MalagaCode(letter + row.getNumber(), row.getEntry(), row.getQualifier()));
        }
      }
    }
    return codes;
  }

  // Lowercase, without accents except the ñ, without apostrophes, keeping single spaces
  static String normalize(String text) {
    if (text == null) {
      return "";
    }
    String lower = text.toLowerCase(Locale.ROOT).replace('ñ', '\u0001');
    String plain =
        Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('\u0001', 'ñ')
            .replaceAll("['’]", "")
            .replaceAll("[^a-zñ ]", " ");
    return plain.strip().replaceAll(" +", " ");
  }

  private static String blockOf(String key) {
    if (key.startsWith("ch")) return "Ch";
    if (key.startsWith("ll")) return "Ll";
    return key.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  // Modern order: a space before any letter, ñ right after n
  private static int compare(String a, String b) {
    for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
      int diff = weight(a.charAt(i)) - weight(b.charAt(i));
      if (diff != 0) {
        return diff;
      }
    }
    return a.length() - b.length();
  }

  private static int weight(char c) {
    if (c == ' ') return 0;
    if (c == 'ñ') return 'n' - 'a' + 2;
    return c <= 'n' ? c - 'a' + 1 : c - 'a' + 2;
  }
}
