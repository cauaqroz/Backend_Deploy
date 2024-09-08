package br.com.cauaqroz.ConectaPlus.Controller;


import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import br.com.cauaqroz.ConectaPlus.model.Freelancer;
import br.com.cauaqroz.ConectaPlus.model.Projeto;
import br.com.cauaqroz.ConectaPlus.model.User;
import br.com.cauaqroz.ConectaPlus.repository.FreelancerRepository;
import br.com.cauaqroz.ConectaPlus.repository.ProjetoRepository;
import br.com.cauaqroz.ConectaPlus.repository.UserRepository;
import br.com.cauaqroz.ConectaPlus.service.FileStorageService;
import br.com.cauaqroz.ConectaPlus.util.JwtUtil;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FreelancerRepository freelancerRepository;

        @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private JwtUtil jwtUtil;

@Autowired
    private FileStorageService fileStorageService;


    // Cadastrar usuário
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User userDto) {
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email já cadastrado.");
        }
        User user = new User();
        user.setName(userDto.getName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
String hashedPassword = passwordEncoder.encode(userDto.getPassword());
user.setPassword(hashedPassword);
        user.setCountry(userDto.getCountry());
        user.setState(userDto.getState());
        userRepository.save(user);
        return ResponseEntity.ok("Usuário cadastrado com sucesso.");
    }

// Buscar usuário pelo email
@GetMapping("/email/{email}")
public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (!userOpt.isPresent()) {
        return ResponseEntity.notFound().build();
    }

    User user = userOpt.get();
    Optional<Freelancer> freelancerOpt = freelancerRepository.findById(user.getId());

    Map<String, Object> result = new HashMap<>();
    result.put("id", user.getId());
    result.put("registrationDate", user.getRegistrationDate());
    result.put("name", user.getName());
    result.put("lastName", user.getLastName());
    result.put("email", user.getEmail());
    result.put("password", user.getPassword()); // remover ou ocultar a senha na resposta
    result.put("country", user.getCountry());
    result.put("state", user.getState());

    freelancerOpt.ifPresent(freelancer -> {
        result.put("description", freelancer.getDescription());
        result.put("portfolio", freelancer.getPortfolio());
        result.put("education", freelancer.getEducation());
        result.put("areaOfExpertise", freelancer.getAreaOfExpertise());
        result.put("completedJobs", freelancer.getCompletedJobs());
        result.put("onTimeDeliveries", freelancer.getOnTimeDeliveries());
    });

    return ResponseEntity.ok(result);
}
// Novo método para buscar usuário por ID
@GetMapping("/{id}")
public ResponseEntity<?> getUserById(@PathVariable String id) {
    Optional<User> userOptional = userRepository.findById(id);
    if (!userOptional.isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
    }

    User user = userOptional.get();

    // Buscar projetos criados pelo usuário
    List<Projeto> projetosCriados = projetoRepository.findByCriador_Id(user.getId());

    // Buscar projetos em que o usuário participa
    List<Projeto> projetosParticipando = projetoRepository.findAll().stream()
            .filter(projeto -> projeto.getApprovedParticipants().contains(user.getId()))
            .collect(Collectors.toList());

    Map<String, Object> response = new HashMap<>();
    response.put("id", user.getId());
    response.put("name", user.getName());
    response.put("lastName", user.getLastName());
    response.put("email", user.getEmail());
    response.put("country", user.getCountry());
    response.put("state", user.getState());
    response.put("projetosCriados", projetosCriados);
    response.put("projetosParticipando", projetosParticipando);

    return ResponseEntity.ok(response);
}

    // Cadastrar usuário como freelancer
    @PostMapping("/freelancer")
    public ResponseEntity<?> createFreelancer(@RequestHeader("userId") String userId, @RequestBody Freelancer freelancerDto) {
        return userRepository.findById(userId).map(user -> {
    
            // Verifica se já existe um cadastro de freelancer para este usuário
            if (freelancerRepository.findById(user.getId()).isPresent()) {
                return ResponseEntity.badRequest().body("Usuário já cadastrado como freelancer.");
            }
            Freelancer freelancer = new Freelancer();
    
            // Associa o freelancer ao mesmo ID do usuário
            freelancer.setId(user.getId());
            freelancer.setDescription(freelancerDto.getDescription());
            freelancer.setPortfolio(freelancerDto.getPortfolio());
            freelancer.setAreaOfExpertise(freelancerDto.getAreaOfExpertise());
            freelancer.setEducation(freelancerDto.getEducation());
            freelancer.setCompletedJobs(freelancerDto.getCompletedJobs());
            freelancer.setOnTimeDeliveries(freelancerDto.getOnTimeDeliveries());
            
            freelancerRepository.save(freelancer);
            return ResponseEntity.ok("Freelancer cadastrado com sucesso.");
        }).orElse(ResponseEntity.badRequest().body("Usuário não encontrado."));
    }
