package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.Future;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;

public class ReleaseVehicleEvent implements Event {
    private Future<DeliveryVehicle> vehicle;
    public ReleaseVehicleEvent(Future<DeliveryVehicle> vehicle) {
        this.vehicle = vehicle;
    }

    public Future<DeliveryVehicle> getFutureVehicle() {
        return vehicle;
    }
}
