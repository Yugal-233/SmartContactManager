package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		
		User user = userRepository.getUserByUserName(userName);
		model.addAttribute("user",user);
	}
	
	@GetMapping("/index")
	public String userDashboard(Model model, Principal principal) {
		
		String userName = principal.getName();
		
		User user = userRepository.getUserByUserName(userName);
		model.addAttribute("title","User Dashboard");
		model.addAttribute("user",user);
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String addContactDetails(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact";
	}
	@PostMapping("/process-contact")
	public String processContactDetails(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {
		
		try {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		if(file.isEmpty()) {
			System.out.println("File is empty");
			
		}else {
			contact.setImage(file.getOriginalFilename());
			File saveFile = new ClassPathResource("static/image").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator+ file.getOriginalFilename());
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Image uploaded");
		}
		contact.setUsers(user);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		System.out.println(contact); 	
		session.setAttribute("message", new Message("Your contact is added.. Add more..!!", "success"));
		}catch(Exception e){
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong.. Try Again..!!", "danger"));

		}
		return "normal/add_contact";
	}
}
