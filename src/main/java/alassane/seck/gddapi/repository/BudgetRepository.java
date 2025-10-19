package alassane.seck.gddapi.repository;

import alassane.seck.gddapi.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserId(Long userId);
}
