package com.codegym.model;

import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

public class MedicalDeclarationForm {
    private int id;
    private String fullName;
    private Integer birthYear;
    private String gender;
    private String nationality;
    private String idCard;

    // Thông tin đi lại
    private String travelInfo;
    private String vehicleNumber;
    private String seatNumber;
    private String departureDate;
    private String arrivalDate;
    private String visitedCities;

    // Địa chỉ liên lạc
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private String phone;
    private String email;

    // Triệu chứng và lịch sử phơi nhiễm
    private List<String> symptoms = new ArrayList<>();
    private List<String> exposures = new ArrayList<>();

    // Tệp đính kèm xử lý qua Spring MultipartFile
    private MultipartFile documentFile;
    private String existingDocumentPath;
    private String existingDocumentName;

    public MedicalDeclarationForm() {
    }

    public MedicalDeclarationForm(MedicalDeclaration declaration) {
        if (declaration != null) {
            this.id = declaration.getId();
            this.fullName = declaration.getFullName();
            this.birthYear = declaration.getBirthYear();
            this.gender = declaration.getGender();
            this.nationality = declaration.getNationality();
            this.idCard = declaration.getIdCard();
            this.travelInfo = declaration.getTravelInfo();
            this.vehicleNumber = declaration.getVehicleNumber();
            this.seatNumber = declaration.getSeatNumber();
            this.departureDate = declaration.getDepartureDate();
            this.arrivalDate = declaration.getArrivalDate();
            this.visitedCities = declaration.getVisitedCities();
            this.province = declaration.getProvince();
            this.district = declaration.getDistrict();
            this.ward = declaration.getWard();
            this.addressDetail = declaration.getAddressDetail();
            this.phone = declaration.getPhone();
            this.email = declaration.getEmail();
            this.symptoms = declaration.getSymptoms() != null ? new ArrayList<>(declaration.getSymptoms()) : new ArrayList<>();
            this.exposures = declaration.getExposures() != null ? new ArrayList<>(declaration.getExposures()) : new ArrayList<>();
            this.existingDocumentPath = declaration.getDocumentPath();
            this.existingDocumentName = declaration.getDocumentName();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getTravelInfo() {
        return travelInfo;
    }

    public void setTravelInfo(String travelInfo) {
        this.travelInfo = travelInfo;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public String getVisitedCities() {
        return visitedCities;
    }

    public void setVisitedCities(String visitedCities) {
        this.visitedCities = visitedCities;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms;
    }

    public List<String> getExposures() {
        return exposures;
    }

    public void setExposures(List<String> exposures) {
        this.exposures = exposures;
    }

    public MultipartFile getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(MultipartFile documentFile) {
        this.documentFile = documentFile;
    }

    public String getExistingDocumentPath() {
        return existingDocumentPath;
    }

    public void setExistingDocumentPath(String existingDocumentPath) {
        this.existingDocumentPath = existingDocumentPath;
    }

    public String getExistingDocumentName() {
        return existingDocumentName;
    }

    public void setExistingDocumentName(String existingDocumentName) {
        this.existingDocumentName = existingDocumentName;
    }
}
