package com.nimbusphagia.library.catalog;

import java.util.Collection;
import java.util.Set;

/**
 * Tells the catalog which label parts the labels stuck on these books take from catalog data, so
 * it can keep that data stable. Implemented by inventory, which owns labels and depends on the
 * catalog.
 */
public interface LabelledBooks {

  Set<LabelPart> derivedPartsInUse(Collection<Long> bookIds);
}
