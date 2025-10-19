package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.entities.BudgetEntryType;
import alassane.seck.gddapi.service.BudgetService;
import alassane.seck.gddapi.service.BudgetService.BudgetEntryView;
import alassane.seck.gddapi.service.BudgetService.BudgetUpdate;
import alassane.seck.gddapi.service.BudgetService.BudgetView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/users/{userId}/budget/entries")
@Validated
@RequiredArgsConstructor
public class BudgetEntryController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<Page<BudgetEntryResponse>> listEntries(@PathVariable Long userId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Sort sort = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
        try {
            Page<BudgetEntryResponse> response = budgetService.listEntries(userId, pageable)
                    .map(this::toResponse);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping
    public ResponseEntity<BudgetEntryCreatedResponse> createEntry(@PathVariable Long userId,
                                                                  @Valid @RequestBody CreateBudgetEntryRequest request) {
        try {
            BudgetUpdate update = budgetService.recordEntry(
                    userId,
                    request.type(),
                    request.amount(),
                    request.occurredAt(),
                    request.description()
            );
            BudgetEntryCreatedResponse response = new BudgetEntryCreatedResponse(
                    toResponse(update.entry()),
                    toBudgetSummary(update.budget())
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private BudgetEntryResponse toResponse(BudgetEntryView view) {
        return new BudgetEntryResponse(
                view.id(),
                view.type(),
                view.amount(),
                view.occurredAt(),
                view.description()
        );
    }

    private BudgetSummaryResponse toBudgetSummary(BudgetView view) {
        return new BudgetSummaryResponse(view.userId(), view.availableAmount());
    }

    public record CreateBudgetEntryRequest(
            @NotNull BudgetEntryType type,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
            Instant occurredAt,
            @Size(max = 512) String description
    ) {}

    public record BudgetEntryResponse(
            Long id,
            BudgetEntryType type,
            BigDecimal amount,
            Instant occurredAt,
            String description
    ) {}

    public record BudgetSummaryResponse(Long userId, BigDecimal availableAmount) {}

    public record BudgetEntryCreatedResponse(BudgetEntryResponse entry, BudgetSummaryResponse budget) {}
}
