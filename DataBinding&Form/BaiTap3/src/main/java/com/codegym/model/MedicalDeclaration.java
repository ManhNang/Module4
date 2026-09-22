package com.codegym.model;

import java.util.ArrayList;
import java.util.List;

public class MedicalDeclaration {
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

    // Tệp đính kèm (giấy chứng nhận tiêm chủng / xét nghiệm / CCCD)
    private String documentPath;
    private String documentName;

    public MedicalDeclaration() {
    }

    public MedicalDeclaration(int id, String fullName, Integer birthYear, String gender, String nationality,
                              String idCard, String travelInfo, String vehicleNumber, String seatNumber,
                              String departureDate, String arrivalDate, String visitedCities, String province,
                              String district, String ward, String addressDetail, String phone, String email,
                              List<String> symptoms, List<String> exposures, String documentPath, String documentName) {
        this.id = id;
        this.fullName = fullName;
        this.birthYear = birthYear;
        this.gender = gender;
        this.nationality = nationality;
        this.idCard = idCard;
        this.travelInfo = travelInfo;
        this.vehicleNumber = vehicleNumber;
        this.seatNumber = seatNumber;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.visitedCities = visitedCities;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.addressDetail = addressDetail;
        this.phone = phone;
        this.email = email;
        this.symptoms = symptoms != null ? symptoms : new ArrayList<>();
        this.exposures = exposures != null ? exposures : new ArrayList<>();
        this.documentPath = documentPath;
        this.documentName = documentName;
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

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}
