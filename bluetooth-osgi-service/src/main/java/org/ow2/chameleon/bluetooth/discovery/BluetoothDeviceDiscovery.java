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

import com.intel.bluetooth.RemoteDeviceHelper;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.bluetooth.BluetoothController;
import org.ow2.chameleon.bluetooth.devices.Device;
import org.ow2.chameleon.bluetooth.devices.DeviceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Component Discovering bluetooth device periodically and publishing a {@link RemoteDevice}
 * service per found device.
 * Notice that bluetooth is an active discovery protocol which means that device departures and
 * arrivals are detected periodically. So interacting with a bluetooth device can throw {@link IOException}
 * at any time.
 * If Bluetooth is not available, the component just stops.
 * Inquiries cannot be ran concurrently.
 */
@Component(public_factory = false)
@Provides
@Instantiate(name = "BluetoothDeviceDiscovery")
public class BluetoothDeviceDiscovery implements BluetoothController {

    /**
     * Bluetooth discovery mode (inquiry).
     */
    public enum DiscoveryMode {
        /**
         * Global inquiry.
         */
        GIAC,
        /**
         * Limited inquiry.
         */
        LIAC
    }

    public static List<String> SUPPORTED_STACKS = Arrays.asList("winsock", "widcomm", "mac", "bluez");  // "bluez-dbus"

    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Map storing the currently exposed bluetooth device.
     */
    private Map<RemoteDevice, ServiceRegistration> m_devices = new HashMap<RemoteDevice, ServiceRegistration>();

    /**
     * Logger.
     */
    private Logger m_logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Set of devices loaded from the <tt>devices.xml</tt> file.
     * This file contains the authentication information for the device.
     */
    private DeviceList m_fleet = null;


    /**
     * Configurable Property specifying the time between two inquiries.
     * This time is specified in <b>second</b>, and should be carefully chosen.
     * Too many inquiries flood the network and block correct discovery. A too big period,
     * makes the device dynamism hard to track.
     */
    @Property(name = "bluetooth.discovery.period", value = "10") // In second !
    private int m_period;

    /**
     * Configurable property specifying the discovery mode among GIAC and LIAC.
     */
    @Property(name = "bluetooth.discovery.mode", value = "GIAC")
    private DiscoveryMode m_discoveryMode;

    /**
     * Configuration property enabling the support of unnamed devices. Unnamed devices do not communicate their name.
     */
    @Property(name = "bluetooth.ignore.unnamed.devices", value = "true")
    boolean m_ignoreUnnamedDevices;

    /**
     * This configuration property enables the online check when a device is found.
     * It turns around the Windows 7 behavior, where the device discovery returns all paired devices
     * even if they are not reachable anymore. However it introduces a performance cost ( a service discovery for
     * each cached device on every discovery search).
     * It should be used in combination with <tt>bluetooth.discovery.unpairOnDeparture</tt>.
     */
    @Property(name = "bluetooth.discovery.onlinecheck", value = "false")
    boolean m_onlineCheckOnDiscovery;

    /**
     * Configuration property enabling the unpairing of matching devices (filter given in the fleet description) when
     * they are not reachable anymore.
     */
    @Property(name = "bluetooth.discovery.unpairOnDeparture", value = "false")
    boolean m_unpairLostDevices;

    /**
     * The file storing the mac -> name association.
     * This file is updated every time a new device is discovered.
     * If set to <code>null</code> the list is not persisted.
     */
    private File m_deviceNameFile;

    /**
     * Map storing the MAc address to name association.
     * It avoids ignoring unnamed devices, as once we get a name, it is stored in this list.
     * This map can be persisted if the device name file is set.
     */
    private Properties m_names = new Properties();

    /**
     * The fleet device filter (regex configured in the devices.xml file).
     */
    Pattern m_filter;

    private DeviceDiscoveryAgent m_agent;


    /**
     * Creates a {@link BluetoothDeviceDiscovery}.
     *
     * @param context the bundle context
     */
    public BluetoothDeviceDiscovery(BundleContext context) {
        m_context = context;
    }

