/*
 * Copyright 2013 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ow2.chameleon.bluetooth.discovery;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.bluetooth.devices.Device;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BluetoothServiceDiscoveryTest {

    @Test
    public void testBluetoothServiceDiscovery() throws InterruptedException {
        if (! LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContext context = EasyMock.createMock(BundleContext.class);
        BluetoothServiceDiscovery bsd = new BluetoothServiceDiscovery(context);

        RemoteDevice unamed = new RemoteDeviceStub("000000000001", null);
        RemoteDevice named = new RemoteDeviceStub("000000000002", "test");

        bsd.bindRemoteDevice(unamed);
        bsd.bindRemoteDevice(named);

        bsd.unbindRemoteDevice(unamed);

        Thread.sleep(5000);

        bsd.stop();
    }

    @Test
    public void testDiscoveredServices() throws InterruptedException {
        if (! LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothServiceDiscovery bsd = new BluetoothServiceDiscovery(context);

        RemoteDevice device = new RemoteDeviceStub("000000000001", "test");
        ServiceRecordStub srs1 = new ServiceRecordStub(device, "test");

        bsd.discovered(device, Arrays.asList(new ServiceRecord[]{srs1}));

        Assert.assertEquals(1, context.getServices().size());

        bsd.stop();
    }

    @Test
    public void testRetry() throws Exception {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothServiceDiscovery osd = new BluetoothServiceDiscovery(context);
        osd.setDeviceFile(new File("src/test/resources/devices.xml"));
        RemoteDevice device = new RemoteDeviceStub("000000000001", "TDU_1111111");

        osd.discovered(device, Arrays.asList(new ServiceRecord[]{}));

        Thread.sleep(60000);

        Assert.assertEquals(0, context.getServices().size());

        osd.stop();
    }


    @Test
    public void testRegexForAuthentication() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothServiceDiscovery osd = new BluetoothServiceDiscovery(context);
        osd.setDeviceFile(new File("src/test/resources/devices.xml"));

        RemoteDevice device = new RemoteDeviceStub("000000000003", "TDU_00000000");
        Device dev = osd.findDeviceFromFleet(device);
        Assert.assertNotNull(dev);

        device = new RemoteDeviceStub("1000E8C18C85", null);
        dev = osd.findDeviceFromFleet(device);
        Assert.assertNotNull(dev);

        device = new RemoteDeviceStub("000000000003", "xxx");
        dev = osd.findDeviceFromFleet(device);
        Assert.assertNull(dev);
    }

}
