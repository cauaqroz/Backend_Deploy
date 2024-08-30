package br.com.cauaqroz.ConectaPlus.Controller;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import br.com.cauaqroz.ConectaPlus.model.Channel;
import br.com.cauaqroz.ConectaPlus.model.Projeto;
import br.com.cauaqroz.ConectaPlus.model.User;
import br.com.cauaqroz.ConectaPlus.repository.ChannelRepository;
import br.com.cauaqroz.ConectaPlus.repository.ProjetoRepository;
import br.com.cauaqroz.ConectaPlus.repository.UserRepository;
import br.com.cauaqroz.ConectaPlus.service.FileStorageService;

@RestController
@RequestMapping("/projetos")
public class ProjetoController {

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

     @Autowired
    private FileStorageService fileStorageService;

    //Implementação antiga do metodo criarProjeto
    @PostMapping
    public Projeto criarProjeto(@RequestBody Projeto projeto, @RequestHeader("userId") String userId) {
        User criador = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        projeto.setCriador(criador);
    
        Channel channel = new Channel();
        channel.setMasterUserId(userId);
        channel.setAllowedUserIds(new ArrayList<>());
        channel.setAccessRequests(new ArrayList<>());
        channel.setMessages(new ArrayList<>());
    
        // Verifica se a lista de participantes aprovados não é nula antes de processar
        if (projeto.getApprovedParticipants() != null) {
            List<String> approvedUserIds = projeto.getApprovedParticipants().stream()
                .map(user -> {
                    User userObj = userRepository.findById(user).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
                    return userObj.getId();
                })
                .collect(Collectors.toList());
            channel.getAllowedUserIds().addAll(approvedUserIds);
        }
    
        channel = channelRepository.save(channel);
        projeto.setChatId(channel.getId());
        return projetoRepository.save(projeto);
    }



    @GetMapping
    public List<Projeto> listarProjetos() {
        return projetoRepository.findAll();
    }

    @GetMapping("/buscarProjetos")
    public List<Projeto> buscarProjetosPorTitulo(@RequestParam String titulo) {
        return projetoRepository.findByTituloContaining(titulo);
    }


