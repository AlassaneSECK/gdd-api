package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.service.BudgetService;
import alassane.seck.gddapi.service.BudgetService.BudgetView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users/{userId}/budget")
@Validated
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable Long userId) {
        try {
            BudgetView budget = budgetService.getBudgetOrThrow(userId);
            return ResponseEntity.ok(toResponse(budget));
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PutMapping
    public ResponseEntity<BudgetResponse> setAmount(@PathVariable Long userId,
                                                    @Valid @RequestBody BudgetAmountRequest request) {
        try {
            BudgetView budget = budgetService.setAmount(userId, request.amount());
            return ResponseEntity.ok(toResponse(budget));
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PatchMapping
    public ResponseEntity<BudgetResponse> applyDelta(@PathVariable Long userId,
                                                     @Valid @RequestBody BudgetDeltaRequest request) {
        try {
            BudgetView budget = budgetService.applyDelta(userId, request.delta(), request.description());
            return ResponseEntity.ok(toResponse(budget));
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private BudgetResponse toResponse(BudgetView budget) {
        return new BudgetResponse(budget.userId(), budget.availableAmount());
    }

    public record BudgetAmountRequest(@NotNull BigDecimal amount) {}

    public record BudgetDeltaRequest(@NotNull BigDecimal delta,
                                     @Size(max = 512) String description) {}

    public record BudgetResponse(Long userId, BigDecimal availableAmount) {}
}
