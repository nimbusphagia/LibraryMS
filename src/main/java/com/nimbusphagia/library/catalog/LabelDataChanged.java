package com.nimbusphagia.library.catalog;

import java.util.Set;

/**
 * Catalog data printed on labels changed for these books. Listened to in the same transaction,
 * so a failure while updating labels rolls the catalog change back. The actor signs the revisions.
 */
public record LabelDataChanged(Set<Long> bookIds, Long actorId) {}
