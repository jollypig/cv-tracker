package com.example.cv.person;

import com.example.cv.auth.AuthenticatedUserService;
import com.example.cv.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    private final PersonService personService;
    private final AuthenticatedUserService authenticatedUsers;

    public PersonController(PersonService personService, AuthenticatedUserService authenticatedUsers) {
        this.personService = personService;
        this.authenticatedUsers = authenticatedUsers;
    }

    @GetMapping
    public List<PersonResponse> findAll(@AuthenticationPrincipal OidcUser principal) {
        return personService.findAll(authenticatedUsers.synchronize(principal).getId());
    }

    @GetMapping("/{id}")
    public PersonResponse findById(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal) {
        return personService.findById(id, authenticatedUsers.synchronize(principal).getId());
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(
            @Valid @RequestBody PersonRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        AuthenticatedUser owner = authenticatedUsers.synchronize(principal);
        PersonResponse person = personService.create(request, owner);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(person.id()).toUri();
        return ResponseEntity.created(location).body(person);
    }

    @PutMapping("/{id}")
    public PersonResponse update(@PathVariable UUID id, @Valid @RequestBody PersonRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return personService.update(id, request, authenticatedUsers.synchronize(principal).getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal) {
        personService.delete(id, authenticatedUsers.synchronize(principal).getId());
        return ResponseEntity.noContent().build();
    }
}