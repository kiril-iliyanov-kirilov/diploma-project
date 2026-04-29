package com.diploma.bookapi.service;

import com.diploma.bookapi.model.Book;
import com.diploma.bookapi.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository repo;

    public BookService(BookRepository repo) {
        this.repo = repo;
    }

    @Cacheable(value = "books")
    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return repo.findAll();
    }

    @Cacheable(value = "book", key = "#id")
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));
    }

    @CacheEvict(value = "books", allEntries = true)
    public Book create(Book book) {
        return repo.save(book);
    }

    @Caching(evict = {
            @CacheEvict(value = "book", key = "#id"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public Book update(Long id, Book updated) {
        Book existing = findByIdNoCache(id);
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setIsbn(updated.getIsbn());
        existing.setDescription(updated.getDescription());
        return repo.save(existing);
    }

    @Caching(evict = {
            @CacheEvict(value = "book", key = "#id"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Book not found with id: " + id);
        }
        repo.deleteById(id);
    }

    // Internal helper that bypasses cache to avoid re-caching stale data during update
    private Book findByIdNoCache(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));
    }
}
