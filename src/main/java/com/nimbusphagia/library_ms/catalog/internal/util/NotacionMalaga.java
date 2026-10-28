package com.nimbusphagia.library_ms.catalog.internal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Notación interna Málaga (versión PUCP 1964) para el primer apellido del autor.
 *
 * <p>La tabla es de rangos: cada entrada es el INICIO de un rango. Si el apellido empieza con la
 * entrada, se usa su número; si cae entre dos entradas, se usa el número de la anterior más la
 * clave numérica de la 3.ª letra (regla 4).
 *
 * <p>Datos en {@code resources/malaga/tabla.csv}, formato {@code prefijo;numero}, una entrada por
 * línea.
 */
public final class NotacionMalaga {

  /** Letras a..z → clave numérica (A-B=1 C-D=2 E-G=3 H-K=4 L-N=5 O-P=6 Q-S=7 T-U=8 V-Z=9). */
  private static final String CLAVE = "11223334444555667778899999";

  /** ch, ll, ñ como UN carácter que ordena después de 'z' (como en la tabla). */
  private static final char CH = '{', LL = '|', ENIE = '}';

  private static final Pattern PARTICULA = Pattern.compile("(de|del|y) .+");
  private static final Pattern NO_LETRA = Pattern.compile("[^a-z\\{\\|\\} ]");
  private static final Pattern ESPACIOS = Pattern.compile("\\s+");
  private static final Pattern MARCAS = Pattern.compile("\\p{M}");

  /** Rangos: búsqueda por floorEntry. */
  private static final NavigableMap<String, Integer> TABLA;

  /** "De la", "De L", "y De Los"...: no están en orden alfabético, prefijo más largo. */
  private static final Map<String, Integer> PARTICULAS;

  static {
    var tabla = new TreeMap<String, Integer>();
    var particulas = new LinkedHashMap<String, Integer>();
    try (var in = NotacionMalaga.class.getResourceAsStream("/malaga/tabla.csv")) {
      if (in == null) throw new IllegalStateException("Falta el recurso /malaga/tabla.csv");
      new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
          .lines()
          .filter(l -> !l.isBlank() && !l.startsWith("#"))
          .forEach(
              l -> {
                String[] p = l.split(";");
                String k = norm(p[0]);
                (k.indexOf(' ') > 0 ? particulas : tabla).put(k, Integer.valueOf(p[1].strip()));
              });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    TABLA = Collections.unmodifiableNavigableMap(tabla);
    PARTICULAS = Collections.unmodifiableMap(particulas);
  }

  private NotacionMalaga() {}

  public static String notacion(String apellido) {
    return notacion(apellido, 1);
  }

  /**
   * @param ordinal 1 = notación de la tabla; 2, 3... = autores homónimos (regla 5: clave de la 4.ª,
   *     5.ª... letra).
   */
  public static String notacion(String apellido, int ordinal) {
    if (ordinal < 1) throw new IllegalArgumentException("ordinal debe ser >= 1");
    String n = norm(apellido);
    if (n.isEmpty()) throw new IllegalArgumentException("Apellido inválido: " + apellido);

    String letras = n.replace(" ", "");
    Hallazgo h = buscar(n);

    StringBuilder sb = new StringBuilder(inicial(n.charAt(0))).append(h.numero());
    // Regla 4: no está en la tabla → clave de la 3.ª letra (solo tablas de 2 dígitos)
    if (!h.exacto() && h.numero() >= 10 && letras.length() > 2) {
      sb.append(clave(letras.charAt(2)));
    }
    // Regla 5: homónimos
    if (ordinal > 1) {
      if (ordinal + 1 >= letras.length()) {
        throw new IllegalArgumentException("Apellido muy corto para el autor n.º " + ordinal);
      }
      sb.append(clave(letras.charAt(ordinal + 1)));
    }
    return sb.toString();
  }

  private record Hallazgo(int numero, boolean exacto) {}

  private static Hallazgo buscar(String n) {
    if (PARTICULA.matcher(n).matches()) {
      Map.Entry<String, Integer> mejor = null;
      for (var e : PARTICULAS.entrySet()) {
        if (coincide(n, e.getKey())
            && (mejor == null || e.getKey().length() > mejor.getKey().length())) {
          mejor = e;
        }
      }
      if (mejor != null) return new Hallazgo(mejor.getValue(), true);
    }
    String s = n.replace(" ", "");
    var e = TABLA.floorEntry(s);
    // antes de la primera entrada de su letra → usar la siguiente (regla 4: "anterior o superior")
    if (e == null || e.getKey().charAt(0) != s.charAt(0)) e = TABLA.ceilingEntry(s);
    if (e == null) throw new IllegalArgumentException("Sin entrada en la tabla para: " + n);
    return new Hallazgo(e.getValue(), s.startsWith(e.getKey()));
  }

  /** "de la" ≠ "de lara": si la clave termina en palabra completa, exige límite de palabra. */
  private static boolean coincide(String n, String k) {
    if (!n.startsWith(k)) return false;
    boolean palabraCompleta = k.length() - k.lastIndexOf(' ') - 1 > 1;
    return !palabraCompleta || n.length() == k.length() || n.charAt(k.length()) == ' ';
  }

  private static String inicial(char c) {
    return switch (c) {
      case CH -> "Ch";
      case LL -> "Ll";
      case ENIE -> "Ñ";
      default -> String.valueOf(Character.toUpperCase(c));
    };
  }

  private static int clave(char c) {
    return switch (c) {
      case CH -> 2; // C
      case LL, ENIE -> 5; // L, N
      default -> CLAVE.charAt(c - 'a') - '0';
    };
  }

  /** Minúsculas, sin tildes (conserva ñ), ch/ll/ñ → un solo carácter, solo [a-z] y espacios. */
  static String norm(String s) {
    s = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFC).replace('ñ', ENIE);
    s = MARCAS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
    s = s.replace('-', ' ').replace("ch", String.valueOf(CH)).replace("ll", String.valueOf(LL));
    return ESPACIOS.matcher(NO_LETRA.matcher(s).replaceAll("")).replaceAll(" ").strip();
  }
}
