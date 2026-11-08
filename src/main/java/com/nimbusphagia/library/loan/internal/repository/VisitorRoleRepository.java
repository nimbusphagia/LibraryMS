package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRoleRepository extends JpaRepository<VisitorRole, Long> {

  List<VisitorRole> findAllByOrderByIdAsc();
}
