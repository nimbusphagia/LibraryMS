package com.nimbusphagia.library.inventory.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

/**
 * A4 sheet of 24 labels (4 columns × 6 rows), as NativeLMS prints them: each pair is a cell with
 * the copy id and, beside it, the vertical label with one layout line per row. Every cell has a
 * border to cut along.
 */
public final class LabelSheet {

  public record Entry(long copyId, List<String> lines) {}

  private static final float MM = 72 / 25.4f;
  private static final float MARGIN = 10 * MM;
  private static final float ID_WIDTH = 15 * MM;
  private static final float LABEL_WIDTH = 25 * MM;
  private static final float LABEL_HEIGHT = 40 * MM;
  private static final float GAP = 4 * MM;
  private static final int COLUMNS = 4;
  private static final int ROWS = 6;
  private static final float MAX_FONT = 14;
  private static final float MAX_ID_FONT = 9;
  private static final float FOOTER_FONT = 8;
  private static final float PADDING = 3;
  private static final float LINE_HEIGHT = 1.25f;
  private static final float BORDER = 0.6f;

  private static final PDType1Font REGULAR = new PDType1Font(FontName.HELVETICA);
  private static final PDType1Font BOLD = new PDType1Font(FontName.HELVETICA_BOLD);

  private LabelSheet() {}

  // The footer gets the page number after it
  public static byte[] render(List<Entry> entries, String footer) {
    List<Entry> printable =
        entries.stream()
            .map(e -> new Entry(e.copyId(), e.lines().stream().map(LabelSheet::printable).toList()))
            .toList();
    float fontSize = fontSize(printable);
    int perPage = COLUMNS * ROWS;
    int pages = (printable.size() + perPage - 1) / perPage;
    float gridWidth = COLUMNS * (ID_WIDTH + LABEL_WIDTH) + (COLUMNS - 1) * GAP;
    float originX = (PDRectangle.A4.getWidth() - gridWidth) / 2;
    float top = PDRectangle.A4.getHeight() - MARGIN;

    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      document.getDocumentInformation().setTitle("Tejuelos");
      for (int p = 0; p < pages; p++) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
          content.setLineWidth(BORDER);
          List<Entry> onPage =
              printable.subList(p * perPage, Math.min(printable.size(), (p + 1) * perPage));
          for (int i = 0; i < onPage.size(); i++) {
            float x = originX + (i % COLUMNS) * (ID_WIDTH + LABEL_WIDTH + GAP);
            float bottom = top - (i / COLUMNS) * (LABEL_HEIGHT + GAP) - LABEL_HEIGHT;
            drawPair(content, x, bottom, onPage.get(i), fontSize);
          }
          text(content, REGULAR, FOOTER_FONT, originX, MARGIN * 0.7f,
              printable(footer + "  ·  Página " + (p + 1) + " de " + pages));
        }
      }
      document.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // One size for the whole sheet: the one the most demanding label fits
  private static float fontSize(List<Entry> entries) {
    float size = MAX_FONT;
    for (Entry entry : entries) {
      float widest = 0;
      for (String line : entry.lines()) widest = Math.max(widest, width(BOLD, line, 1));
      if (widest > 0) size = Math.min(size, (LABEL_WIDTH - 2 * PADDING) / widest);
      size = Math.min(size, (LABEL_HEIGHT - 2 * PADDING) / (entry.lines().size() * LINE_HEIGHT));
    }
    return size;
  }

  private static void drawPair(
      PDPageContentStream content, float x, float bottom, Entry entry, float fontSize)
      throws IOException {
    content.addRect(x, bottom, ID_WIDTH, LABEL_HEIGHT);
    content.addRect(x + ID_WIDTH, bottom, LABEL_WIDTH, LABEL_HEIGHT);
    content.stroke();

    String id = "#" + entry.copyId();
    float idSize = Math.min(MAX_ID_FONT, (ID_WIDTH - 2 * PADDING) / width(REGULAR, id, 1));
    text(content, REGULAR, idSize, x + (ID_WIDTH - width(REGULAR, id, idSize)) / 2,
        bottom + LABEL_HEIGHT / 2 - idSize * 0.35f, id);

    float lineHeight = fontSize * LINE_HEIGHT;
    float blockTop = bottom + (LABEL_HEIGHT + entry.lines().size() * lineHeight) / 2;
    for (int i = 0; i < entry.lines().size(); i++) {
      String line = entry.lines().get(i);
      text(content, BOLD, fontSize,
          x + ID_WIDTH + (LABEL_WIDTH - width(BOLD, line, fontSize)) / 2,
          blockTop - (i + 1) * lineHeight + (lineHeight - fontSize) / 2 + fontSize * 0.1f, line);
    }
  }

  private static void text(
      PDPageContentStream content, PDType1Font font, float size, float x, float y, String text)
      throws IOException {
    content.beginText();
    content.setFont(font, size);
    content.newLineAtOffset(x, y);
    content.showText(text);
    content.endText();
  }

  private static float width(PDType1Font font, String text, float size) {
    try {
      return font.getStringWidth(text) / 1000 * size;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // The standard fonts only cover Latin-1 (WinAnsi); anything else would make PDFBox throw
  private static String printable(String text) {
    StringBuilder out = new StringBuilder();
    text.codePoints()
        .forEach(
            cp -> {
              String c = Character.toString(cp);
              try {
                BOLD.encode(c);
                out.append(c);
              } catch (IllegalArgumentException | IOException e) {
                out.append('?');
              }
            });
    return out.toString();
  }
}
