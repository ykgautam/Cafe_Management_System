package com.inn.cafe.serviceImpl;

import com.inn.cafe.JWT.JwtFilter;
import com.inn.cafe.POJO.Bill;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.dao.BillDao;
import com.inn.cafe.service.BillService;
import com.inn.cafe.utils.CafeUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class BillServiceImpl implements BillService {


    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    BillDao dao;

    @Override
    public ResponseEntity<String> generateReport(Map<String, Object> requestMap) {
        try {
            log.info("inside generateReport");
            String fileName;
            if (validaterequestMap(requestMap)) {
                if (requestMap.containsKey("isGenerate") && !(Boolean) requestMap.get("isGenerate")) {
                    fileName = (String) requestMap.get("uuid");
                } else {
//                    generate new file
                    fileName = CafeUtils.getUUID();
                    requestMap.put("uuid", fileName);
                    insertBill(requestMap);
                }
                String data = "Name: " + requestMap.get("name") + "\n" +
                        "Contact Number: " + requestMap.get("contactNumber") + "\n" +
                        "Email: " + requestMap.get("email") + "\n" +
                        "Payment Method " + requestMap.get("paymentMethod");

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(CafeConstants.STORE_LOCATION + "\\" + fileName + ".pdf"));
                document.open();
                setRectangleInPdf(document);

                Paragraph chunk = new Paragraph("Cafe Management System", getFont("Header"));
                chunk.setAlignment(Element.ALIGN_CENTER);
                document.add(chunk);

                Paragraph paragraph = new Paragraph(data + "\n \n");
                document.add(paragraph);

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                addTableHeader(table);

                JSONArray jsonArray = CafeUtils.getJsonArrayfromString((String) requestMap.get("productDetails"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    addRows(table, CafeUtils.getMapFromJson(jsonArray.getString(i)));
                }
                document.add(table);

                Paragraph footer = new Paragraph("Total : " + requestMap.get("totalAmount") + "\n"
                        + "Thank you for visiting. Please visit again", getFont("Data"));

                document.add(footer);
                document.close();
                return new ResponseEntity<>("{\"uuid\":\"" + fileName + "\"}", HttpStatus.OK);

            } else {
                return CafeUtils.getResponseEntity("Required data not found", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<Bill>> getBills() {
        List<Bill> list = new ArrayList<>();
        if (jwtFilter.isAdmin()) {
            list = dao.getAllBills();
        } else {
            list = dao.getBillByUsername(jwtFilter.getCurrentUser());
        }
        return new ResponseEntity<>(list, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<byte[]> getPdf(Map<String, Object> requestMap) {
        log.info("inside getPdf : requestMap {}", requestMap);
        try {
            byte[] byteArray = new byte[0];
            if (!requestMap.containsKey("uuid") && validaterequestMap(requestMap)) {
                return new ResponseEntity<>(byteArray, HttpStatus.BAD_REQUEST);
            }
            String filePath = CafeConstants.STORE_LOCATION + "\\" + (String) requestMap.get("uuid") + ".pdf";
            Boolean fileExist = CafeUtils.isFileExist(filePath);
            if (fileExist) {
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray, HttpStatus.OK);
            } else {
                requestMap.put("isGenerate", false);
                generateReport(requestMap);
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray, HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResponseEntity<String> deleteBill(Integer id) {
        try {
            Optional<Bill> optional = dao.findById(id);
            if (!optional.isEmpty()) {
                dao.deleteById(id);
                return CafeUtils.getResponseEntity("Bill deleted successfully.", HttpStatus.OK);

            }
            return CafeUtils.getResponseEntity("Bill id does not exist", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    private byte[] getByteArray(String filePath) throws Exception {
        File initialfile = new File(filePath);
        InputStream targetStream = new FileInputStream(initialfile);
        byte[] byteArray = IOUtils.toByteArray(targetStream);
        targetStream.close();
        return byteArray;
    }

    private void addRows(PdfPTable table, Map<String, Object> data) {
        log.info("inside addRows");
        table.addCell((String) data.get("name"));
        table.addCell((String) data.get("category"));
        table.addCell((String) data.get("quantity"));
        table.addCell(Double.toString((Double) data.get("price")));
        table.addCell(Double.toString((Double) data.get("total")));
    }

    private void addTableHeader(PdfPTable table) {
        log.info("inside addTableHeader");
        Stream.of("Name", "Category", "Quantity", "Price", "Sub total")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    header.setVerticalAlignment(Element.ALIGN_CENTER);
                    table.addCell(header);
                });
    }

    private Font getFont(String type) {

        log.info("inside getFont");
        switch (type) {
            case "Header":
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 18, BaseColor.BLACK);
                headerFont.setStyle(Font.BOLD);
                return headerFont;
            case "Data":
                Font dataFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, BaseColor.BLACK);
                dataFont.setStyle(Font.BOLD);
                return dataFont;
            default:
                return new Font();


        }
    }

    private void setRectangleInPdf(Document document) throws DocumentException {
        log.info("inside setRectangleInPdf");
        Rectangle rectangle = new Rectangle(577, 825, 18, 15);
        rectangle.enableBorderSide(1);
        rectangle.enableBorderSide(2);
        rectangle.enableBorderSide(4);
        rectangle.enableBorderSide(8);

        rectangle.setBackgroundColor(BaseColor.GRAY);
        rectangle.setBorderWidth(1);
        document.add(rectangle);
    }

    private void insertBill(Map<String, Object> requestMap) {
        try {
            Bill bill = new Bill();
            bill.setUuid((String) requestMap.get("uuid"));
            bill.setEmail((String) requestMap.get("email"));
            bill.setName((String) requestMap.get("name"));
            bill.setContactNumber((String) requestMap.get("contactNumber"));
            bill.setPaymentMethod((String) requestMap.get("paymentMethod"));
            bill.setTotal(Integer.parseInt((String) requestMap.get("totalAmount")));
            bill.setProductDetails((String) requestMap.get("productDetails"));
            bill.setCreatedBy(jwtFilter.getCurrentUser());

            dao.save(bill);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean validaterequestMap(Map<String, Object> requestMap) {
        return requestMap.containsKey("name")
                && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email")
                && requestMap.containsKey("paymentMethod")
                && requestMap.containsKey("productDetails")
                && requestMap.containsKey("totalAmount");
    }
}