/*
// Atualizar Informações do Freelancer passando o userId pelo header
@PutMapping("/freelancer/update")
public ResponseEntity<?> updateFreelancerById(@RequestHeader("userId") String userId, @RequestBody Freelancer freelancerDto) {
    return userRepository.findById(userId).map(user -> {
        return freelancerRepository.findById(user.getId()).map(freelancer -> {
            // Atualiza as informações do freelancer com os dados recebidos
            freelancer.setDescription(freelancerDto.getDescription());
            freelancer.setPortfolio(freelancerDto.getPortfolio());
            freelancer.setAreaOfExpertise(freelancerDto.getAreaOfExpertise());
            freelancer.setEducation(freelancerDto.getEducation());
            freelancer.setCompletedJobs(freelancerDto.getCompletedJobs());
            freelancer.setOnTimeDeliveries(freelancerDto.getOnTimeDeliveries());

            freelancerRepository.save(freelancer);
            return ResponseEntity.ok("Freelancer atualizado com sucesso.");
        }).orElse(ResponseEntity.badRequest().body("Freelancer não encontrado para este usuário."));
    }).orElse(ResponseEntity.badRequest().body("Usuário não encontrado."));
}*/

//login antigo
/*
@PostMapping("/login")
public ResponseEntity<?> loginUser(@RequestBody User loginDetails) {
    Optional<User> userOpt = userRepository.findByEmail(loginDetails.getEmail());
    if (!userOpt.isPresent()) {
        return ResponseEntity.badRequest().body("Usuário não encontrado.");
    }
    User user = userOpt.get();
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    if (!passwordEncoder.matches(loginDetails.getPassword(), user.getPassword())) {
        return ResponseEntity.badRequest().body("Senha inválida.");
    }
    Map<String, Object> response = new HashMap<>();
response.put("id", user.getId());
return ResponseEntity.ok(response);
}
*/

