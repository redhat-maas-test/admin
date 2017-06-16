SUBDIRS=configserv queue-scheduler address-controller/server

all: $(SUBDIRS)

clean: $(SUBDIRS) 
	$(MAKE) -C $@ clean

$(SUBDIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)

.PHONY: all $(SUBDIRS)
