package com.nimbusphagia.library.account.internal.entity;

public enum AccountRole {
  OWNER,
  ADMIN,
  LIBRARIAN,
  STAFF;

  public boolean isOrganizationWide() {
    return this == OWNER || this == ADMIN;
  }
}