//nova versão do login
@PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginDetails) {
        Optional<User> userOpt = userRepository.findByEmail(loginDetails.getEmail());
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Usuário não encontrado.");
        }
        User user = userOpt.get();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(loginDetails.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Senha inválida.");
        }

        // Gerar token JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // Buscar projetos criados pelo usuário
        List<Projeto> projetosCriados = projetoRepository.findByCriador_Id(user.getId());

        // Buscar projetos em que o usuário participa
        List<Projeto> projetosParticipando = projetoRepository.findAll().stream()
                .filter(projeto -> projeto.getApprovedParticipants().contains(user.getId()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("country", user.getCountry());
        response.put("state", user.getState());
        response.put("projetosCriados", projetosCriados);
        response.put("projetosParticipando", projetosParticipando);
        response.put("token", token);
        response.put("avatar", user.getAvatar());

        return ResponseEntity.ok(response);
    }

//Atualizar Informações do Freelancer passando o email pela URL
/*@PutMapping("/freelancer/update")
public ResponseEntity<?> updateFreelancerByEmail(@RequestParam String email, @RequestBody Freelancer freelancerDto) {
    return userRepository.findByEmail(email).map(user -> {
        return freelancerRepository.findById(user.getId()).map(freelancer -> {
            // Atualiza as informações do freelancer com os dados recebidos
            freelancer.setDescription(freelancerDto.getDescription());
            freelancer.setPortfolio(freelancerDto.getPortfolio());
            freelancer.setAreaOfExpertise(freelancerDto.getAreaOfExpertise());
            freelancer.setEducation(freelancerDto.getEducation());
            freelancer.setCompletedJobs(freelancerDto.getCompletedJobs());
            freelancer.setOnTimeDeliveries(freelancerDto.getOnTimeDeliveries());

            freelancerRepository.save(freelancer);
            return ResponseEntity.ok("Freelancer atualizado com sucesso.");
        }).orElse(ResponseEntity.badRequest().body("Freelancer não encontrado para este usuário."));
    }).orElse(ResponseEntity.badRequest().body("Email não cadastrado como usuário."));
}*/

//Cadastra Freelancer passando o email pela URL
/*@PostMapping("/freelancer")
public ResponseEntity<?> createFreelancer(@RequestParam String email, @RequestBody Freelancer freelancerDto) {
    return userRepository.findByEmail(email).map(user -> {

        // Verifica se já existe um cadastro de freelancer para este usuário
        if (freelancerRepository.findById(user.getId()).isPresent()) {
            return ResponseEntity.badRequest().body("Usuário já cadastrado como freelancer.");
        }
        Freelancer freelancer = new Freelancer();

        // Associa o freelancer ao mesmo ID do usuário
        freelancer.setId(user.getId());
        freelancer.setDescription(freelancerDto.getDescription());
        freelancer.setPortfolio(freelancerDto.getPortfolio());
        freelancer.setAreaOfExpertise(freelancerDto.getAreaOfExpertise());
        freelancer.setEducation(freelancerDto.getEducation());
        freelancer.setCompletedJobs(freelancerDto.getCompletedJobs());
        freelancer.setOnTimeDeliveries(freelancerDto.getOnTimeDeliveries());
        
        freelancerRepository.save(freelancer);
        return ResponseEntity.ok("Freelancer cadastrado com sucesso.");
    }).orElse(ResponseEntity.badRequest().body("Email não cadastrado como usuário."));
}*/


@GetMapping("/{id}/freelancer")
public ResponseEntity<?> getFreelancerInfo(@PathVariable String id) {
    Optional<User> userOptional = userRepository.findById(id);
    if (!userOptional.isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
    }

    User user = userOptional.get();
    Optional<Freelancer> freelancerOptional = freelancerRepository.findById(user.getId());

    if (!freelancerOptional.isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Freelancer não encontrado para este usuário.");
    }

    Freelancer freelancer = freelancerOptional.get();
    Map<String, Object> result = new HashMap<>();
    result.put("id", freelancer.getId());
    result.put("description", freelancer.getDescription());
    result.put("portfolio", freelancer.getPortfolio());
    result.put("education", freelancer.getEducation());
    result.put("areaOfExpertise", freelancer.getAreaOfExpertise());
    result.put("completedJobs", freelancer.getCompletedJobs());
    result.put("onTimeDeliveries", freelancer.getOnTimeDeliveries());

    return ResponseEntity.ok(result);
}

// Atualizar informações do usuário usando PATCH e passando o userId pelo header
// Atualizar informações do usuário usando PATCH e passando o userId pelo header
@PatchMapping("/update")
public ResponseEntity<?> patchUserById(@RequestHeader("userId") String userId, @RequestBody Map<String, Object> updates) {
    return userRepository.findById(userId).map(user -> {
        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    user.setName((String) value);
                    break;
                case "lastName":
                    user.setLastName((String) value);
                    break;
                case "email":
                    String newEmail = (String) value;
                    // Verificar se o novo email já está em uso por outro usuário
                    if (userRepository.findByEmail(newEmail).isPresent() && !user.getEmail().equals(newEmail)) {
                        throw new IllegalArgumentException("Email já está em uso por outro usuário.");
                    }
                    user.setEmail(newEmail);
                    break;
                case "password":
                    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                    String hashedPassword = passwordEncoder.encode((String) value);
                    user.setPassword(hashedPassword);
                    break;
                case "country":
                    user.setCountry((String) value);
                    break;
                case "state":
                    user.setState((String) value);
                    break;
                default:
                    break;
            }
        });
        userRepository.save(user);
        return ResponseEntity.ok("Usuário atualizado com sucesso.");
    }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado."));
}
// Atualizar informações do Freelancer usando PATCH e passando o userId pelo header
@PatchMapping("/freelancer/update")
public ResponseEntity<?> patchFreelancerById(@RequestHeader("userId") String userId, @RequestBody Map<String, Object> updates) {
    return userRepository.findById(userId).map(user -> {
        return freelancerRepository.findById(user.getId()).map(freelancer -> {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "description":
                        freelancer.setDescription((String) value);
                        break;
                    case "portfolio":
                        freelancer.setPortfolio((String) value);
                        break;
                    case "education":
                        freelancer.setEducation((String) value);
                        break;
                    case "areaOfExpertise":
                        freelancer.setAreaOfExpertise((String) value);
                        break;
                    case "completedJobs":
                        freelancer.setCompletedJobs((Integer) value);
                        break;
                    case "onTimeDeliveries":
                        freelancer.setOnTimeDeliveries((Integer) value);
                        break;
                    default:
                        break;
                }
            });
            freelancerRepository.save(freelancer);
            return ResponseEntity.ok("Freelancer atualizado com sucesso.");
        }).orElse(ResponseEntity.badRequest().body("Freelancer não encontrado para este usuário."));
    }).orElse(ResponseEntity.badRequest().body("Usuário não encontrado."));
}
@DeleteMapping("/delete")
public ResponseEntity<?> deleteUserById(@RequestHeader("userId") String userId) {
    return userRepository.findById(userId).map(user -> {
        // Deletar perfil de freelancer se existir
        freelancerRepository.findById(user.getId()).ifPresent(freelancer -> {
            freelancerRepository.delete(freelancer);
        });
        // Deletar usuário
        userRepository.delete(user);
        return ResponseEntity.ok("Usuário deletado com sucesso.");
    }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado."));
}

// Deletar apenas o perfil de freelancer
@DeleteMapping("/freelancer/delete")
public ResponseEntity<?> deleteFreelancerById(@RequestHeader("userId") String userId) {
    return userRepository.findById(userId).map(user -> {
        return freelancerRepository.findById(user.getId()).map(freelancer -> {
            freelancerRepository.delete(freelancer);
            return ResponseEntity.ok("Perfil de freelancer deletado com sucesso.");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Perfil de freelancer não encontrado para este usuário."));
    }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado."));
}


@PostMapping("/uploadAvatar")
public ResponseEntity<?> uploadAvatar(@RequestHeader("userId") String userId, @RequestParam("file") MultipartFile file) {
    Optional<User> userOptional = userRepository.findById(userId);
    if (!userOptional.isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
    }

    String avatarUrl = fileStorageService.uploadFile(file);
    User user = userOptional.get();
    user.setAvatar(avatarUrl);
    userRepository.save(user);

    Map<String, Object> response = new HashMap<>();
    response.put("message", "Avatar atualizado com sucesso.");
    response.put("avatarId", avatarUrl); // Supondo que avatarUrl seja o ID do avatar

    return ResponseEntity.ok(response);
}
/*
@PostMapping("/uploadAvatar")
    public ResponseEntity<?> uploadAvatar(@RequestHeader("userId") String userId, @RequestParam("file") MultipartFile file) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
        }

        String avatarUrl = fileStorageService.uploadFile(file);
        User user = userOptional.get();
        user.setAvatar(avatarUrl);
        userRepository.save(user);

        return ResponseEntity.ok("Avatar atualizado com sucesso.");
    }
*/
    // Endpoint para recuperar a imagem de perfil do usuário pelo ID do avatar
    @GetMapping("/avatar/{avatarId}")
    public ResponseEntity<?> getAvatar(@PathVariable("avatarId") String avatarId) {
        Optional<User> userOptional = userRepository.findByAvatar(avatarId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Avatar não encontrado.");
        }
    
        User user = userOptional.get();
        String avatarUrl = user.getAvatar();
        if (avatarUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Avatar não encontrado.");
        }
    
        try {
            InputStream fileStream = fileStorageService.downloadFile(avatarUrl);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + avatarUrl + "\"")
                    .contentType(MediaType.IMAGE_JPEG) // Ajuste o tipo de mídia conforme necessário
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao recuperar o avatar.");
        }
    }

    

}