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

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Unbind;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.bluetooth.devices.Device;
import org.ow2.chameleon.bluetooth.devices.DeviceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.DataElement;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Component publishing a {@link ServiceRecord} for all bluetooth services. This component consumes {@link RemoteDevice}
 * services and gets all the bluetooth services of the device. For each bluetooth service, it publishs a
 * {@link ServiceRecord}.
 */
@Component(public_factory = false, immediate = true)
@Instantiate(name = "BluetoothServiceDiscovery")
public class BluetoothServiceDiscovery {

    static final int[] ATTRIBUTES = null;

    private static final int SERVICE_NAME_ATTRIBUTE = 0x0100;

    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Logger.
     */
    private Logger m_logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Map storing the currently registered ServiceRecord(with their ServiceRegistration) by RemoteDevice
     */
    private Map<RemoteDevice, Map<ServiceRecord, ServiceRegistration>> m_servicesRecord = new HashMap<RemoteDevice, Map<ServiceRecord, ServiceRegistration>>();

    /**
     * Set of devices loaded from the <tt>devices.xml</tt> file.
     * This file contains the authentication information for the device.
     */
    private DeviceList m_fleet = null;

    /**
     * List of device under attempts.
     */
    private Map<RemoteDevice, Integer> m_attempts = new HashMap<RemoteDevice, Integer>();

    /**
     * Creates a {@link BluetoothServiceDiscovery}.
     *
     * @param context the context
     */
    public BluetoothServiceDiscovery(BundleContext context) {
        m_context = context;
        m_logger.info("Bluetooth Tracker Started");
    }

