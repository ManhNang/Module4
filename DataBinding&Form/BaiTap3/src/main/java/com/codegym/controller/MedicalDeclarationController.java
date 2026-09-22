package com.codegym.controller;

import com.codegym.model.MedicalDeclaration;
import com.codegym.model.MedicalDeclarationForm;
import com.codegym.service.IMedicalDeclarationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.Year;
import java.util.*;

@Controller
@RequestMapping({"/declarations", "/"})
public class MedicalDeclarationController {

    private final IMedicalDeclarationService declarationService;

    @Autowired
    public MedicalDeclarationController(IMedicalDeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @ModelAttribute("years")
    public List<Integer> getYears() {
        int currentYear = Year.now().getValue();
        List<Integer> years = new ArrayList<>();
        for (int i = currentYear; i >= 1920; i--) {
            years.add(i);
        }
        return years;
    }

    @ModelAttribute("genders")
    public List<String> getGenders() {
        return Arrays.asList("Nam", "Nữ", "Khác");
    }

    @ModelAttribute("nationalities")
    public List<String> getNationalities() {
        return Arrays.asList("Việt Nam", "Mỹ", "Hàn Quốc", "Nhật Bản", "Trung Quốc", "Pháp", "Đức", "Anh", "Úc", "Khác");
    }

    @ModelAttribute("provinces")
    public List<String> getProvinces() {
        return Arrays.asList("Hà Nội", "TP Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ", "Quảng Ninh", "Khánh Hòa", "Đồng Nai", "Bình Dương", "Huế", "Khác");
    }

    @ModelAttribute("vehicles")
    public List<String> getVehicles() {
        return Arrays.asList("Tàu bay", "Tàu thuyền", "Ô tô", "Khác");
    }

    @ModelAttribute("symptomOptions")
    public List<String> getSymptomOptions() {
        return Arrays.asList(
                "Sốt",
                "Ho",
                "Khó thở",
                "Đau họng",
                "Nôn / Buồn nôn",
                "Tiêu chảy",
                "Xuất huyết ngoài da",
                "Nổi ban ngoài da"
        );
    }

    @ModelAttribute("exposureOptions")
    public List<String> getExposureOptions() {
        return Arrays.asList(
                "Đến trang trại chăn nuôi / chợ buôn bán động vật sống / tiếp xúc động vật",
                "Tiếp xúc gần (< 2m) với người mắc bệnh hoặc nghi ngờ mắc bệnh viêm đường hô hấp do nCoV"
        );
    }

    // Danh sách tờ khai y tế
    @GetMapping({"", "list"})
    public String listDeclarations(Model model) {
        List<MedicalDeclaration> declarations = declarationService.findAll();
        model.addAttribute("declarations", declarations);
        return "list";
    }

    // Mở form khai báo y tế mới
    @GetMapping("create")
    public String showCreateForm(Model model) {
        MedicalDeclarationForm form = new MedicalDeclarationForm();
        // Gán một số giá trị mặc định cho tiện người dùng
        form.setNationality("Việt Nam");
        form.setGender("Nam");
        form.setTravelInfo("Tàu bay");
        model.addAttribute("declarationForm", form);
        return "create";
    }

    // Xử lý nộp tờ khai y tế mới
    @PostMapping("save")
    public String saveDeclaration(@ModelAttribute("declarationForm") MedicalDeclarationForm form,
                                  Model model,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Map<String, String> errors = validateRequiredFields(form);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("missingFields", new ArrayList<>(errors.values()));
            return "create";
        }

        // Xử lý upload file đính kèm
        String documentPath = null;
        String documentName = null;
        MultipartFile multipartFile = form.getDocumentFile();
        if (multipartFile != null && !multipartFile.isEmpty()) {
            try {
                String uploadDir = request.getServletContext().getRealPath("/uploads/");
                File uploadFolder = new File(uploadDir);
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdirs();
                }
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
                String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
                File destinationFile = new File(uploadFolder, uniqueFilename);
                multipartFile.transferTo(destinationFile);
                documentPath = "/uploads/" + uniqueFilename;
                documentName = originalFilename;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MedicalDeclaration declaration = new MedicalDeclaration(
                0,
                form.getFullName().trim().toUpperCase(),
                form.getBirthYear(),
                form.getGender(),
                form.getNationality(),
                form.getIdCard().trim(),
                form.getTravelInfo(),
                form.getVehicleNumber(),
                form.getSeatNumber(),
                form.getDepartureDate(),
                form.getArrivalDate(),
                form.getVisitedCities(),
                form.getProvince(),
                form.getDistrict(),
                form.getWard(),
                form.getAddressDetail(),
                form.getPhone().trim(),
                form.getEmail(),
                form.getSymptoms(),
                form.getExposures(),
                documentPath,
                documentName
        );

        declarationService.save(declaration);
        redirectAttributes.addFlashAttribute("successMessage", "Gửi tờ khai y tế thành công!");
        return "redirect:/declarations/view?id=" + declaration.getId();
    }

    // Xem chi tiết thông tin tờ khai đã khai báo
    @GetMapping("view")
    public String viewDeclaration(@RequestParam("id") int id, Model model, RedirectAttributes redirectAttributes) {
        MedicalDeclaration declaration = declarationService.findById(id);
        if (declaration == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tờ khai y tế với mã: " + id);
            return "redirect:/declarations/list";
        }
        model.addAttribute("declaration", declaration);
        return "view";
    }

    // Mở form chỉnh sửa / cập nhật thông tin tờ khai
    @GetMapping("edit")
    public String showEditForm(@RequestParam("id") int id, Model model, RedirectAttributes redirectAttributes) {
        MedicalDeclaration declaration = declarationService.findById(id);
        if (declaration == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tờ khai y tế với mã: " + id);
            return "redirect:/declarations/list";
        }
        MedicalDeclarationForm form = new MedicalDeclarationForm(declaration);
        model.addAttribute("declarationForm", form);
        return "edit";
    }

    // Xử lý cập nhật thông tin tờ khai
    @PostMapping("update")
    public String updateDeclaration(@ModelAttribute("declarationForm") MedicalDeclarationForm form,
                                    Model model,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        Map<String, String> errors = validateRequiredFields(form);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("missingFields", new ArrayList<>(errors.values()));
            return "edit";
        }

        MedicalDeclaration existing = declarationService.findById(form.getId());
        String documentPath = form.getExistingDocumentPath();
        String documentName = form.getExistingDocumentName();

        MultipartFile multipartFile = form.getDocumentFile();
        if (multipartFile != null && !multipartFile.isEmpty()) {
            try {
                String uploadDir = request.getServletContext().getRealPath("/uploads/");
                File uploadFolder = new File(uploadDir);
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdirs();
                }
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
                String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
                File destinationFile = new File(uploadFolder, uniqueFilename);
                multipartFile.transferTo(destinationFile);
                documentPath = "/uploads/" + uniqueFilename;
                documentName = originalFilename;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MedicalDeclaration updatedDeclaration = new MedicalDeclaration(
                form.getId(),
                form.getFullName().trim().toUpperCase(),
                form.getBirthYear(),
                form.getGender(),
                form.getNationality(),
                form.getIdCard().trim(),
                form.getTravelInfo(),
                form.getVehicleNumber(),
                form.getSeatNumber(),
                form.getDepartureDate(),
                form.getArrivalDate(),
                form.getVisitedCities(),
                form.getProvince(),
                form.getDistrict(),
                form.getWard(),
                form.getAddressDetail(),
                form.getPhone().trim(),
                form.getEmail(),
                form.getSymptoms(),
                form.getExposures(),
                documentPath,
                documentName
        );

        declarationService.update(form.getId(), updatedDeclaration);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tờ khai y tế thành công!");
        return "redirect:/declarations/view?id=" + form.getId();
    }

    // Kiểm tra các trường thông tin bắt buộc
    private Map<String, String> validateRequiredFields(MedicalDeclarationForm form) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (form.getFullName() == null || form.getFullName().trim().isEmpty()) {
            errors.put("fullName", "Họ và tên là mục bắt buộc");
        }
        if (form.getBirthYear() == null) {
            errors.put("birthYear", "Năm sinh là mục bắt buộc");
        }
        if (form.getGender() == null || form.getGender().trim().isEmpty()) {
            errors.put("gender", "Giới tính là mục bắt buộc");
        }
        if (form.getNationality() == null || form.getNationality().trim().isEmpty()) {
            errors.put("nationality", "Quốc tịch là mục bắt buộc");
        }
        if (form.getIdCard() == null || form.getIdCard().trim().isEmpty()) {
            errors.put("idCard", "Số CMND / CCCD / Hộ chiếu là mục bắt buộc");
        }
        if (form.getProvince() == null || form.getProvince().trim().isEmpty()) {
            errors.put("province", "Tỉnh / Thành phố là mục bắt buộc");
        }
        if (form.getDistrict() == null || form.getDistrict().trim().isEmpty()) {
            errors.put("district", "Quận / Huyện là mục bắt buộc");
        }
        if (form.getWard() == null || form.getWard().trim().isEmpty()) {
            errors.put("ward", "Phường / Xã là mục bắt buộc");
        }
        if (form.getPhone() == null || form.getPhone().trim().isEmpty()) {
            errors.put("phone", "Số điện thoại liên lạc là mục bắt buộc");
        }

        return errors;
    }
}
