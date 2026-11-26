package com.nimbusphagia.library.organization;

import java.time.ZoneId;

public interface OrganizationApi {

  Long createOrganization(String name);

  ZoneId zoneOfLibrary(Long libraryId);
}
