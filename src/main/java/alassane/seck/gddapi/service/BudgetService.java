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

    /**
     * Enregistre un mouvement budgétaire et synchronise le solde restant.
     * <p>
     * Subtilité : on crée à la volée le budget associé à l'utilisateur si nécessaire (lors du premier mouvement),
     * puis on persiste l'entrée et on met à jour le champ `availableAmount`. Une dépense entraîne une
     * soustraction, un revenu une addition. Le calcul reste localisé ici pour éviter toute divergence avec
     * d'autres mises à jour manuelles.
     */
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
        // Important : on stocke l'entrée avant de recalculer le solde, afin de disposer d'un identifiant
        // et d'un horodatage cohérents dans la réponse.
        BudgetEntry savedEntry = budgetEntryRepository.save(entry);
        budget.getEntries().add(savedEntry);

        BigDecimal delta = type == BudgetEntryType.INCOME ? amount : amount.negate();
        // `availableAmount` évolue exclusivement au fil des entrées : une dépense retire le montant,
        // un revenu l'ajoute. Ce calcul doit impérativement rester atomique (transactionnel) pour éviter
        // les courses critiques si plusieurs écritures arrivent en même temps.
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
            // Cas limite : si un budget a été attaché via un autre flux (ex: synchronisation), on le réutilise
            // pour éviter de dupliquer les enregistrements et de fausser le solde courant.
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
