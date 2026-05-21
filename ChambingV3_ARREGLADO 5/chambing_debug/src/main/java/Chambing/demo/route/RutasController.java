package Chambing.demo.route;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;

@Controller
public class RutasController {

    
    @GetMapping("/")
    public String index() {
        return "index";
    }

    
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    
    @GetMapping("/user-dashboard")
    public String userDashboard() {
        return "user-dashboard";
    }

    @GetMapping("/user-begin")
    public String userBegin() {
        return "user-begin";
    }

    
    @GetMapping("/company-dashboard")
    public String companyDashboard() {
        return "company-dashboard";
    }

    @GetMapping("/companies-registers")
    public String companiesRegisters() {
        return "companies-registers";
    }

    @GetMapping("/only-proporsals")
    public String onlyProporsals() {
        return "only-proporsals";
    }

    @GetMapping("/proposals-answer")
    public String proposalsAnswer() {
        return "proposals-answer";
    }

    @GetMapping("/proposals-states")
    public String proposalsStates() {
        return "proposals-states";
    }


     @GetMapping("/companies-personals-profiles")
    public String companiesPersonalsProfiles() {
        return "companies-personals-profiles";
    }

    
    @GetMapping("/company-profile")
    public String companyProfile() {
        return "company-profile";
    }

    @GetMapping("/edit-propuesta")
    public String editPropuesta() {
        return "edit-propuesta";
    }

    @GetMapping("/company-proposal-edit")
    public String companyProposalEdit() {
        return "company-proposal-edit";
    }
    
    @GetMapping("/company-proposals")
    public String companyProposals() {
        return "company-proposals";
    }

  
    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    
    @GetMapping("/blog")
    public String blog() {
        return "blog";
    }

    
    @GetMapping("/about")
    public String about() {
        return "about";
    }

   
    @GetMapping("/admin")
    public String loginPage() {
        return "login-admin";
    }

    @GetMapping("/admin/panel")
    public ModelAndView panelPage(HttpSession session) {
        if (session.getAttribute("adminLogged") == null) {
            return new ModelAndView("redirect:/admin");
        }

        return new ModelAndView("panel");
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "403";
    }

    @GetMapping("/error")
    public String errorGeneral() {
        return "error";
    }
}
