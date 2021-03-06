package enmasse.controller.api.osb.v2.lastoperation;

import enmasse.controller.api.osb.v2.OSBExceptions;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/service_instances/{instanceId}/last_operation")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBLastOperationService extends OSBServiceBase {

    public OSBLastOperationService(InstanceApi instanceApi, FlavorRepository flavorRepository) {
        super(instanceApi, flavorRepository);
    }

    @GET
    public Response getLastOperationStatus(@PathParam("instanceId") String instanceId,
                                           @QueryParam("service_id") String serviceId,
                                           @QueryParam("plan_id") String planId,
                                           @QueryParam("operation") String operation) throws Exception {

        log.info("Received last_operation request for instance {}, operation {}, service id {}, plan id {}",
                instanceId, operation, serviceId, planId);

        Instance instance = findInstanceByDestinationUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Destination destination = findDestination(instance, instanceId)  // TODO: replace this and findInstanceByDestinationUuid so it returns both objects
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));


        LastOperationResponse response;
        if (isAddressReady(instance, destination)) {
            response = new LastOperationResponse(LastOperationState.SUCCEEDED, "All required pods are ready.");
        } else {
            response = new LastOperationResponse(LastOperationState.IN_PROGRESS, "Waiting for pods to be ready");
        }
        // TODO LastOperationState.FAILED ?

        return Response.ok(response).build();
    }

}
