SUBDIRS=configserv queue-scheduler address-controller

all: $(SUBDIRS)

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

clean: $(SUBDIRS) 
	$(MAKE) -C $@ clean

$(SUBDIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)

.PHONY: all $(SUBDIRS)