    @GetMapping("/UserProjeto")
    public List<Projeto> listarProjetosDoUsuario(@RequestHeader("userId") String userId) {
        return projetoRepository.findByCriador_Id(userId);
    }
/* 
//Antiga atualização do projeto

@PutMapping("/{projetoId}")
public ResponseEntity<?> editarProjeto(@PathVariable String projetoId, @RequestHeader("ownerId") String ownerId, @RequestBody Projeto projeto) {
    Projeto projetoExistente = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    
    // Verificar se o usuário que está tentando editar é o proprietário do projeto
    if (!projetoExistente.getCriador().getId().equals(ownerId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o proprietário do projeto pode editá-lo.");
    }
    
    projeto.setId(projetoId);
    Projeto projetoAtualizado = projetoRepository.save(projeto);
    return ResponseEntity.ok(projetoAtualizado);
}
*/

//atualização do projeto com patch
@PatchMapping("/{projetoId}")
public ResponseEntity<?> atualizarParcialProjeto(@PathVariable String projetoId, @RequestHeader("ownerId") String ownerId, @RequestBody Map<String, Object> updates) {
    Projeto projetoExistente = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    
    // Verificar se o usuário que está tentando editar é o proprietário do projeto
    if (!projetoExistente.getCriador().getId().equals(ownerId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o proprietário do projeto pode editá-lo.");
    }
    
    // Atualizar apenas os campos fornecidos na requisição
    updates.forEach((key, value) -> {
        switch (key) {
            case "titulo":
                projetoExistente.setTitulo((String) value);
                break;
            case "descricao":
                projetoExistente.setDescricao((String) value);
                break;
            case "tecnologia":
                projetoExistente.setTecnologia((String) value);
                break;
            // Adicione mais campos conforme necessário
            default:
                throw new IllegalArgumentException("Campo não suportado: " + key);
        }
    });

    Projeto projetoAtualizado = projetoRepository.save(projetoExistente);
    return ResponseEntity.ok(projetoAtualizado);
}


    @PostMapping("/{projetoId}/uploadCapa")
    public ResponseEntity<?> uploadCapa(@PathVariable String projetoId, @RequestParam("file") MultipartFile file) {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
        String capaUrl = fileStorageService.uploadFile(file);
        projeto.setCapaUrl(capaUrl);
        projetoRepository.save(projeto);
        return ResponseEntity.ok().build();
    }

@PostMapping("/{projetoId}/solicitarParticipacao")
public ResponseEntity<?> solicitarParticipacao(@PathVariable String projetoId, @RequestHeader("userId") String userId) {
    Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    
    if (projeto.getParticipationRequests().contains(userId)) {
        return ResponseEntity.badRequest().body("Solicitação de participação já enviada.");
    }

    projeto.getParticipationRequests().add(userId);
    projetoRepository.save(projeto);

    return ResponseEntity.ok().body("Solicitação de participação enviada com sucesso.");
}

// Método para aprovar um usuário em um projeto
@PostMapping("/{projetoId}/aprovarUsuario")
public ResponseEntity<?> aprovarUsuario(@PathVariable String projetoId, @RequestHeader("ownerId") String ownerId, @RequestBody Map<String, String> requestBody) {
    String userId = requestBody.get("userId");
    Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    
    // Verificar se o usuário que está tentando aprovar é o proprietário do projeto
    if (!projeto.getCriador().getId().equals(ownerId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o proprietário do projeto pode aprovar usuários.");
    }

    Channel channel = channelRepository.findById(projeto.getChatId()).orElseThrow(() -> new RuntimeException("Canal não encontrado."));

    // Verificar se a lista allowedUserIds está inicializada
    if (channel.getAllowedUserIds() == null) {
        channel.setAllowedUserIds(new ArrayList<>());
    }

    // Adicionar log para verificar o estado atual das solicitações de participação
    System.out.println("Solicitações de participação antes da remoção: " + projeto.getParticipationRequests());

    if (!projeto.getParticipationRequests().remove(userId)) {
        return ResponseEntity.badRequest().body("Solicitação de participação não encontrada.");
    }

    // Adicionar log para verificar o estado das solicitações de participação após a remoção
    System.out.println("Solicitações de participação após a remoção: " + projeto.getParticipationRequests());

    projeto.getApprovedParticipants().add(userId);
    channel.getAllowedUserIds().add(userId);

    projetoRepository.save(projeto);
    channelRepository.save(channel);

    return ResponseEntity.ok().body("Usuário aprovado com sucesso.");
}
// Método para listar solicitações de participação em um projeto
    @GetMapping("/{projetoId}/pedidosParticipacao")
    public List<String> listarPedidosParticipacao(@PathVariable String projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
        return projeto.getParticipationRequests();
    }

    
    @DeleteMapping("/{projetoId}/negarSolicitacao/{userId}")
    public ResponseEntity<?> negarSolicitacao(@PathVariable String projetoId, @PathVariable String userId, @RequestHeader("ownerId") String ownerId) {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
        
        // Verificar se o usuário que está tentando negar é o proprietário do projeto
        if (!projeto.getCriador().getId().equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o proprietário do projeto pode negar solicitações.");
        }
    
        if (!projeto.getParticipationRequests().remove(userId)) {
            return ResponseEntity.badRequest().body("Solicitação de participação não encontrada.");
        }
        projetoRepository.save(projeto);
        return ResponseEntity.ok().body("Solicitação negada.");
    }
    /* //negar solicitação antigo
    @DeleteMapping("/{projetoId}/negarSolicitacao/{userId}")
    public ResponseEntity<?> negarSolicitacao(@PathVariable String projetoId, @PathVariable String userId) {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
        if (!projeto.getParticipationRequests().remove(userId)) {
            return ResponseEntity.badRequest().body("Solicitação de participação não encontrada.");
        }
        projetoRepository.save(projeto);
        return ResponseEntity.ok().body("Solicitação negada.");
    }*/

    @DeleteMapping("/{projetoId}")
public ResponseEntity<?> excluirProjeto(@PathVariable String projetoId, @RequestHeader("ownerId") String ownerId) {
    Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    
    // Verificar se o usuário que está tentando excluir é o proprietário do projeto
    if (!projeto.getCriador().getId().equals(ownerId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas o proprietário do projeto pode excluí-lo.");
    }

    channelRepository.deleteById(projeto.getChatId());
    projetoRepository.deleteById(projetoId);
    return ResponseEntity.ok().body("Projeto excluído com sucesso.");
}

    @GetMapping("/{projetoId}")
    public Projeto exibirProjeto(@PathVariable String projetoId) {
        return projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    }

    
    //recuperar imagem
    @GetMapping("/{projetoId}/capa")
    public ResponseEntity<InputStreamResource> getCapa(@PathVariable String projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
        
        String capaUrl = projeto.getCapaUrl();
        if (capaUrl == null) {
            throw new RuntimeException("Capa não encontrada para o projeto.");
        }

        InputStream fileStream = fileStorageService.downloadFile(capaUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + capaUrl + "\"")
                .contentType(MediaType.IMAGE_JPEG) // Ajuste o tipo de mídia conforme necessário
                .body(new InputStreamResource(fileStream));
    }
}