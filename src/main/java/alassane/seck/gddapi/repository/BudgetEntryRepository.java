package alassane.seck.gddapi.repository;

import alassane.seck.gddapi.entities.BudgetEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

    Page<BudgetEntry> findByBudgetUserId(Long userId, Pageable pageable);
}
