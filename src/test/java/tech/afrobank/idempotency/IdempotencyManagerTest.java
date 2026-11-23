package tech.afrobank.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.exception.IdempotencyException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes de Unidade - IdempotencyManager")
class IdempotencyManagerTest {

    private final IdempotencyStorage storage = new InMemoryIdempotencyStorage();
    private final IdempotencyManager manager = new IdempotencyManager(storage);

    @Test
    @DisplayName("Deve permitir primeira requisicao com chave inedita")
    void devePermitirChaveInedita() {
        assertThatNoException().isThrownBy(() -> manager.acquire("key-1"));
        manager.complete("key-1", "response-ok");
    }

    @Test
    @DisplayName("Deve lancar IdempotencyException ao receber mesma chave em duplicidade")
    void deveRejeitarChaveDuplicada() {
        manager.acquire("key-2");
        manager.complete("key-2", "response-ok");

        assertThatThrownBy(() -> manager.acquire("key-2"))
                .isInstanceOf(IdempotencyException.class)
                .hasMessageContaining("Operação já processada com sucesso anteriormente");
    }

    @Test
    @DisplayName("Deve ignorar quando chave for nula ou em branco")
    void deveIgnorarChaveVazia() {
        assertThatNoException().isThrownBy(() -> manager.acquire(null));
        assertThatNoException().isThrownBy(() -> manager.acquire("   "));
    }
}