    @Property(name = "bluetooth.devices")
    public void setAutopairingConfiguration(File file) throws IOException {
        if (!file.exists()) {
            m_fleet = null;
            m_logger.warn("No devices.xml file found, ignoring auto-pairing and device filter");
        } else {
            try {
                FileInputStream fis = new FileInputStream(file);
                m_fleet = ConfigurationUtils.unmarshal(DeviceList.class, fis);
                String filter = m_fleet.getDeviceFilter();

                if (filter != null) {
                    m_filter = Pattern.compile(filter);
                }

                m_logger.info(m_fleet.getDevices().size() + " devices loaded from devices.xml");
                if (m_filter != null) {
                    m_logger.info("Device filter set to : " + m_filter.pattern());
                } else {
                    m_logger.info("No device filter set - Accepting all devices");
                }

                fis.close();
            } catch (JAXBException e) {
                m_logger.error("Cannot unmarshall devices from " + file.getAbsolutePath(), e);
            } catch (IOException e) {
                m_logger.error("Cannot read devices from " + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Sets the device name file.
     * If set to <code>null</code> or to <code>""</code> or to <code>"null"</code>, the persistent support is disabled.
     * Otherwise, the file is read to initialize the device list and written each time we find a new device.
     *
     * @param name the path to the file relative to the working directory.
     */
    @Property(name = "bluetooth.discovery.names", value = "btnames.properties")
    public void setDeviceNameFile(String name) {
        if (name == null || name.equals("null") || name.trim().length() == 0) {
            m_logger.warn("No device name file set, disabling persistent support");
            return;
        }
        m_deviceNameFile = new File(name);

        m_names = loadDeviceNames();
    }

    private Properties loadDeviceNames() {
        Properties properties = new Properties();

        if (m_deviceNameFile == null) {
            m_logger.error("No device name files, ignoring persistent support");
            return properties;
        }

        if (!m_deviceNameFile.exists()) {
            m_logger.error("The device name file does not exist, ignoring (" + m_deviceNameFile.getAbsolutePath() + ")");
            return properties;
        }

        try {
            FileInputStream fis = new FileInputStream(m_deviceNameFile);
            properties.load(fis);
            fis.close();
            m_logger.info("Device name file loaded, " + properties.size() + " devices read");
        } catch (IOException e) {
            m_logger.error("Cannot load the device name file (" + m_deviceNameFile.getAbsolutePath() + ")", e);
        }

        return properties;
    }


    private void storeDeviceNames(Properties properties) {
        if (m_deviceNameFile == null) {
            return;
        }

        if (!m_deviceNameFile.exists()) {
            final File parent = m_deviceNameFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(m_deviceNameFile);
            properties.store(fos, "Mac to Name file");
            fos.close();
        } catch (IOException e) {
            m_logger.error("Cannot store the 'names' in " + m_deviceNameFile.getAbsolutePath(), e);
        }
    }

    /**
     * Initializes the discovery.
     */
    @Validate
    public void start() {
        if (m_agent != null) {
            return;
        }

        m_names = loadDeviceNames();

        if (m_period == 0) {
            m_period = 10; // Default to 30 seconds.
        }

        if (!isBluetoothStackSupported()) {
            m_logger.error("The Bluetooth stack " + getBluetoothStack() + " is not supported (" + SUPPORTED_STACKS + ")");
            return;
        }

        if ("winsock".equals(getBluetoothStack())) {
            m_logger.info("Winsock stack detected, forcing online check and lost device unpairing");
            m_onlineCheckOnDiscovery = true;
            m_unpairLostDevices = true;
        }

        m_agent = new DeviceDiscoveryAgent(this, m_discoveryMode, m_onlineCheckOnDiscovery);
        BluetoothThreadManager.scheduleJob(m_agent, m_period);
    }

    /**
     * Stops the discovery.
     */
    @Invalidate
    public void stop() {
        if (m_agent == null) {
            return;
        }
        storeDeviceNames(m_names);
        m_agent = null;
        BluetoothThreadManager.stopScheduler();
        unregisterAll();
    }

    @Override
    public String getBluetoothStack() {
        return LocalDevice.getProperty("bluecove.stack");
    }

    @Override
    public boolean isBluetoothDeviceTurnedOn() {
        return LocalDevice.isPowerOn();
    }

    @Override
    public boolean isBluetoothStackSupported() {
        return SUPPORTED_STACKS.contains(getBluetoothStack());
    }

    /**
     * Callback receiving the new set of reachable devices.
     *
     * @param discovered the set of found RemoteDevice
     */
    public void discovered(Set<RemoteDevice> discovered) {
        if (discovered == null) {
            // Bluetooth error, we unregister all devices
            m_logger.warn("Bluetooth error detected, unregistering all devices");
            unregisterAll();
            return;
        }

        // Detect devices that have left
        // We must create a copy of the list to avoid concurrent modifications
        Set<RemoteDevice> presents = new HashSet<RemoteDevice>(m_devices.keySet());
        for (RemoteDevice old : presents) {
            m_logger.info("Did we lost contact with " + old.getBluetoothAddress() + " => " + (!contains(discovered, old)));
            if (!contains(discovered, old)) {
                ServiceCheckAgent serviceCheckAgent = new ServiceCheckAgent(old, SERVICECHECK_UNREGISTER_IF_NOT_HERE);
                BluetoothThreadManager.submit(serviceCheckAgent);
            }
        }

        // Detect new devices
        for (RemoteDevice remote : discovered) {
            if (!m_devices.containsKey(remote)) {
                if (matchesDeviceFilter(remote)) {
                    m_logger.info("New device found (" + remote.getBluetoothAddress() + ")");
                    register(remote);
                } else {
                    m_logger.info("Device ignored because it does not match the device filter");
                }
            } else {
                m_logger.info("Already known device " + remote.getBluetoothAddress());
            }
        }

        if ("bluez".equals(getBluetoothStack())) {
            // Workaround for bluez : trying to keep all the paired devices.
            // Has bluez doesn't return the paired devices when we have an inquiry, we can try to search if some of the
            // cached devices is are reachable
            LocalDevice local = null;
            try {
                local = LocalDevice.getLocalDevice();
            } catch (BluetoothStateException e) {
                m_logger.error("Bluetooth Adapter not started.");
            }
            RemoteDevice[] cachedDevices = local.getDiscoveryAgent().retrieveDevices(DiscoveryAgent.CACHED);
            if (cachedDevices == null || cachedDevices.length == 0) {
                return;
            }
            presents = new HashSet<RemoteDevice>(m_devices.keySet());
            for (RemoteDevice cached : cachedDevices) {
                if (!contains(presents, cached)) {
                    ServiceCheckAgent serviceCheckAgent = new ServiceCheckAgent(cached, SERVICECHECK_REGISTER_IF_HERE);
                    BluetoothThreadManager.submit(serviceCheckAgent);
                }
            }
        }
    }

    /**
     * Checks whether the given list contains the given device.
     * The check is based on the bluetooth address.
     *
     * @param list   a non-null list of remote device
     * @param device the device to check
     * @return <code>true</code> if the device is in the list, <code>false</code> otherwise.
     */

    public static boolean contains(Set<RemoteDevice> list, RemoteDevice device) {
        for (RemoteDevice d : list) {
            if (d.getBluetoothAddress().equals(device.getBluetoothAddress())) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesDeviceFilter(RemoteDevice device) {
        if (m_filter == null) {
            // No filter... all devices accepted
            return true;
        }

        String address = device.getBluetoothAddress();
        String name = getDeviceName(device);

        return (m_filter.matcher(address).matches() || (name != null && m_filter.matcher(name).matches()));
    }

    private String getDeviceName(RemoteDevice device) {
        String name = m_names.getProperty(device.getBluetoothAddress());
        if (name == null) {
            try {
                name = device.getFriendlyName(false);
                if (name != null && name.length() != 0) {
                    m_logger.info("New device name discovered : " + device.getBluetoothAddress() + " => " + name);
                    m_names.setProperty(device.getBluetoothAddress(), name);
                }
            } catch (IOException e) {
                m_logger.info("Not able to get the device friendly name of " + device.getBluetoothAddress(), e);
            }
        } else {
            m_logger.info("Found the device name in memory : " + device.getBluetoothAddress() + " => " + name);
        }
        return name;
    }

    private synchronized void unregisterAll() {
        for (Map.Entry<RemoteDevice, ServiceRegistration> entry : m_devices.entrySet()) {
            entry.getValue().unregister();
            unpair(entry.getKey());
        }
        m_devices.clear();
    }

    private synchronized void unregister(RemoteDevice device) {
        ServiceRegistration reg = m_devices.remove(device);
        if (reg != null) {
            reg.unregister();
        }
        unpair(device);

    }

    private synchronized void register(RemoteDevice device) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("device.id", device.getBluetoothAddress());
        String name = getDeviceName(device);

        if (name != null) {
            // Switch device to our own implementation
            device = new RemoteNamedDevice(device, name);
            props.put("device.name", name);
        } else if (m_ignoreUnnamedDevices) {
            m_logger.warn("Ignoring device " + device.getBluetoothAddress() + " - discovery set to ignore " +
                    "unnamed devices");
            return;
        }

        m_logger.info("Registering new service for " + device.getBluetoothAddress() + " with properties " + props);

        // check autopairing
        if (!device.isAuthenticated()) {
            if (!pair(device)) {
                m_logger.warn("Aborting registering for " + device.getBluetoothAddress());
                return;
            }
        }

        ServiceRegistration reg = m_context.registerService(RemoteDevice.class.getName(), device, props);
        m_devices.put(device, reg);

    }

    void unpair(RemoteDevice device) {
        if (matchesDeviceFilter(device) && m_unpairLostDevices) {
            try {
                RemoteDeviceHelper.removeAuthentication(device);
            } catch (IOException e) {
                m_logger.error("Can't unpair device " + device.getBluetoothAddress(), e);
            }
        }
    }

    boolean pair(final RemoteDevice device) {
        if (m_fleet == null || m_fleet.getDevices() == null) {
            m_logger.info("Ignoring autopairing - no fleet configured");
            return true;
        }

        String address = device.getBluetoothAddress();
        String name = getDeviceName(device);

        if (name == null && m_ignoreUnnamedDevices) {
            m_logger.warn("Pairing not attempted, ignoring unnamed devices");
            return false;
        }

        List<Device> devices = m_fleet.getDevices();
        for (Device model : devices) {
            String regex = model.getId();
            String pin = model.getPin();
            if (Pattern.matches(regex, address) || (name != null && Pattern.matches(regex, name))) {
                m_logger.info("Paring pattern match for " + address + " / " + name + " with " + regex);
                try {
                    RemoteDeviceHelper.authenticate(device, pin);
                    m_logger.info("Device " + address + " paired");
                    return true;
                } catch (IOException e) {
                    m_logger.error("Cannot authenticate device despite it match the regex " + regex, e);
                }
            }
        }
        return false;
    }

    private static final int SERVICECHECK_UNREGISTER_IF_NOT_HERE = 0;

    private static final int SERVICECHECK_REGISTER_IF_HERE = 1;

    class ServiceCheckAgent implements Runnable, DiscoveryListener {

        private final RemoteDevice m_device;

        private final int m_action;

        private boolean m_searchInProgress = false;

        private Logger m_logger = LoggerFactory.getLogger(ServiceCheckAgent.class);

        public ServiceCheckAgent(RemoteDevice remoteDevice, int action) {
            if (action != SERVICECHECK_REGISTER_IF_HERE && action != SERVICECHECK_UNREGISTER_IF_NOT_HERE) {
                throw new IllegalArgumentException();
            }
            m_device = remoteDevice;
            m_action = action;
        }

        private LocalDevice initialize() {
            LocalDevice local = null;
            try {
                local = LocalDevice.getLocalDevice();
            } catch (BluetoothStateException e) {
                m_logger.error("Bluetooth Adapter not started.");
            }
            return local;
        }

        public void run() {
            try {
                LocalDevice local = initialize();
                if (!LocalDevice.isPowerOn() || local == null) {
                    m_logger.error("Bluetooth adapter not ready");
                    unregister(m_device);
                    return;
                }
                doSearch(local);
            } catch (Throwable e) {
                m_logger.error("Unexpected exception during service inquiry", e);
                unregister(m_device);
            }
        }

        void doSearch(LocalDevice local) {
            synchronized (this) {
                m_searchInProgress = true;
                try {

                    if (Env.isTestEnvironmentEnabled()) {
                        m_logger.warn("=== TEST ENVIRONMENT ENABLED ===");
                    } else {
                        javax.bluetooth.UUID[] searchUuidSet = {UUIDs.PUBLIC_BROWSE_GROUP};
                        local.getDiscoveryAgent().searchServices(null, searchUuidSet, m_device, this);
                    }
                    wait();
                } catch (InterruptedException e) {
                    if (m_searchInProgress) {
                        // we're stopping, aborting discovery.
                        m_searchInProgress = false;
                        m_logger.warn("Interrupting bluetooth service discovery - interruption");
                    } else {
                        // Search done !
                    }
                } catch (BluetoothStateException e) {
                    // well ... bad choice. Bluetooth driver not ready
                    // Just abort.
                    m_logger.error("Cannot search for bluetooth services", e);
                    unregister(m_device);
                    return;
                }
                // Do nothing
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
            // Do nothing
        }

        @Override
        public void serviceSearchCompleted(int transID, int respCode) {
            if (respCode != SERVICE_SEARCH_COMPLETED) {
                if (m_action == SERVICECHECK_UNREGISTER_IF_NOT_HERE) {
                    m_logger.info("Device " + m_device.getBluetoothAddress() + " have disappeared : Unregister it.");
                    unregister(m_device);
                } else if (m_action == SERVICECHECK_REGISTER_IF_HERE) {
                    m_logger.info("Device " + m_device.getBluetoothAddress() + " is not here");
                }
            } else {
                if (m_action == SERVICECHECK_REGISTER_IF_HERE) {
                    m_logger.info("Device " + m_device.getBluetoothAddress() + " is here : Register it.");
                    register(m_device);
                } else if (m_action == SERVICECHECK_UNREGISTER_IF_NOT_HERE) {
                    m_logger.info("Device " + m_device.getBluetoothAddress() + " is still here.");
                }
            }

            synchronized (this) {
                m_searchInProgress = false;
                notifyAll();
            }
        }

        @Override
        public void inquiryCompleted(int discType) {
            // Not used here.
        }
    }

}
