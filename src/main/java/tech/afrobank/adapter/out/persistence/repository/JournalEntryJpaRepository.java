package tech.afrobank.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.afrobank.adapter.out.persistence.entity.JournalEntryJpaEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryJpaEntity, UUID> {

    Optional<JournalEntryJpaEntity> findByTransactionId(UUID transactionId);
}
