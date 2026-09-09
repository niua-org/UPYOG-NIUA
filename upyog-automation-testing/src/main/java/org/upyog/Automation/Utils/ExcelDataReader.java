package org.upyog.Automation.Utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelDataReader {

    private static final String EXCEL_FILE =
            "test-data/test-data.xlsx";

    /*
     * Uploaded Excel is the PRIMARY source.
     * Bundled Excel is used only when no Excel has been uploaded.
     */
    private static File uploadedExcelFile;


    // =========================================================
    // UPLOADED EXCEL MANAGEMENT
    // =========================================================

    public static void setUploadedExcelFile(File file) {
        uploadedExcelFile = file;
    }

    public static void clearUploadedExcelFile() {
        uploadedExcelFile = null;
    }

    public static boolean hasUploadedExcelFile() {
        return uploadedExcelFile != null
                && uploadedExcelFile.exists();
    }

    public static File getUploadedExcelFile() {
        return uploadedExcelFile;
    }


    // =========================================================
    // READ EXCEL SHEET
    // =========================================================

    public static List<Map<String, String>> readSheet(
            String sheetName) {

        List<Map<String, String>> testData =
                new ArrayList<>();

        /*
         * IMPORTANT:
         *
         * Uploaded Excel → PRIMARY
         * Bundled Excel   → FALLBACK
         */
        try (InputStream inputStream =
                     hasUploadedExcelFile()
                             ? new FileInputStream(
                             uploadedExcelFile)
                             : ExcelDataReader.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     EXCEL_FILE)) {

            if (inputStream == null) {
                throw new RuntimeException(
                        "Excel file not found: "
                                + EXCEL_FILE
                );
            }

            Workbook workbook =
                    WorkbookFactory.create(inputStream);

            Sheet sheet =
                    workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new RuntimeException(
                        "Sheet not found in Excel: "
                                + sheetName
                );
            }

            // First row contains column names
            Row headerRow =
                    sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException(
                        "Excel sheet is empty: "
                                + sheetName
                );
            }

            List<String> headers =
                    new ArrayList<>();

            DataFormatter formatter =
                    new DataFormatter();

            for (Cell cell : headerRow) {

                headers.add(
                        formatter
                                .formatCellValue(cell)
                                .trim()
                );
            }

            // Read each data row
            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                Map<String, String> rowData =
                        new LinkedHashMap<>();

                for (int columnIndex = 0;
                     columnIndex < headers.size();
                     columnIndex++) {

                    Cell cell =
                            row.getCell(
                                    columnIndex,
                                    Row.MissingCellPolicy
                                            .CREATE_NULL_AS_BLANK
                            );

                    rowData.put(
                            headers.get(columnIndex),
                            getCellValue(cell)
                    );
                }

                // Only execute rows where Execute = YES
                String execute =
                        rowData.get("Execute");

                if ("YES".equalsIgnoreCase(execute)) {
                    testData.add(rowData);
                }
            }

            workbook.close();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read Excel sheet: "
                            + sheetName,
                    e
            );
        }

        return testData;
    }


    // =========================================================
    // CHECK SHEET
    // =========================================================

    public static boolean hasSheet(
            String sheetName) {

        /*
         * Uploaded Excel → PRIMARY
         * Bundled Excel   → FALLBACK
         */
        try (InputStream inputStream =
                     hasUploadedExcelFile()
                             ? new FileInputStream(
                             uploadedExcelFile)
                             : ExcelDataReader.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     EXCEL_FILE)) {

            if (inputStream == null) {
                return false;
            }

            Workbook workbook =
                    WorkbookFactory.create(inputStream);

            boolean exists =
                    workbook.getSheet(sheetName) != null;

            workbook.close();

            return exists;

        } catch (Exception e) {

            return false;
        }
    }


    // =========================================================
    // CELL VALUE
    // =========================================================

    private static String getCellValue(
            Cell cell) {

        DataFormatter formatter =
                new DataFormatter();

        return formatter
                .formatCellValue(cell)
                .trim();
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        List<Map<String, String>> data =
                ExcelDataReader.readSheet(
                        "NDC_Test_Data"
                );

        System.out.println(
                "Total test cases: "
                        + data.size()
        );

        for (Map<String, String> row : data) {

            System.out.println(row);
        }
    }
}