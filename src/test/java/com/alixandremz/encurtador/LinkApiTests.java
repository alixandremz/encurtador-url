package com.alixandremz.encurtador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.alixandremz.encurtador.model.Link;
import com.alixandremz.encurtador.repository.LinkRepository;

@SpringBootTest
@AutoConfigureMockMvc
class LinkApiTests {

    private static final Pattern SLUG = Pattern.compile("\"slug\"\\s*:\\s*\"([^\"]+)\"");
    private static final String URL = "https://exemplo.com/pagina";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LinkRepository repositorio;

    @BeforeEach
    void limpar() {
        repositorio.deleteAll();
    }

    private String criarLink() throws Exception {
        String resposta = mvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlOriginal\":\"" + URL + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = SLUG.matcher(resposta);
        matcher.find();
        return matcher.group(1);
    }

    @Test
    void criaLinkComStatus201EUrlCurta() throws Exception {
        mvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlOriginal\":\"" + URL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.slug").isString())
                .andExpect(jsonPath("$.urlOriginal").value(URL))
                .andExpect(jsonPath("$.urlCurta").value(org.hamcrest.Matchers.startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.cliques").value(0));
    }

    @Test
    void redirecionaParaUrlOriginal() throws Exception {
        String slug = criarLink();
        mvc.perform(get("/{slug}", slug))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", URL));
    }

    @Test
    void contaCliquesPorRedirecionamento() throws Exception {
        String slug = criarLink();
        mvc.perform(get("/{slug}", slug)).andExpect(status().isFound());
        mvc.perform(get("/{slug}", slug)).andExpect(status().isFound());
        mvc.perform(get("/links/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliques").value(2));
    }

    @Test
    void slugInexistenteRetorna404() throws Exception {
        mvc.perform(get("/{slug}", "naoexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void urlInvalidaRetorna400() throws Exception {
        mvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlOriginal\":\"ftp://arquivo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.urlOriginal").exists());
    }

    @Test
    void urlEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlOriginal\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.urlOriginal").exists());
    }

    @Test
    void expiracaoNoPassadoRetorna400() throws Exception {
        mvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlOriginal\":\"" + URL + "\",\"expiraEm\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.expiraEm").exists());
    }

    @Test
    void linkExpiradoRetorna410() throws Exception {
        Link link = new Link();
        link.setSlug("expirado");
        link.setUrlOriginal(URL);
        link.setExpiraEm(Instant.now().minusSeconds(60));
        repositorio.save(link);

        mvc.perform(get("/{slug}", "expirado"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.erro").exists());
    }
}
