package alassane.seck.gddapi.service;

import alassane.seck.gddapi.entities.Budget;
import alassane.seck.gddapi.entities.BudgetEntry;
import alassane.seck.gddapi.entities.BudgetEntryType;
import alassane.seck.gddapi.entities.User;
import alassane.seck.gddapi.repository.BudgetRepository;
import alassane.seck.gddapi.repository.BudgetEntryRepository;
import alassane.seck.gddapi.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetEntryRepository budgetEntryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BudgetView getBudgetOrThrow(Long userId) {
        Budget budget = budgetRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Budget introuvable pour l'utilisateur " + userId));
        return toView(budget);
    }

    @Transactional
    public BudgetView setAmount(Long userId, BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul.");
        }
        Budget budget = budgetRepository.findByUserId(userId)
                .orElseGet(() -> createForUser(userId));
        budget.setAvailableAmount(amount);
        Budget saved = budgetRepository.save(budget);
        return toView(saved);
    }

    @Transactional
    public BudgetView applyDelta(Long userId, BigDecimal delta, String description) {
        if (delta == null || delta.signum() == 0) {
            throw new IllegalArgumentException("Le delta doit être non nul.");
        }
        BudgetEntryType type = delta.signum() >= 0 ? BudgetEntryType.INCOME : BudgetEntryType.EXPENSE;
        BudgetUpdate update = recordEntry(userId, type, delta.abs(), null, description);
        return update.budget();
    }

    @Transactional
    public BudgetUpdate recordEntry(Long userId,
                                    BudgetEntryType type,
                                    BigDecimal amount,
                                    Instant occurredAt,
                                    String description) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }

        Budget budget = budgetRepository.findByUserId(userId)
                .orElseGet(() -> createForUser(userId));
        if (budget.getId() == null) {
            budget = budgetRepository.save(budget);
        }

        BudgetEntry entry = new BudgetEntry();
        entry.setBudget(budget);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setDescription(description);
        entry.setOccurredAt(occurredAt);
        BudgetEntry savedEntry = budgetEntryRepository.save(entry);
        budget.getEntries().add(savedEntry);

        BigDecimal delta = type == BudgetEntryType.INCOME ? amount : amount.negate();
        BigDecimal updated = budget.getAvailableAmount().add(delta);
        budget.setAvailableAmount(updated);
        Budget savedBudget = budgetRepository.save(budget);

        return new BudgetUpdate(toView(savedBudget), toEntryView(savedEntry));
    }

    @Transactional(readOnly = true)
    public Page<BudgetEntryView> listEntries(Long userId, Pageable pageable) {
        Budget budget = budgetRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Budget introuvable pour l'utilisateur " + userId));

        return budgetEntryRepository.findByBudgetUserId(userId, pageable)
                .map(this::toEntryView);
    }

    private Budget createForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));
        Budget existing = user.getBudget();
        if (existing != null) {
            return existing;
        }
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setAvailableAmount(BigDecimal.ZERO);
        user.setBudget(budget);
        return budget;
    }

    private BudgetView toView(Budget budget) {
        Long userId = budget.getUser() != null ? budget.getUser().getId() : null;
        return new BudgetView(userId, budget.getAvailableAmount());
    }

    private BudgetEntryView toEntryView(BudgetEntry entry) {
        return new BudgetEntryView(
                entry.getId(),
                entry.getType(),
                entry.getAmount(),
                entry.getOccurredAt(),
                entry.getDescription()
        );
    }

    public record BudgetView(Long userId, BigDecimal availableAmount) {}

    public record BudgetEntryView(Long id,
                                  BudgetEntryType type,
                                  BigDecimal amount,
                                  Instant occurredAt,
                                  String description) {}

    public record BudgetUpdate(BudgetView budget, BudgetEntryView entry) {}
}
