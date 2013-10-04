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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Discovery Agent searching services for one specific device.
 * If a matching service is found, we publishes an ??
 */
class ServiceDiscoveryAgent implements DiscoveryListener, Runnable {

    static UUID[] searchUuidSet = {UUIDs.PUBLIC_BROWSE_GROUP};

    static int[] attrIDs = BluetoothServiceDiscovery.ATTRIBUTES;

    /**
     *
     */
    private final BluetoothServiceDiscovery m_parent;

    private final RemoteDevice m_device;

    private String m_name;

    private boolean m_searchInProgress = false;

    private Logger m_logger = LoggerFactory.getLogger(ServiceDiscoveryAgent.class);

    private List<ServiceRecord> m_discoveredServices = new ArrayList<ServiceRecord>();

    public ServiceDiscoveryAgent(BluetoothServiceDiscovery bluetoothServiceDiscovery, RemoteDevice device) {
        m_parent = bluetoothServiceDiscovery;
        m_device = device;
        try {
            m_name = m_device.getFriendlyName(false);
        } catch (IOException e) {
            m_name = m_device.getBluetoothAddress();
        }
    }

    private LocalDevice initialize() {
        LocalDevice local = null;
        try {
            local = LocalDevice.getLocalDevice();
            m_logger.info("Address: " + local.getBluetoothAddress());
            m_logger.info("Name: " + local.getFriendlyName());
        } catch (BluetoothStateException e) {
            m_logger.error("Bluetooth Adapter not started.");
        }

        return local;

    }

    public void run() {
        try {
            m_logger.info("Search services on " + m_device.getBluetoothAddress()
                    + " " + m_name);

            LocalDevice local = initialize();
            if (!LocalDevice.isPowerOn() || local == null) {
                m_logger.error("Bluetooth adapter not ready, aborting service discovery");
                m_parent.discovered(m_device, null);
                return;
            }

            doSearch(local);
        } catch (Throwable e) {
            m_logger.error("Unexpected exception during service inquiry", e);
        }
    }

    void doSearch(LocalDevice local) {
        synchronized (this) {
            m_searchInProgress = true;
            try {

                if (Env.isTestEnvironmentEnabled()) {
                    m_logger.warn("=== TEST ENVIRONMENT ENABLED ===");
                } else {
                    local.getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, m_device, this);
                }

                wait();
            } catch (InterruptedException e) {
                if (m_searchInProgress) {
                    // we're stopping, aborting discovery.
                    m_searchInProgress = false;
                    m_logger.warn("Interrupting bluetooth service discovery - interruption");
                } else {
                    // Search done !
                    m_logger.info("Bluetooth discovery for " + m_name + " completed !");
                }
            } catch (BluetoothStateException e) {
                // well ... bad choice. Bluetooth driver not ready
                // Just abort.
                m_logger.error("Cannot search for bluetooth services", e);
                m_parent.discovered(m_device, null);
                return;
            }
            m_logger.info("Bluetooth discovery for " + m_name + " is now completed - injecting "
                    + m_discoveredServices.size() + " discovered services ");
            m_parent.discovered(m_device, m_discoveredServices);
        }
    }

    /*
     *
     ********** DiscoveryListener **********
     *
     */
    @Override
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        // Not used here.
    }

    @Override
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        synchronized (this) {
            if (!m_searchInProgress) {
                // We were stopped.
                notifyAll();
                return;
            }
        }

        m_logger.info("Matching service found - " + servRecord.length);
        m_discoveredServices.addAll(Arrays.asList(servRecord));
    }

    @Override
    public void serviceSearchCompleted(int transID, int respCode) {
        synchronized (this) {
            m_logger.info("Service search completed for device " + m_device.getBluetoothAddress());
            m_searchInProgress = false;
            notifyAll();
        }
    }

    @Override
    public void inquiryCompleted(int discType) {
        // Not used here.
    }

}