package de.caritas.cob.mailservice.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import de.caritas.cob.mailservice.api.exception.TemplateServiceException;
import de.caritas.cob.mailservice.api.mailtemplate.SubjectDescription;
import de.caritas.cob.mailservice.api.mailtemplate.TemplateDescription;
import de.caritas.cob.mailservice.api.model.Dialect;
import de.caritas.cob.mailservice.api.model.LanguageCode;
import de.caritas.cob.mailservice.api.model.MailDTO;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceTest {

  private final String TEMPLATE_NAME = "test";
  private final String PLACEHOLDER1 = "placeholder1";
  private final String PLACEHOLDER1_VALUE = "test1";
  private final String PLACEHOLDER2 = "placeholder2";
  private final String PLACEHOLDER2_VALUE = "test2";
  private List<String> FIELDS = Arrays.asList(PLACEHOLDER1, PLACEHOLDER2);
  private final String SUBJECT_WITH_PLACEHOLDERS =
      "Test ${" + PLACEHOLDER1 + "} Test ${" + PLACEHOLDER2 + "}";
  private final String SUBJECT_WITH_REPLACED_PLACEHOLDERS =
      "Test " + PLACEHOLDER1_VALUE + " Test " + PLACEHOLDER2_VALUE;
  private final TemplateDescription TEMPLATE_DESCRIPTION =
      new TemplateDescription(
          Map.of(LanguageCode.DE, "test.html"),
          new SubjectDescription("translationKey"),
          FIELDS,
          null);

  @SuppressWarnings("serial")
  private final Map<String, Object> TEMPLATE_DATA =
      new HashMap<String, Object>() {
        {
          put(PLACEHOLDER1, PLACEHOLDER1_VALUE);
          put(PLACEHOLDER2, PLACEHOLDER2_VALUE);
        }
      };

  @SuppressWarnings("serial")
  private final Map<String, Object> TEMPLATE_DATA_WITH_MISSING_FIELD =
      new HashMap<>() {
        {
          put(PLACEHOLDER1, PLACEHOLDER1_VALUE);
        }
      };

  private TemplateService templateService;

  @Mock private TranslationService translationService;

  @Before
  public void setup() {
    this.templateService = new TemplateService(translationService);
  }

  @Test
  public void getProcessedSubject_Should_ReturnSubjectWithReplacedPlaceholders() {

    when(translationService.tryFetchTranslations(LanguageCode.DE.getValue(), Dialect.INFORMAL))
        .thenReturn(Optional.of(Map.of("translationKey", SUBJECT_WITH_PLACEHOLDERS)));

    var result =
        templateService.getRenderedSubject(
            TEMPLATE_DESCRIPTION,
            TEMPLATE_DATA,
            new MailDTO()
                .template(TEMPLATE_NAME)
                .language(LanguageCode.DE)
                .dialect(Dialect.INFORMAL));
    assertEquals(SUBJECT_WITH_REPLACED_PLACEHOLDERS, result);
  }

  @Test
  public void getProcessedHtmlTemplate_Should_ThrowServiceException_WhenTemplateDataIsMissing() {

    try {
      templateService.render(
          TEMPLATE_DESCRIPTION,
          new MailDTO().template(TEMPLATE_NAME).language(LanguageCode.DE).dialect(Dialect.INFORMAL),
          TEMPLATE_DATA_WITH_MISSING_FIELD);
      fail("Expected exception: ServiceException");
    } catch (TemplateServiceException serviceException) {
      assertTrue("Excepted ServiceException thrown", true);
    }
  }
}
