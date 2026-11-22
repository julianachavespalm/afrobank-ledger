package tech.afrobank.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.afrobank.entity.ContaCorrente;

import java.util.Optional;

@Repository
public interface ContaCorrenteRepository extends JpaRepository<ContaCorrente, Long> {
    Optional<ContaCorrente> findByNumeroConta(String numeroConta);
    boolean existsByNumeroConta(String numeroConta);

    /**
     * Lock pessimista para garantir consistência em operações financeiras concorrentes
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ContaCorrente c WHERE c.id = :id")
    Optional<ContaCorrente> findByIdWithLock(@Param("id") Long id);
}
