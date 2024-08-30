package br.com.cauaqroz.ConectaPlus.Controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import br.com.cauaqroz.ConectaPlus.model.Freelancer;
import br.com.cauaqroz.ConectaPlus.model.Projeto;
import br.com.cauaqroz.ConectaPlus.model.User;
import br.com.cauaqroz.ConectaPlus.repository.FreelancerRepository;
import br.com.cauaqroz.ConectaPlus.repository.ProjetoRepository;
import br.com.cauaqroz.ConectaPlus.repository.UserRepository;
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
public ResponseEntity<?> createFreelancer(@RequestBody Freelancer freelancerDto) {
    return userRepository.findByEmail(freelancerDto.getEmail()).map(user -> {

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
}

//Atualizar Informações do Freelancer passando o email pelo corpo da requisição
@PutMapping("/freelancer/update")
public ResponseEntity<?> updateFreelancerByEmail(@RequestBody Freelancer freelancerDto) {
    String email = freelancerDto.getEmail(); // Supondo que FreelancerDto tenha um campo email
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
}

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

}