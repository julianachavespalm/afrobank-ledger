package tech.afrobank.adapter.out.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.afrobank.adapter.out.persistence.entity.PostingJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostingJpaRepository extends JpaRepository<PostingJpaEntity, UUID> {

    List<PostingJpaEntity> findByAccountIdOrderByRecordedAtDesc(UUID accountId, Pageable pageable);

    @Query("SELECT p FROM PostingJpaEntity p WHERE p.journalEntry.bookingDate = :date")
    List<PostingJpaEntity> findByBookingDate(@Param("date") LocalDate date);
}
