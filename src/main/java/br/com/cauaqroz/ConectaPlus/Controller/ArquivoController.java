package br.com.cauaqroz.ConectaPlus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import br.com.cauaqroz.ConectaPlus.repository.ArquivoRepository;
import br.com.cauaqroz.ConectaPlus.repository.ProjetoRepository;
import br.com.cauaqroz.ConectaPlus.model.Arquivo;
import br.com.cauaqroz.ConectaPlus.model.Projeto;
import br.com.cauaqroz.ConectaPlus.service.FileStorageService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/arquivos")
public class ArquivoController {

    @Autowired
    private ArquivoRepository arquivoRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("projetoId") String projetoId,
                                                   @RequestHeader("userId") String userId) {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        if (!projeto.getApprovedParticipants().contains(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuário não autorizado");
        }

        String filePath = fileStorageService.uploadFile(file);
        Arquivo arquivo = new Arquivo();
        arquivo.setNome(file.getOriginalFilename());
        arquivo.setUrl(filePath);
        arquivo.setProjetoId(projetoId);
        arquivoRepository.save(arquivo);
        projeto.getArquivos().add(arquivo.getId());
        projetoRepository.save(projeto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Arquivo carregado com sucesso!");
    }


}