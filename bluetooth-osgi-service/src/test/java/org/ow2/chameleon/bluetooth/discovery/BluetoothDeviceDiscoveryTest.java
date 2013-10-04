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

import org.junit.Assert;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class BluetoothDeviceDiscoveryTest {

    @Test
    public void testBluetoothDeviceDiscovery() throws InterruptedException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContext context = EasyMock.createMock(BundleContext.class);
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.start();

        Thread.sleep(10000);

        bdd.stop();
    }

    @Test
    public void testDiscoveredDevices() throws InterruptedException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.m_ignoreUnnamedDevices = true;
        bdd.start();

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        RemoteDevice unamed = new RemoteDeviceStub("000000000001", null);
        discovered.add(unamed);
        RemoteDevice named = new RemoteDeviceStub("000000000002", "test");
        discovered.add(named);

        bdd.discovered(discovered);

        Assert.assertEquals(1, context.getServices().size());

        bdd.stop();
    }

    @Test
    public void testDiscoveredDevicesSupportingUnamed() throws InterruptedException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.m_ignoreUnnamedDevices = false;
        bdd.start();

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        RemoteDevice unamed = new RemoteDeviceStub("000000000001", null);
        discovered.add(unamed);
        RemoteDevice named = new RemoteDeviceStub("000000000002", "test");
        discovered.add(named);

        bdd.discovered(discovered);

        Assert.assertEquals(2, context.getServices().size());

        bdd.stop();
    }

    @Test
    public void testRegexForPairing() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.m_ignoreUnnamedDevices = false;
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices.xml"));
        bdd.start();

        RemoteDevice device = new RemoteDeviceStub("000000000002", "test");
        Assert.assertFalse(bdd.pair(device));

        device = new RemoteDeviceStub("000000000003", "TDU_00000000");
        Assert.assertTrue(bdd.pair(device));
    }

    @Test
    public void testDeviceFilter() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.m_ignoreUnnamedDevices = false;
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices-with-filter.xml"));
        bdd.start();

        RemoteDevice device1 = new RemoteDeviceStub("000000000002", "test");
        Assert.assertFalse(bdd.matchesDeviceFilter(device1));


        RemoteDevice device2 = new RemoteDeviceStub("000000000003", "TDU_00000000");
        Assert.assertTrue(bdd.matchesDeviceFilter(device2));

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        discovered.add(device1);
        discovered.add(device2);

        bdd.discovered(discovered);

        Assert.assertEquals(1, context.getServices().size());
    }

    @Test
    public void testDeviceName() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.setDeviceNameFile("target/test-classes/names.properties");
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices-with-filter.xml"));
        bdd.start();

        // Case to tests:
        // 1) unknown device with a name
        // 2) unknown device without a name
        // 3) known device with a name
        // 4) known device without a name
        // 5) known device with a different name

        RemoteDevice device1 = new RemoteDeviceStub("000012345678", "TDU_00000006");
        RemoteDevice device2 = new RemoteDeviceStub("000012345679", null);
        RemoteDevice device3 = new RemoteDeviceStub("000000000000", "TDU_00000000");
        RemoteDevice device4 = new RemoteDeviceStub("000000000001", null);
        RemoteDevice device5 = new RemoteDeviceStub("000000000002", "TDU_00000004"); // 00000002 expected

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        discovered.addAll(Arrays.asList(device1, device2, device3, device4, device5));

        bdd.discovered(discovered);

        // Expected
        // device 1 exposed
        // device 2 rejected
        // device 3 exposed
        // device 4 exposed, name set to the memory one
        // device 5 exposed using memory name
        // properties file updated with device 1

        Map<Object, Dictionary> services = context.getServices();
        Assert.assertEquals(4, services.size());
        // Check devices 1
        Assert.assertNotNull(services.get(device1));
        Assert.assertEquals("TDU_00000006", services.get(device1).get("device.name"));

        // Check devices 2
        Assert.assertNull(services.get(device2));

        // Check devices 3
        Assert.assertNotNull(services.get(device3));
        Assert.assertEquals("TDU_00000000", services.get(device3).get("device.name"));

        // Check devices 4
        Assert.assertNotNull(services.get(device4));
        Assert.assertEquals("TDU_00000001", services.get(device4).get("device.name"));

        // Check devices 5
        Assert.assertNotNull(services.get(device5));
        Assert.assertEquals("TDU_00000002", services.get(device5).get("device.name"));

        bdd.stop(); // write the file.

        // Load properties
        Properties properties = new Properties();
        File p = new File("target/test-classes/names.properties");
        FileInputStream fis = new FileInputStream(p);
        properties.load(fis);
        fis.close();

        Assert.assertTrue(properties.containsKey("000012345678"));
        Assert.assertEquals("TDU_00000006", properties.getProperty("000012345678"));

    }

    @Test
    public void testPersistentSupportDisabledWithNull() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.setDeviceNameFile(null); // Disabled.
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices-with-filter.xml"));
        bdd.start();


        RemoteDevice device1 = new RemoteDeviceStub("000012345677", "TDU_00000006");
        RemoteDevice device2 = new RemoteDeviceStub("000012345679", null);

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        discovered.addAll(Arrays.asList(device1, device2));
        bdd.discovered(discovered);


        Map<Object, Dictionary> services = context.getServices();
        // Check devices 1
        Assert.assertNotNull(services.get(device1));
        Assert.assertEquals("TDU_00000006", services.get(device1).get("device.name"));

        // Check devices 2
        Assert.assertNull(services.get(device2));

        bdd.stop(); // write the file.

        // Load properties
        Properties properties = new Properties();
        File p = new File("target/test-classes/names.properties");
        FileInputStream fis = new FileInputStream(p);
        properties.load(fis);
        fis.close();

        Assert.assertFalse(properties.containsKey("000012345677"));

    }

    @Test
    public void testPersistentSupportDisabledWithNullString() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.setDeviceNameFile("null"); // Disabled.
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices-with-filter.xml"));
        bdd.start();


        RemoteDevice device1 = new RemoteDeviceStub("000012345677", "TDU_00000006");
        RemoteDevice device2 = new RemoteDeviceStub("000012345679", null);

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        discovered.addAll(Arrays.asList(device1, device2));
        bdd.discovered(discovered);


        Map<Object, Dictionary> services = context.getServices();
        // Check devices 1
        Assert.assertNotNull(services.get(device1));
        Assert.assertEquals("TDU_00000006", services.get(device1).get("device.name"));

        // Check devices 2
        Assert.assertNull(services.get(device2));

        bdd.stop(); // write the file.

        // Load properties
        Properties properties = new Properties();
        File p = new File("target/test-classes/names.properties");
        FileInputStream fis = new FileInputStream(p);
        properties.load(fis);
        fis.close();

        Assert.assertFalse(properties.containsKey("000012345677"));

    }


    @Test
    public void testPersistentSupportDisabledWithEmptyString() throws IOException {
        if (!LocalDevice.isPowerOn()) {
            System.err.println("Bluetooth Adapter required");
            return;
        }

        BundleContextStub context = new BundleContextStub();
        BluetoothDeviceDiscovery bdd = new BluetoothDeviceDiscovery(context);
        bdd.setDeviceNameFile(""); // Disabled.
        bdd.setAutopairingConfiguration(new File("src/test/resources/devices-with-filter.xml"));
        bdd.start();


        RemoteDevice device1 = new RemoteDeviceStub("000012345677", "TDU_00000006");
        RemoteDevice device2 = new RemoteDeviceStub("000012345679", null);

        Set<RemoteDevice> discovered = new HashSet<RemoteDevice>();
        discovered.addAll(Arrays.asList(device1, device2));
        bdd.discovered(discovered);


        Map<Object, Dictionary> services = context.getServices();
        // Check devices 1
        Assert.assertNotNull(services.get(device1));
        Assert.assertEquals("TDU_00000006", services.get(device1).get("device.name"));

        // Check devices 2
        Assert.assertNull(services.get(device2));

        bdd.stop(); // write the file.

        // Load properties
        Properties properties = new Properties();
        File p = new File("target/test-classes/names.properties");
        FileInputStream fis = new FileInputStream(p);
        properties.load(fis);
        fis.close();

        Assert.assertFalse(properties.containsKey("000012345677"));

    }

}