    @Property(name = "bluetooth.devices", value = "devices.xml")
    public void setDeviceFile(File file) throws IOException {
        if (!file.exists()) {
            m_fleet = null;
            m_logger.warn("No devices.xml file found, ignoring authentication");
        } else {
            try {
                FileInputStream fis = new FileInputStream(file);
                m_fleet = ConfigurationUtils.unmarshal(DeviceList.class, fis);
                m_logger.info(m_fleet.getDevices().size() + " devices loaded from devices.xml");
                fis.close();
            } catch (JAXBException e) {
                m_logger.error("Cannot unmarshall devices from " + file.getAbsolutePath(), e);
            } catch (IOException e) {
                m_logger.error("Cannot read devices from " + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Stops the discovery. All published services are withdrawn.
     */
    @Invalidate
    public synchronized void stop() {
        unregisterAll();
        m_attempts.clear();
    }

    private synchronized void unregisterAll() {
        for (RemoteDevice remoteDevice : m_servicesRecord.keySet()) {
            unregister(remoteDevice);
        }
    }

    private synchronized void unregister(RemoteDevice remote) {
        Map<ServiceRecord, ServiceRegistration> services = m_servicesRecord.remove(remote);
        if (services == null) {
            return;
        }
        for (ServiceRegistration sr : services.values()) {
            sr.unregister();
        }
    }

    private synchronized void register(RemoteDevice remote, ServiceRecord serviceRecord, Device device, String url) {
        if (!m_servicesRecord.containsKey(remote)) {
            m_servicesRecord.put(remote, new HashMap<ServiceRecord, ServiceRegistration>());
        }
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("device.id", remote.getBluetoothAddress());
        int[] attributeIDs = serviceRecord.getAttributeIDs();
        if (attributeIDs != null && attributeIDs.length > 0) {
            Map<Integer, DataElement> attrs = new HashMap<Integer, DataElement>();
            for (int attrID : attributeIDs) {
                attrs.put(attrID, serviceRecord.getAttributeValue(attrID));
            }
            props.put("service.attributes", attrs);
        }
        props.put("service.url", url);
        if (device != null) {
            props.put("fleet.device", device);
        }
        ServiceRegistration sr = m_context.registerService(ServiceRecord.class.getName(), serviceRecord, props);
        m_servicesRecord.get(remote).put(serviceRecord, sr);
    }

    /**
     * A new {@link RemoteDevice} is available. Checks if it implements OBEX, if
     * so publish the service
     *
     * @param device the device
     */
    @Bind(aggregate = true, optional = true)
    public synchronized void bindRemoteDevice(RemoteDevice device) {
        try {
            // We can't run searches concurrently.
            ServiceDiscoveryAgent agent = new ServiceDiscoveryAgent(this, device);
            BluetoothThreadManager.submit(agent);
        } catch (Exception e) {
            m_logger.error(
                    "Cannot discover services from "
                            + device.getBluetoothAddress(), e);
        }
    }

    /**
     * A {@link RemoteDevice} disappears. If an OBEX service was published for
     * this device, the service is unpublished.
     *
     * @param device the device
     */
    @Unbind
    public synchronized void unbindRemoteDevice(RemoteDevice device) {
        unregister(device);
    }

    /**
     * Callback receiving the set of discovered service from the given RemoteDevice.
     *
     * @param remote the RemoteDevice
     * @param discoveredServices the list of ServiceRecord
     */
    public void discovered(RemoteDevice remote,
                           List<ServiceRecord> discoveredServices) {
        if (discoveredServices == null || discoveredServices.isEmpty()) {
            unregister(remote);

            if (retry(remote)) {
                m_logger.info("Retrying service discovery for device " + remote.getBluetoothAddress() + " - " + m_attempts.get(remote));
                incrementAttempt(remote);
                ServiceDiscoveryAgent agent = new ServiceDiscoveryAgent(this, remote);
                BluetoothThreadManager.submit(agent);
            } else {
                // We don't retry, either retry is false or we reached the number of attempts.
                m_attempts.remove(remote);
            }
            return;
        }
        m_logger.info("Agent has discovered " + discoveredServices.size()
                + " services from " + remote.getBluetoothAddress() + ".");

        // Service discovery successful, we reset the number of attempts.
        m_attempts.remove(remote);
        Device device = findDeviceFromFleet(remote);

        for (ServiceRecord record : discoveredServices) {
            String url;
            if (device == null) {
                url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            } else {
                url = record.getConnectionURL(ServiceRecord.AUTHENTICATE_NOENCRYPT, false);
            }

            if (url == null) {
                m_logger.warn("Can't compute the service url for device " + remote.getBluetoothAddress() + " - Ignoring service record");
            } else {
                DataElement serviceName = record.getAttributeValue(BluetoothServiceDiscovery.SERVICE_NAME_ATTRIBUTE);
                if (serviceName != null) {
                    m_logger.info("Service " + serviceName.getValue() + " found " + url);
                } else {
                    m_logger.info("Service found " + url);
                }
                register(remote, record, device, url);
            }
        }

    }

    private void incrementAttempt(RemoteDevice remote) {
        Integer attempt = m_attempts.get(remote);
        if (attempt == null) {
            attempt = 1;
            m_attempts.put(remote, attempt);
        } else {
            m_attempts.put(remote, ++attempt);
        }
    }

    private boolean retry(RemoteDevice remote) {
        Device device = findDeviceFromFleet(remote);
        if (device == null) {
            return true;
        }

        Integer numberOfTries = m_attempts.get(remote);
        if (numberOfTries == null) {
            numberOfTries = 0;
        }

        BigInteger mr = device.getMaxRetry();
        int max = 1;
        if (mr != null && mr.intValue() != 0) {
            max = mr.intValue();
        }

        return device.isRetry() && max >= numberOfTries;
    }

    Device findDeviceFromFleet(RemoteDevice remote) {
        if (m_fleet != null) {
            String address = remote.getBluetoothAddress();
            String sn = null;
            try {
                sn = remote.getFriendlyName(false);
            } catch (IOException e) {
                // ignore the exception
            }

            for (int i = 0; i < m_fleet.getDevices().size(); i++) {
                Device d = m_fleet.getDevices().get(i);
                String regex = d.getId(); // id can be regex.
                if (Pattern.matches(regex, address) || (sn != null && Pattern.matches(regex, sn))) {
                    return d;
                }
            }
        }
        return null;
    }

}
