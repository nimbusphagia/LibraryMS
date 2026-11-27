package com.nimbusphagia.library.catalog.internal.dto.book;

import com.nimbusphagia.library.catalog.internal.dto.author.AuthorResponse;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryResponse;
import com.nimbusphagia.library.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library.catalog.internal.dto.publisher.PublisherResponse;
import com.nimbusphagia.library.catalog.internal.dto.series.SeriesResponse;
import com.nimbusphagia.library.catalog.internal.dto.subcategory.SubcategoryResponse;
import com.nimbusphagia.library.catalog.internal.dto.tag.TagResponse;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import java.util.List;

public record BookResponse(
    Long id,
    Long institutionId,
    String title,
    String description,
    AuthorResponse author,
    AuthorResponse illustrator,
    List<AuthorResponse> collaborators,
    PublisherResponse publisher,
    CollectionResponse collection,
    Integer collectionVolume,
    SeriesResponse series,
    Integer seriesVolume,
    CategoryResponse category,
    SubcategoryResponse subcategory,
    String country,
    Integer publicationYear,
    Integer pageCount,
    String isbn,
    String deweyNumber,
    List<TagResponse> tags,
    boolean active) {

  public static BookResponse fromEntity(Book book) {
    return new BookResponse(
        book.getId(),
        book.getInstitutionId(),
        book.getTitle(),
        book.getDescription(),
        book.getAuthor() == null ? null : AuthorResponse.fromEntity(book.getAuthor()),
        book.getIllustrator() == null ? null : AuthorResponse.fromEntity(book.getIllustrator()),
        book.getCollaborators().stream().map(AuthorResponse::fromEntity).toList(),
        book.getPublisher() == null ? null : PublisherResponse.fromEntity(book.getPublisher()),
        book.getCollection() == null ? null : CollectionResponse.fromEntity(book.getCollection()),
        book.getCollectionVolume(),
        book.getSeries() == null ? null : SeriesResponse.fromEntity(book.getSeries()),
        book.getSeriesVolume(),
        book.getCategory() == null ? null : CategoryResponse.fromEntity(book.getCategory()),
        book.getSubcategory() == null
            ? null
            : SubcategoryResponse.fromEntity(book.getSubcategory()),
        book.getCountry(),
        book.getPublicationYear(),
        book.getPageCount(),
        book.getIsbn(),
        book.getDeweyNumber(),
        book.getTags().stream().map(TagResponse::fromEntity).toList(),
        book.isActive());
  }
}
