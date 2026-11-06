package com.nimbusphagia.library.inventory.internal.dto.copy;

import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;

public record CopyFilter(Long bookId, Long shelfId, CopyStatus status, Boolean unlabeled) {}
