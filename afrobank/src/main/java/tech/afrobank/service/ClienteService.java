package tech.afrobank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.afrobank.dto.ClienteRequestDTO;
import tech.afrobank.dto.ClienteResponseDTO;
import tech.afrobank.entity.Cliente;
import tech.afrobank.exception.NegocioException;
import tech.afrobank.exception.RecursoNaoEncontradoException;
import tech.afrobank.repository.ClienteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        return ClienteResponseDTO.fromEntity(cliente);
    }

    @Transactional
    public ClienteResponseDTO cadastrar(ClienteRequestDTO dto) {
        if (clienteRepository.existsByCpf(dto.cpf())) {
            throw new NegocioException("Já existe um cliente cadastrado com o CPF informado: " + dto.cpf());
        }
        if (clienteRepository.existsByEmail(dto.email())) {
            throw new NegocioException("Já existe um cliente cadastrado com o e-mail informado: " + dto.email());
        }

        Cliente cliente = Cliente.builder()
                .nome(dto.nome())
                .cpf(dto.cpf())
                .email(dto.email())
                .endereco(dto.endereco())
                .rendaSalarial(dto.rendaSalarial())
                .senha(passwordEncoder.encode(dto.senha()))
                .build();

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
    }
}
