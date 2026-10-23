package org.upyog.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommonUtilsTest {

    @Test
    @DisplayName("getCurrentEpochMillis returns a positive timestamp near now")
    void getCurrentEpochMillis_returnsPositiveValue() {
        long before = System.currentTimeMillis();
        long epoch = CommonUtils.getCurrentEpochMillis();
        long after = System.currentTimeMillis();

        assertThat(epoch).isBetween(before, after);
    }

    @Test
    @DisplayName("generateUUID returns a non-blank UUID string")
    void generateUUID_returnsUuidString() {
        String uuid = CommonUtils.generateUUID();
        assertThat(uuid).isNotBlank();
        assertThat(uuid).matches("[0-9a-f\\-]{36}");
    }

    @Test
    @DisplayName("buildS3Key omits blank segments and appends file name")
    void buildS3Key_buildsExpectedPath() {
        String key = CommonUtils.buildS3Key("UPYOG", "pg", "PT", "report.xlsx");

        assertThat(key).startsWith("UPYOG/pg/PT/");
        assertThat(key).endsWith("_report.xlsx");
    }

    @Test
    @DisplayName("buildS3Key handles null folder and null file name")
    void buildS3Key_handlesNullFolderAndFileName() {
        String key = CommonUtils.buildS3Key(null, "pg", "PT", null);

        assertThat(key).startsWith("pg/PT/");
        assertThat(key).endsWith("_file");
    }

    @Test
    @DisplayName("buildS3Key trims whitespace-only folder")
    void buildS3Key_skipsBlankFolder() {
        String key = CommonUtils.buildS3Key("   ", "pg", "PT", "a.csv");

        assertThat(key).startsWith("pg/PT/");
        assertThat(key).doesNotStartWith("   ");
    }
}
