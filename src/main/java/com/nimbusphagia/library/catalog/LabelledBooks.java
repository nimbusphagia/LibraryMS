package com.nimbusphagia.library.catalog;

import java.util.Collection;

/**
 * Tells the catalog whether books already have labels, so it can keep label data stable.
 * Implemented by inventory, which owns labels and already depends on the catalog.
 */
public interface LabelledBooks {

  boolean anyLabelled(Collection<Long> bookIds);
}
