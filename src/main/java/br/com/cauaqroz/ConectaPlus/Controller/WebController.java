package br.com.cauaqroz.ConectaPlus.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class WebController {
    @GetMapping("/")
    public String Index() {
        return "Index";
    }
    

    @GetMapping("/menu")
    public String menu() {
        return "fragments";
    }

    @GetMapping("/app")
    public String chat() {
        return "chat";
    }

}
