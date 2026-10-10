package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// CORRECCIÓN: Se removió el import 'java.awt.*' innecesario en un API REST de Spring Boot.

/**
 * Controlador REST para la gestión de personas.
 */
@RestController
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @PostMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crearPersona(@Valid @RequestBody PersonasRequest personasRequest) {
        // CORRECCIÓN: Nombre de método renombrado a crearPersona para mayor claridad y coherencia.
        return new ResponseEntity<>(personaService.creaPersona(personasRequest), HttpStatus.OK);
    }

    @PutMapping(value = "/personasActualiza", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizaPersona(@Valid @RequestBody PersonasRequest personasRequest) {
        // CORRECCIÓN: Nombre de método corregido a actualizaPersona.
        return new ResponseEntity<>(personaService.actualizaPersona(personasRequest), HttpStatus.OK);
    }

    // CORRECCIÓN: Se cambió el verbo de @PutMapping a @DeleteMapping y se renombró el método para evitar duplicidad de nombre.
    @DeleteMapping(value = "/personasElimina", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminaPersona(@Valid @RequestBody EliminaPersonaRequest personasRequest) {
        return new ResponseEntity<>(personaService.eliminaPersona(personasRequest), HttpStatus.OK);
    }
}

