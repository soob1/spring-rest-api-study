package com.soob1.rest.events;

import com.soob1.rest.common.ErrorsResource;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_JSON_VALUE)
public class EventController {

	private final EventRepository eventRepository;
	private final ModelMapper modelMapper;
	private final EventValidator eventValidator;

	@PostMapping
	public ResponseEntity createEvent(@RequestBody @Valid EventDto eventDto, Errors errors) {
		if(errors.hasErrors()) {
			return badRequest(errors);
		}

		eventValidator.validate(eventDto, errors);
		if(errors.hasErrors()) {
			return badRequest(errors);
		}

		Event event = modelMapper.map(eventDto, Event.class);
		event.update();
		Event savedEvent = eventRepository.save(event);
		URI createdUri = linkTo(EventController.class).slash(savedEvent.getId()).toUri();

		EventResource eventResource = new EventResource(savedEvent);
		eventResource.add(linkTo(EventController.class).withRel("query-events"));
		eventResource.add(linkTo(EventController.class).slash(event.getId()).withRel("update-event"));
		eventResource.add(Link.of("/docs/index.html#resources-events-create").withRel("profile"));

		return ResponseEntity.created(createdUri).body(eventResource);
	}

	@GetMapping
	public ResponseEntity queryEvents(Pageable pageable, PagedResourcesAssembler<Event> assembler) {
		Page<Event> events = this.eventRepository.findAll(pageable);
		// TODO 각 이벤트에 링크 정보 추가
		var pagedModel = assembler.toModel(events);
		return ResponseEntity.ok(pagedModel);
	}

	@GetMapping("/{id}")
	public ResponseEntity getEvent(@PathVariable Integer id) {
		Optional<Event> optionalEvent = eventRepository.findById(id);

		if (optionalEvent.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Event event = optionalEvent.get();
		EventResource eventResource = new EventResource(event);
		eventResource.add(Link.of("/docs/index.html#resources-events-get").withRel("profile"));
		return ResponseEntity.ok(eventResource);
	}

	private ResponseEntity badRequest(Errors errors) {
		ErrorsResource errorsResource = new ErrorsResource(errors);
		return ResponseEntity.badRequest().body(errorsResource);
	}

}
