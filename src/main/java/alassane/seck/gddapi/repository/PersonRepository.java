package alassane.seck.gddapi.repository;

import alassane.seck.gddapi.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
