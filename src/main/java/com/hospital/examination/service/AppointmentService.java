package com.hospital.examination.service;

import com.hospital.examination.model.*;
import com.hospital.examination.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AppointmentService {
    private static final String[] TEMPLATE_HEADERS = {
            "姓名*", "性别*", "出生日期*", "手机号*", "身份证号", "部门", "工号", "地址", "既往病史"
    };
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final DateTimeFormatter APPOINTMENT_NO_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AppointmentRepository appointmentRepository;
    private final OrganizationRepository organizationRepository;
    private final PatientRepository patientRepository;
    private final CheckupPackageRepository packageRepository;
    private final DoctorRepository doctorRepository;
    private final ExamOrderService examOrderService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              OrganizationRepository organizationRepository,
                              PatientRepository patientRepository,
                              CheckupPackageRepository packageRepository,
                              DoctorRepository doctorRepository,
                              ExamOrderService examOrderService) {
        this.appointmentRepository = appointmentRepository;
        this.organizationRepository = organizationRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.examOrderService = examOrderService;
    }

    @Transactional
    public Appointment createPersonal(Long patientId, Long packageId, Long doctorId,
                                      LocalDate appointmentDate, String remark) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("受检人不存在"));
        Appointment appointment = baseAppointment(AppointmentType.PERSONAL, packageId, doctorId,
                appointmentDate, remark);
        AppointmentParticipant participant = new AppointmentParticipant();
        participant.setPatient(patient);
        appointment.addParticipant(participant);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment createOrganization(String organizationName, String creditCode,
                                          String contactName, String contactPhone, String address,
                                          Long packageId, Long doctorId, LocalDate appointmentDate,
                                          String remark, MultipartFile participantFile) {
        validateOrganization(organizationName, contactName, contactPhone);
        Organization organization = findOrCreateOrganization(organizationName, creditCode,
                contactName, contactPhone, address);
        Appointment appointment = baseAppointment(AppointmentType.ORGANIZATION, packageId, doctorId,
                appointmentDate, remark);
        appointment.setOrganization(organization);
        appointment = appointmentRepository.save(appointment);
        if (participantFile != null && !participantFile.isEmpty()) {
            importParticipants(appointment, participantFile);
        }
        return appointment;
    }

    private Appointment baseAppointment(AppointmentType type, Long packageId, Long doctorId,
                                        LocalDate appointmentDate, String remark) {
        if (appointmentDate == null || appointmentDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("预约日期不能早于今天");
        }
        CheckupPackage checkupPackage = packageRepository.findById(packageId)
                .filter(CheckupPackage::isEnabled)
                .orElseThrow(() -> new EntityNotFoundException("体检套餐不存在或已停用"));
        if ((type == AppointmentType.PERSONAL
                && checkupPackage.getType() != PackageType.PERSONAL)
                || (type == AppointmentType.ORGANIZATION
                && checkupPackage.getType() != PackageType.ORGANIZATION)) {
            throw new IllegalArgumentException("所选套餐类型与预约类型不一致");
        }
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(nextAppointmentNo());
        appointment.setType(type);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setCheckupPackage(checkupPackage);
        appointment.setDoctor(doctorId == null ? null : doctorRepository.findById(doctorId).orElse(null));
        appointment.setAppointmentDate(appointmentDate);
        appointment.setRemark(trimToNull(remark));
        return appointment;
    }

    private void validateOrganization(String name, String contactName, String contactPhone) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("单位名称不能为空");
        }
        if (isBlank(contactName)) {
            throw new IllegalArgumentException("单位联系人不能为空");
        }
        if (isBlank(contactPhone) || !PHONE_PATTERN.matcher(contactPhone.trim()).matches()) {
            throw new IllegalArgumentException("请输入正确的单位联系人手机号");
        }
    }

    private Organization findOrCreateOrganization(String name, String creditCode, String contactName,
                                                  String contactPhone, String address) {
        String normalizedCreditCode = trimToNull(creditCode);
        Optional<Organization> existing = normalizedCreditCode == null
                ? organizationRepository.findFirstByNameIgnoreCase(name.trim())
                : organizationRepository.findByCreditCode(normalizedCreditCode);
        Organization organization = existing.orElseGet(Organization::new);
        organization.setName(name.trim());
        organization.setCreditCode(normalizedCreditCode);
        organization.setContactName(contactName.trim());
        organization.setContactPhone(contactPhone.trim());
        organization.setAddress(trimToNull(address));
        return organizationRepository.save(organization);
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("预约记录不存在"));
        appointment.getParticipants().size();
        return appointment;
    }

    @Transactional
    public ExamOrder checkIn(Long appointmentId, Long participantId) {
        Appointment appointment = get(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("已取消的预约不能签到");
        }
        AppointmentParticipant participant = appointment.getParticipants().stream()
                .filter(item -> item.getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("参检人不属于当前预约"));
        if (participant.getExamOrder() != null) {
            return participant.getExamOrder();
        }
        ExamOrder order = examOrderService.create(participant.getPatient().getId(),
                appointment.getCheckupPackage().getId(),
                appointment.getDoctor() == null ? null : appointment.getDoctor().getId(),
                appointment.getAppointmentDate());
        participant.setExamOrder(order);
        return order;
    }

    @Transactional
    public int importParticipants(Long appointmentId, MultipartFile file) {
        Appointment appointment = get(appointmentId);
        return importParticipants(appointment, file);
    }

    private int importParticipants(Appointment appointment, MultipartFile file) {
        if (appointment.getType() != AppointmentType.ORGANIZATION) {
            throw new IllegalStateException("仅单位体检预约支持批量导入");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("已取消的预约不能导入参检人");
        }
        List<ParticipantRow> rows = readRows(file);
        Set<Long> existingPatientIds = new HashSet<>();
        appointment.getParticipants().forEach(item -> existingPatientIds.add(item.getPatient().getId()));
        List<String> errors = new ArrayList<>();
        List<PendingParticipant> pending = new ArrayList<>();
        Set<String> fileIdentities = new HashSet<>();

        for (ParticipantRow row : rows) {
            try {
                Patient patient = resolvePatient(row);
                String identity = patient.getId() == null
                        ? (row.idCard() == null ? "PHONE:" + row.phone() : "ID:" + row.idCard())
                        : "PATIENT:" + patient.getId();
                if (!fileIdentities.add(identity)) {
                    errors.add("第" + row.rowNumber() + "行：同一文件中人员重复");
                } else if (patient.getId() != null && existingPatientIds.contains(patient.getId())) {
                    errors.add("第" + row.rowNumber() + "行：" + row.name() + " 已在本预约中");
                } else {
                    pending.add(new PendingParticipant(row, patient));
                }
            } catch (IllegalArgumentException ex) {
                errors.add("第" + row.rowNumber() + "行：" + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new AppointmentImportException(errors);
        }

        for (PendingParticipant item : pending) {
            Patient patient = item.patient();
            if (patient.getId() == null) {
                patient = patientRepository.save(patient);
            }
            AppointmentParticipant participant = new AppointmentParticipant();
            participant.setPatient(patient);
            participant.setDepartment(item.row().department());
            participant.setEmployeeNo(item.row().employeeNo());
            appointment.addParticipant(participant);
        }
        appointmentRepository.save(appointment);
        return pending.size();
    }

    private Patient resolvePatient(ParticipantRow row) {
        Optional<Patient> existing = row.idCard() == null
                ? patientRepository.findFirstByPhone(row.phone())
                : patientRepository.findByIdCard(row.idCard())
                        .or(() -> patientRepository.findFirstByPhone(row.phone()));
        if (existing.isPresent()) {
            Patient patient = existing.get();
            if (!patient.getName().equals(row.name())) {
                throw new IllegalArgumentException("证件号或手机号已属于其他姓名，无法自动关联");
            }
            if (!patient.getPhone().equals(row.phone())) {
                throw new IllegalArgumentException("已有档案手机号与导入内容不一致");
            }
            return patient;
        }

        Patient patient = new Patient();
        patient.setName(row.name());
        patient.setGender(row.gender());
        patient.setBirthday(row.birthday());
        patient.setPhone(row.phone());
        patient.setIdCard(row.idCard());
        patient.setAddress(row.address());
        patient.setMedicalHistory(row.medicalHistory());
        return patient;
    }

    private List<ParticipantRow> readRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要导入的 Excel 文件");
        }
        List<String> errors = new ArrayList<>();
        List<ParticipantRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getRow(0) == null) {
                throw new IllegalArgumentException("Excel 中没有可读取的数据");
            }
            validateHeaders(sheet.getRow(0), formatter);
            int lastRow = Math.min(sheet.getLastRowNum(), 1000);
            for (int index = 1; index <= lastRow; index++) {
                Row row = sheet.getRow(index);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }
                int rowNumber = index + 1;
                try {
                    rows.add(parseRow(row, rowNumber, formatter));
                } catch (IllegalArgumentException ex) {
                    errors.add("第" + rowNumber + "行：" + ex.getMessage());
                }
            }
            if (sheet.getLastRowNum() > 1000) {
                errors.add("单次最多导入1000人");
            }
        } catch (AppointmentImportException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel 文件无法读取，请使用系统下载的 .xlsx 模板", ex);
        }
        if (rows.isEmpty() && errors.isEmpty()) {
            errors.add("Excel 中没有参检人数据");
        }
        if (!errors.isEmpty()) {
            throw new AppointmentImportException(errors);
        }
        return rows;
    }

    private void validateHeaders(Row headerRow, DataFormatter formatter) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            String actual = formatter.formatCellValue(headerRow.getCell(i)).trim();
            if (!TEMPLATE_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException("模板表头不正确，请重新下载最新模板");
            }
        }
    }

    private ParticipantRow parseRow(Row row, int rowNumber, DataFormatter formatter) {
        String name = required(formatter.formatCellValue(row.getCell(0)), "姓名不能为空");
        String genderText = required(formatter.formatCellValue(row.getCell(1)), "性别不能为空");
        Gender gender = switch (genderText.trim()) {
            case "男", "MALE" -> Gender.MALE;
            case "女", "FEMALE" -> Gender.FEMALE;
            default -> throw new IllegalArgumentException("性别只能填写男或女");
        };
        LocalDate birthday = parseBirthday(row.getCell(2), formatter);
        String phone = required(formatter.formatCellValue(row.getCell(3)), "手机号不能为空");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("手机号必须为11位中国大陆手机号");
        }
        String idCard = trimToNull(formatter.formatCellValue(row.getCell(4)));
        if (idCard != null && (idCard.length() < 15 || idCard.length() > 18)) {
            throw new IllegalArgumentException("身份证号长度应为15至18位");
        }
        return new ParticipantRow(rowNumber, name.trim(), gender, birthday, phone.trim(), idCard,
                trimToNull(formatter.formatCellValue(row.getCell(5))),
                trimToNull(formatter.formatCellValue(row.getCell(6))),
                trimToNull(formatter.formatCellValue(row.getCell(7))),
                trimToNull(formatter.formatCellValue(row.getCell(8))));
    }

    private LocalDate parseBirthday(Cell cell, DataFormatter formatter) {
        LocalDate birthday;
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            birthday = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            String value = required(formatter.formatCellValue(cell), "出生日期不能为空");
            try {
                birthday = LocalDate.parse(value.trim(), BIRTHDAY_FORMAT);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("出生日期格式应为 yyyy-MM-dd");
            }
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("出生日期不能晚于今天");
        }
        return birthday;
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (!formatter.formatCellValue(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public byte[] createImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("参检人信息");
            sheet.createFreezePane(0, 1);
            sheet.setDisplayGridlines(false);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            CellStyle inputStyle = workbook.createCellStyle();
            inputStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            inputStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            inputStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(inputStyle);
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            Row header = sheet.createRow(0);
            header.setHeightInPoints(24);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, switch (i) {
                    case 0, 1, 5, 6 -> 14 * 256;
                    case 2, 3, 4 -> 20 * 256;
                    default -> 28 * 256;
                });
            }
            for (int rowIndex = 1; rowIndex <= 100; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(22);
                for (int col = 0; col < TEMPLATE_HEADERS.length; col++) {
                    row.createCell(col).setCellStyle(col == 2 ? dateStyle : inputStyle);
                }
            }

            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidation validation = helper.createValidation(
                    helper.createExplicitListConstraint(new String[]{"男", "女"}),
                    new CellRangeAddressList(1, 100, 1, 1));
            validation.setShowErrorBox(true);
            validation.createErrorBox("性别填写错误", "请选择男或女");
            sheet.addValidationData(validation);

            Sheet instructions = workbook.createSheet("填写说明");
            instructions.setDisplayGridlines(false);
            String[][] notes = {
                    {"单位体检参检人导入说明", ""},
                    {"必填字段", "姓名、性别、出生日期、手机号"},
                    {"日期格式", "yyyy-MM-dd，例如 1990-05-20"},
                    {"性别", "仅填写“男”或“女”"},
                    {"手机号", "11位中国大陆手机号"},
                    {"导入规则", "整表校验；任一行错误时不会导入任何人员"},
                    {"档案关联", "身份证号优先，其次手机号；已有档案不会被导入内容覆盖"},
                    {"单次上限", "1000人"}
            };
            for (int i = 0; i < notes.length; i++) {
                Row row = instructions.createRow(i);
                row.createCell(0).setCellValue(notes[i][0]);
                row.createCell(1).setCellValue(notes[i][1]);
            }
            instructions.setColumnWidth(0, 20 * 256);
            instructions.setColumnWidth(1, 62 * 256);
            instructions.getRow(0).getCell(0).setCellStyle(headerStyle);
            instructions.getRow(0).getCell(1).setCellStyle(headerStyle);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成导入模板失败", ex);
        }
    }

    @Transactional
    public void cancel(Long id) {
        Appointment appointment = get(id);
        appointment.setStatus(AppointmentStatus.CANCELLED);
    }

    private String nextAppointmentNo() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        long count = appointmentRepository.countByCreatedAtBetween(start, start.plusDays(1)) + 1;
        return "AP" + today.format(APPOINTMENT_NO_DATE) + String.format("%04d", count);
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private record ParticipantRow(int rowNumber, String name, Gender gender, LocalDate birthday,
                                  String phone, String idCard, String department, String employeeNo,
                                  String address, String medicalHistory) {
    }

    private record PendingParticipant(ParticipantRow row, Patient patient) {
    }
}
