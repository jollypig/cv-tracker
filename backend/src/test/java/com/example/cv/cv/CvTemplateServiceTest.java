package com.example.cv.cv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvTemplateServiceTest {

    @Mock
    private CvTemplateRepository templateRepository;

    @InjectMocks
    private CvTemplateService templateService;

    @Test
    void findAllReturnsOnlyActiveTemplatesInRepositoryOrder() {
        CvTemplate modern = org.mockito.Mockito.mock(CvTemplate.class);
        when(templateRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(modern));
        when(modern.getTemplateKey()).thenReturn("modern");

        List<CvTemplateResponse> result = templateService.findAll();

        assertEquals(1, result.size());
        assertEquals("modern", result.get(0).templateKey());
        verify(templateRepository).findAllByActiveTrueOrderByNameAsc();
    }

    @Test
    void findReturnsNotFoundWhenTemplateDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> templateService.find(id));

        assertEquals(404, error.getStatusCode().value());
    }
}