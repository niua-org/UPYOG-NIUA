// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.repository.querybuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.tp.config.TreePruningConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TreePruningQueryBuilderTest {
    @Mock
    private TreePruningConfiguration treePruningConfiguration;

    private TreePruningQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new TreePruningQueryBuilder(treePruningConfiguration);
    }


    @Test
    void getTreePruningQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
