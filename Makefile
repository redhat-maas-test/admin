SUBDIRS=configserv queue-scheduler address-controller

buildall:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir build; \
	done

pushall:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir push; \
	done

snapshotall:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir snapshot; \
	done

cleanall:
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir clean; \
	done

.PHONY: build push snapshot clean
