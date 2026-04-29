package com.diploma.bookapi;

import com.diploma.bookapi.model.Book;
import com.diploma.bookapi.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookApiIntegrationTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            // In-memory cache manager with the same cache names used in production
            return new ConcurrentMapCacheManager("books", "book");
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void createAndRetrieveBook() throws Exception {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", "A handbook of agile software craftsmanship");

        String location = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateBook() throws Exception {
        Book saved = bookRepository.save(new Book("Old Title", "Author", null, null));

        Book updated = new Book("New Title", "Author", null, null);

        mockMvc.perform(put("/api/books/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void deleteBook() throws Exception {
        Book saved = bookRepository.save(new Book("To Delete", "Author", null, null));

        mockMvc.perform(delete("/api/books/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBookWithBlankTitleReturns400() throws Exception {
        Book invalid = new Book("", "Author", null, null);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
