package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.entities.Person;
import alassane.seck.gddapi.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonRepository personRepository;

    @GetMapping
    public List<Person> findAll() {
        return personRepository.findAll();
    }

    @GetMapping("/{id}")
    public Person findById(@PathVariable Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Person not found"));
    }

    @PostMapping
    public ResponseEntity<Person> create(@RequestBody Person payload) {
        Person created = personRepository.save(payload);
        return ResponseEntity.created(URI.create("/api/persons/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public Person update(@PathVariable Long id, @RequestBody Person payload) {
        Person existing = personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Person not found"));
        existing.setName(payload.getName());
        existing.setCity(payload.getCity());
        existing.setPhoneNumber(payload.getPhoneNumber());
        return personRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!personRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Person not found");
        }
        personRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
