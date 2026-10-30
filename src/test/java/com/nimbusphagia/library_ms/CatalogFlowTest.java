package com.nimbusphagia.library_ms;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class CatalogFlowTest extends IntegrationTestBase {

  private static final long MISSING = 999_999L;
  private static final RequestPostProcessor ANON = request -> request;

  private static RequestPostProcessor staff() {
    return user("staff").roles("STAFF");
  }

  private static RequestPostProcessor admin() {
    return user("admin").roles("ADMIN");
  }

  private static String json(Object... kv) {
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < kv.length; i += 2) {
      Object v = kv[i + 1];
      if (v == null) continue;
      if (sb.length() > 1) sb.append(',');
      sb.append('"')
          .append(kv[i])
          .append("\":")
          .append(v instanceof String s ? "\"" + s + "\"" : v);
    }
    return sb.append('}').toString();
  }

  private ResultActions doGet(String url, RequestPostProcessor who) throws Exception {
    return mockMvc.perform(get(url).with(who));
  }

  private ResultActions doPost(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        post(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ResultActions doPut(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        put(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ResultActions doPatch(String url, RequestPostProcessor who) throws Exception {
    return mockMvc.perform(patch(url).with(who).with(csrf()));
  }

  private ResultActions doDelete(String url, RequestPostProcessor who) throws Exception {
    return mockMvc.perform(delete(url).with(who).with(csrf()));
  }

  private long idOf(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long created(String url, String body) throws Exception {
    return idOf(doPost(url, staff(), body).andExpect(status().isCreated()));
  }

  private long author(String first, String last) throws Exception {
    return created("/api/authors", json("firstName", first, "lastName", last));
  }

  private long publisher(String name) throws Exception {
    return created("/api/publishers", json("name", name));
  }

  private long collection(long publisherId, String name) throws Exception {
    return created("/api/publishers/" + publisherId + "/collections", json("name", name));
  }

  private long tag(String name) throws Exception {
    return created("/api/tags", json("name", name));
  }

  private long dewey(String code) {
    return jdbc.queryForObject("SELECT id FROM dewey_tag WHERE code = ?", Long.class, code);
  }

  private long book(String title, String isbn, Long authorId, List<Long> tagIds) throws Exception {
    return created(
        "/api/books", json("title", title, "isbn", isbn, "authorId", authorId, "tagIds", tagIds));
  }

  // Access

  @Test
  void getAuthorsIsPublic() throws Exception {
    doGet("/api/authors", ANON).andExpect(status().isOk());
  }

  @Test
  void postAuthorWithoutSessionIs401() throws Exception {
    doPost("/api/authors", ANON, json("firstName", "Gabriel")).andExpect(status().isUnauthorized());
  }

  @Test
  void postAuthorWithSessionButWithoutCsrfIs403() throws Exception {
    mockMvc
        .perform(
            post("/api/authors")
                .with(staff())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("firstName", "Gabriel")))
        .andExpect(status().isForbidden());
  }

  @Test
  void getBooksWithoutSessionIs401() throws Exception {
    doGet("/api/books", ANON).andExpect(status().isUnauthorized());
  }

  // Author

  @Test
  void createAuthorReturns201() throws Exception {
    doPost("/api/authors", staff(), json("firstName", "Gabriel", "lastName", "Garcia"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.firstName").value("Gabriel"))
        .andExpect(jsonPath("$.lastName").value("Garcia"));
  }

  @Test
  void duplicateAuthorWithDifferentCaseIs409() throws Exception {
    author("Gabriel", "Garcia");

    doPost("/api/authors", staff(), json("firstName", "GABRIEL", "lastName", "GARCIA"))
        .andExpect(status().isConflict());
  }

  @Test
  void editAuthorUpdatesFields() throws Exception {
    long id = author("Gabriel", "Garcia");

    doPut("/api/authors/" + id, staff(), json("firstName", "Gabriel", "lastName", "Marquez"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.lastName").value("Marquez"));
  }

  @Test
  void listAuthorsFiltersBySearch() throws Exception {
    author("Gabriel", "Garcia");
    author("Isabel", "Allende");

    doGet("/api/authors?search=garcia", ANON)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].firstName").value("Gabriel"));

    doGet("/api/authors", ANON).andExpect(jsonPath("$.content", hasSize(2)));
  }

  // Delete

  @Test
  void deleteAuthorIsForbiddenForStaffAndAllowedForAdmin() throws Exception {
    long id = author("Gabriel", "Garcia");

    doDelete("/api/authors/" + id, staff()).andExpect(status().isForbidden());
    doDelete("/api/authors/" + id, admin()).andExpect(status().isNoContent());
  }

  @Test
  void deleteAuthorWithBooksIs409() throws Exception {
    long id = author("Gabriel", "Garcia");
    book("Cien anos", "9780306406157", id, null);

    doDelete("/api/authors/" + id, admin()).andExpect(status().isConflict());
  }

  // Collections

  @Test
  void collectionsAreScopedToTheirPublisher() throws Exception {
    long alfaguara = publisher("Alfaguara");
    long planeta = publisher("Planeta");
    String url = "/api/publishers/%d/collections";

    doPost(url.formatted(alfaguara), staff(), json("name", "Clasicos"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Clasicos"));

    doPost(url.formatted(alfaguara), staff(), json("name", "CLASICOS"))
        .andExpect(status().isConflict());

    doPost(url.formatted(planeta), staff(), json("name", "Clasicos"))
        .andExpect(status().isCreated());

    doGet(url.formatted(alfaguara), ANON)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  void collectionForMissingPublisherIs404() throws Exception {
    doPost("/api/publishers/" + MISSING + "/collections", staff(), json("name", "Clasicos"))
        .andExpect(status().isNotFound());
  }

  // Book - write

  @Test
  void createBookReturnsFullDetail() throws Exception {
    long author = author("Gabriel", "Garcia");
    long illustrator = author("Ana", "Lopez");
    long collaborator = author("Luis", "Perez");
    long publisher = publisher("Alfaguara");
    long collection = collection(publisher, "Clasicos");
    long tag = tag("novela");
    long dewey = dewey("800");

    long id =
        created(
            "/api/books",
            json(
                "title",
                "Cien anos de soledad",
                "description",
                "Realismo magico",
                "authorId",
                author,
                "illustratorId",
                illustrator,
                "collaboratorIds",
                List.of(collaborator),
                "publisherId",
                publisher,
                "collectionId",
                collection,
                "country",
                "CO",
                "publicationYear",
                1967,
                "pageCount",
                471,
                "isbn",
                "9780306406157",
                "deweyId",
                dewey,
                "tagIds",
                List.of(tag)));

    doGet("/api/books/" + id, staff())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Cien anos de soledad"))
        .andExpect(jsonPath("$.author.firstName").value("Gabriel"))
        .andExpect(jsonPath("$.illustrator.firstName").value("Ana"))
        .andExpect(jsonPath("$.collaborators", hasSize(1)))
        .andExpect(jsonPath("$.collaborators[0].firstName").value("Luis"))
        .andExpect(jsonPath("$.publisher.name").value("Alfaguara"))
        .andExpect(jsonPath("$.collection.name").value("Clasicos"))
        .andExpect(jsonPath("$.dewey.code").value("800"))
        .andExpect(jsonPath("$.tags", hasSize(1)))
        .andExpect(jsonPath("$.tags[0].name").value("novela"))
        .andExpect(jsonPath("$.country").value("CO"))
        .andExpect(jsonPath("$.isbn").value("9780306406157"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void duplicateIsbnIs409() throws Exception {
    book("Libro uno", "9780306406157", null, null);

    doPost("/api/books", staff(), json("title", "Libro dos", "isbn", "9780306406157"))
        .andExpect(status().isConflict());
  }

  @Test
  void collectionFromAnotherPublisherIs400() throws Exception {
    long alfaguara = publisher("Alfaguara");
    long planeta = publisher("Planeta");
    long clasicos = collection(alfaguara, "Clasicos");

    doPost(
            "/api/books",
            staff(),
            json("title", "Libro", "publisherId", planeta, "collectionId", clasicos))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unknownReferencesReturn404() throws Exception {
    doPost("/api/books", staff(), json("title", "Libro", "authorId", MISSING))
        .andExpect(status().isNotFound());
    doPost("/api/books", staff(), json("title", "Libro", "publisherId", MISSING))
        .andExpect(status().isNotFound());
    doGet("/api/books/" + MISSING, staff()).andExpect(status().isNotFound());
  }

  @Test
  void invalidBodyIs400() throws Exception {
    doPost("/api/books", staff(), json("title", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.title").exists());

    doPost("/api/books", staff(), json("title", "Libro", "isbn", "12345"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.isbn").exists());
  }

  // Book - Read

  @Test
  void listBooksFiltersBySearch() throws Exception {
    book("Cien anos de soledad", "9780306406157", null, null);
    book("El amor en los tiempos del colera", "9780306406164", null, null);

    doGet("/api/books?search=colera", staff())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("El amor en los tiempos del colera"));
  }

  @Test
  void listBooksFiltersByPersonInAnyRole() throws Exception {
    long garcia = author("Gabriel", "Garcia");
    long allende = author("Isabel", "Allende");

    book("Libro de Garcia", "9780306406157", garcia, null);
    created(
        "/api/books",
        json(
            "title",
            "Libro colaborativo",
            "authorId",
            allende,
            "collaboratorIds",
            List.of(garcia)));
    book("Libro de Allende", "9780306406164", allende, null);

    mockMvc
        .perform(get("/api/books").with(staff()).param("personId", String.valueOf(garcia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void listBooksFiltersByTags() throws Exception {
    long novela = tag("novela");
    long clasico = tag("clasico");
    book("Solo novela", "9780306406157", null, List.of(novela));
    book("Novela y clasico", "9780306406164", null, List.of(novela, clasico));

    mockMvc
        .perform(get("/api/books").with(staff()).param("tagIds", String.valueOf(novela)))
        .andExpect(jsonPath("$.content", hasSize(2)));

    // Book tags
    mockMvc
        .perform(
            get("/api/books")
                .with(staff())
                .param("tagIds", String.valueOf(novela), String.valueOf(clasico)))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Novela y clasico"));
  }

  @Test
  void deactivatedBookShowsAsInactive() throws Exception {
    long id = book("Libro viejo", "9780306406157", null, null);
    book("Libro vigente", "9780306406164", null, null);

    doPatch("/api/books/" + id + "/deactivate", staff())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    doGet("/api/books?active=false", staff())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Libro viejo"))
        .andExpect(jsonPath("$.content[0].active").value(false));

    doGet("/api/books?active=true", staff())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Libro vigente"));

    doPatch("/api/books/" + id + "/activate", staff()).andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void listBooksIsPaginated() throws Exception {
    book("Libro A", "9780306406157", null, null);
    book("Libro B", "9780306406164", null, null);
    book("Libro C", "9780306406171", null, null);

    doGet("/api/books?size=2&page=0&sort=title,asc", staff())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].title").value("Libro A"))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(2));

    doGet("/api/books?size=2&page=1&sort=title,asc", staff())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Libro C"));
  }

  // Dewey

  @Test
  void deweyEditIsAdminOnly() throws Exception {
    long id = dewey("800");
    var original = jdbc.queryForMap("SELECT name, description FROM dewey_tag WHERE id = ?", id);

    try {
      doPut("/api/dewey-tags/" + id, staff(), json("name", "Otro nombre"))
          .andExpect(status().isForbidden());

      doPut("/api/dewey-tags/" + id, admin(), json("name", "Otro nombre"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("800"))
          .andExpect(jsonPath("$.name").value("Otro nombre"));
    } finally {
      jdbc.update(
          "UPDATE dewey_tag SET name = ?, description = ? WHERE id = ?",
          original.get("name"),
          original.get("description"),
          id);
    }
  }

  // Spring MVC error

  @Test
  void nonNumericBookIdIs400() throws Exception {
    doGet("/api/books/abc", staff()).andExpect(status().isBadRequest());
  }

  @Test
  void invalidFilterValuesAre400() throws Exception {
    doGet("/api/books?active=quizas", staff()).andExpect(status().isBadRequest());
    doGet("/api/books?personId=abc", staff()).andExpect(status().isBadRequest());
  }

  @Test
  void unknownSortPropertyIs400() throws Exception {
    doGet("/api/books?sort=noExiste,asc", staff()).andExpect(status().isBadRequest());
  }

  @Test
  void unsupportedMethodIs405() throws Exception {
    // no existe DELETE /api/books/{id}
    doDelete("/api/books/1", staff()).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void unknownRouteIs404() throws Exception {
    doGet("/api/no-existe", staff()).andExpect(status().isNotFound());
  }

  @Test
  void unreadableBodyIs400() throws Exception {
    doPost("/api/authors", staff(), "{").andExpect(status().isBadRequest());
    doPost("/api/books", staff(), "{\"title\":\"Libro\",\"authorId\":\"abc\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void unsupportedContentTypeIs415() throws Exception {
    mockMvc
        .perform(
            post("/api/authors")
                .with(staff())
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("hola"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void editBookKeepingItsIsbnUpdatesFields() throws Exception {
    long id = book("Titulo viejo", "9780306406157", null, null);

    doPut(
            "/api/books/" + id,
            staff(),
            json("title", "Titulo nuevo", "isbn", "9780306406157", "pageCount", 200))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.title").value("Titulo nuevo"))
        .andExpect(jsonPath("$.pageCount").value(200));

    doGet("/api/books/" + id, staff()).andExpect(jsonPath("$.title").value("Titulo nuevo"));
  }

  @Test
  void editBookReplacesTagsAndCollaborators() throws Exception {
    long collaborator = author("Luis", "Perez");
    long novela = tag("novela");
    long clasico = tag("clasico");
    long historia = tag("historia");
    long id =
        created(
            "/api/books",
            json(
                "title", "Libro",
                "collaboratorIds", List.of(collaborator),
                "tagIds", List.of(novela, clasico)));

    doPut(
            "/api/books/" + id,
            staff(),
            json("title", "Libro", "collaboratorIds", List.of(), "tagIds", List.of(historia)))
        .andExpect(status().isOk());

    doGet("/api/books/" + id, staff())
        .andExpect(jsonPath("$.collaborators", hasSize(0)))
        .andExpect(jsonPath("$.tags", hasSize(1)))
        .andExpect(jsonPath("$.tags[0].name").value("historia"));
  }

  @Test
  void editBookClearsFieldsThatAreOmitted() throws Exception {
    long author = author("Gabriel", "Garcia");
    long publisher = publisher("Alfaguara");
    long dewey = dewey("800");
    long id =
        created(
            "/api/books",
            json("title", "Libro", "authorId", author, "publisherId", publisher, "deweyId", dewey));

    doPut("/api/books/" + id, staff(), json("title", "Libro"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.author").value(nullValue()))
        .andExpect(jsonPath("$.publisher").value(nullValue()))
        .andExpect(jsonPath("$.dewey").value(nullValue()));
  }

  @Test
  void editBookWithIsbnOfAnotherBookIs409() throws Exception {
    book("Uno", "9780306406157", null, null);
    long dos = book("Dos", "9780306406164", null, null);

    doPut("/api/books/" + dos, staff(), json("title", "Dos", "isbn", "9780306406157"))
        .andExpect(status().isConflict());
  }

  @Test
  void editBookChangingPublisherButKeepingOldCollectionIs400() throws Exception {
    long alfaguara = publisher("Alfaguara");
    long planeta = publisher("Planeta");
    long clasicos = collection(alfaguara, "Clasicos");
    long id =
        created(
            "/api/books",
            json("title", "Libro", "publisherId", alfaguara, "collectionId", clasicos));

    doPut(
            "/api/books/" + id,
            staff(),
            json("title", "Libro", "publisherId", planeta, "collectionId", clasicos))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editBookWithCollectionButNoPublisherIs400() throws Exception {
    long alfaguara = publisher("Alfaguara");
    long clasicos = collection(alfaguara, "Clasicos");
    long id = book("Libro", "9780306406157", null, null);

    doPut("/api/books/" + id, staff(), json("title", "Libro", "collectionId", clasicos))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editBookWithUnknownReferenceOrIdIs404() throws Exception {
    long id = book("Libro", "9780306406157", null, null);

    doPut("/api/books/" + id, staff(), json("title", "Libro", "authorId", MISSING))
        .andExpect(status().isNotFound());
    doPut("/api/books/" + MISSING, staff(), json("title", "Libro"))
        .andExpect(status().isNotFound());
  }

  @Test
  void editBookWithoutSessionIs401() throws Exception {
    long id = book("Libro", "9780306406157", null, null);

    doPut("/api/books/" + id, ANON, json("title", "Otro")).andExpect(status().isUnauthorized());
  }
}
