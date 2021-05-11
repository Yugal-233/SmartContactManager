package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
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
			contact.setImage("saara1.jpg");
			
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
	
	@GetMapping("/view-contacts/{page}")
	public String viewContactDetails(@PathVariable("page") Integer page, Model model,Principal principal) {
		model.addAttribute("title", "View Contacts Details");
		String userName =  principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/view_contacts";
	}
	
	@GetMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model,Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		String userName= principal.getName();
		User user =this.userRepository.getUserByUserName(userName);
		if(user.getId()==contact.getUsers().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getContactName());
		}
		return "normal/contact_details";
	}
	 
	@GetMapping("/delete/{cid}")
	@Transactional
	public String deleteContact(@PathVariable("cid") Integer cId, Model Model, HttpSession session, Principal principal) {
		System.out.println("CID :: "+cId);
		Contact contact = this.contactRepository.findById(cId).get();
		/*
		 * contact.setUsers(null); this.contactRepository.delete(contact);
		 */
		
		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		session.setAttribute("message", new Message("Contact Deleted Successfully", "success"));
		return "redirect:/user/view-contacts/0";
	}
	
	/*
	 * @GetMapping("/delete") public String deleteByID(Integer cId) {
	 * 
	 * List<Contact> clist=contactRepository.findAll(); Contact ct=clist.stream()
	 * .filter(contact->cId.equals(contact.getcId())) .findAny() .orElse(null);
	 * contactRepository.delete(ct); return "redirect:/user/view-contacts/0"; }
	 */
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model m)
	{
		m.addAttribute("title","Update Contact");
		Contact contact = this.contactRepository.findById(cId).get();
		m.addAttribute("contact", contact);
		return "normal/update-form";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model m, HttpSession session, Principal principal) {
		
		try {
			
			Contact oldContactDet = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty()) {
				
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldContactDet.getImage());
				file1.delete();
				
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator+ file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldContactDet.getImage());
			}
			User user =  this.userRepository.getUserByUserName(principal.getName());
			contact.setUsers(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your Contact is is updated", "success"));
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return "redirect:/user/"+contact.getcId()+"/contact"; 
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model m) {
		
		m.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
}












