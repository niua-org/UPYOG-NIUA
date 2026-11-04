package com.tarento.analytics.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class AmazonS3ClientServiceImplTest {

    @Mock
    private AmazonS3 amazonS3;

    private AmazonS3ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = mock(AmazonS3ClientServiceImpl.class, withSettings().defaultAnswer(org.mockito.Answers.CALLS_REAL_METHODS));
        ReflectionTestUtils.setField(service, "amazonS3", amazonS3);
        ReflectionTestUtils.setField(service, "awsS3AudioBucket", "test-bucket");
    }

    @Test
    void deleteFileFromS3Bucket_invokesClientDelete() {
        service.deleteFileFromS3Bucket("file.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(amazonS3).deleteObject(captor.capture());
        assertThat(captor.getValue().getBucketName()).isEqualTo("test-bucket");
        assertThat(captor.getValue().getKey()).isEqualTo("file.pdf");
    }
}
