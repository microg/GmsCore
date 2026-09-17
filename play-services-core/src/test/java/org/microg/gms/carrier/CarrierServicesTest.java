package org.microg.gms.carrier;

import static org.junit.Assert.*;
import android.content.Intent;
import org.junit.Test;

public class CarrierServicesTest {
    @Test
    public void testCarrierServicesIntentBinding() {
        Intent intent = new Intent(CarrierServicesShimService.ACTION_CARRIER_SERVICES);
        CarrierServicesShimService service = new CarrierServicesShimService();
        assertNotNull("CarrierServices binding must not be null for RCS negotiation", service.onBind(intent));
    }
}
