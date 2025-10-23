package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.security.AuthenticatedUser;
import alassane.seck.gddapi.service.BudgetService;
import alassane.seck.gddapi.service.BudgetService.BudgetView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<BudgetResponse> getBudget(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        try {
            // On part du principe que le JWT contient déjà l'identité de l'utilisateur.
            // Aucun identifiant n'est accepté en paramètre : cela évite les accès croisés.
            BudgetView budget = budgetService.getBudgetOrThrow(currentUser.getId());
            return ResponseEntity.ok(toResponse(budget));
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    private BudgetResponse toResponse(BudgetView budget) {
        return new BudgetResponse(budget.userId(), budget.availableAmount());
    }

    public record BudgetResponse(Long userId, java.math.BigDecimal availableAmount) {}
}
