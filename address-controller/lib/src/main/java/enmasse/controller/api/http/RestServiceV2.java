/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller.api.http;

import enmasse.controller.address.AddressManagerFactory;
import enmasse.controller.api.v1v2common.RestServiceBase;
import enmasse.controller.api.v1v2common.common.AddressProperties;
import enmasse.controller.model.Destination;
import enmasse.controller.model.DestinationGroup;
import enmasse.controller.model.InstanceId;
import io.vertx.core.Vertx;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The V2 interface is not yet public, so it is temporary in order to have a way to deploy colocated addresses
 */
@Path("/v2/enmasse/addresses")
public class RestServiceV2 extends RestServiceBase {

    public RestServiceV2(@Context InstanceId instanceId, @Context AddressManagerFactory addressManagerFactory, @Context Vertx vertx) {
        super(instanceId, addressManagerFactory, vertx);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public void getAddresses(@Suspended final AsyncResponse response) {
        super.getAddresses(response);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void putAddresses(Map<String, Map<String, AddressProperties>> addressMap, @Suspended final AsyncResponse response) {
        super.putAddresses(mapToDestinations(addressMap), response);
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void deleteAddresses(List<String> data, @Suspended final AsyncResponse response) {
        super.deleteAddresses(data, response);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void appendAddresses(Map<String, Map<String, AddressProperties>> addressMap, @Suspended final AsyncResponse response) {
        super.appendAddresses(mapToDestinations(addressMap), response);
    }

    private static Set<DestinationGroup> mapToDestinations(Map<String, Map<String, AddressProperties>> addressMap) {
        return addressMap.entrySet().stream()
                .map(e -> {
                    DestinationGroup.Builder groupBuilder = new DestinationGroup.Builder(e.getKey());
                    e.getValue().entrySet()
                            .forEach(d -> {
                                groupBuilder.destination(new Destination(d.getKey(), e.getKey(), d.getValue().store_and_forward, d.getValue().multicast, Optional.ofNullable(d.getValue().flavor), Optional.empty()));
                            });
                    return groupBuilder.build();
                })
                .collect(Collectors.toSet());
    }

    protected Map<String, Map<String, AddressProperties>> getResponseEntity(Collection<DestinationGroup> destinationGroups) {
        Map<String, Map<String, AddressProperties>> map = new LinkedHashMap<>();
        for (DestinationGroup destinationGroup : destinationGroups) {
            Map<String, AddressProperties> addrMap = new LinkedHashMap<>();
            map.put(destinationGroup.getGroupId(), addrMap);
            for (Destination destination : destinationGroup.getDestinations()) {
                String flavor = destination.flavor().orElse(null);
                addrMap.put(destination.address(), new AddressProperties(destination.storeAndForward(), destination.multicast(), flavor));
            }
        }
        return map;
    }
}