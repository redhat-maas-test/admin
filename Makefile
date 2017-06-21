SUBDIRS=configserv queue-scheduler address-controller

build:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir build; \
	done

push:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir push; \
	done

snapshot:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir snapshot; \
	done

clean:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir clean; \
	done

.PHONY: build push snapshot clean
