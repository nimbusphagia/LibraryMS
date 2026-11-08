package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolGradeRepository extends JpaRepository<SchoolGrade, Long> {

  List<SchoolGrade> findAllByOrderByLevelAscGradeAsc();
}
