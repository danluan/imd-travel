package br.ufrn.imd.imdtravel.obs;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.ufrn.imd.imdtravel.controller.HealthController;
import br.ufrn.imd.imdtravel.service.HealthService;

@WebMvcTest(HealthController.class)
@Import({CorrelationIdFilter.class, JsonLogger.class})
class HealthControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean HealthService healthService;

    @Test
    void saudavelDevolve200ComStatusEUptimeNumerico() throws Exception {
        when(healthService.check()).thenReturn(new HealthService.Result(true, null));

        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.uptimeS").isNumber());
    }

    @Test
    void comFalhaDevolve503ComMotivo() throws Exception {
        when(healthService.check()).thenReturn(new HealthService.Result(false, "banco: caiu"));

        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("down"))
                .andExpect(jsonPath("$.reason").value("banco: caiu"));
    }
}
