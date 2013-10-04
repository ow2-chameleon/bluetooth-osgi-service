Configuration
=============

The bluetooth device are track by the Device Discovery, which is responsible to track reachable devices and exposes a RemoteDevice for each found device

The configuration is stored in the instance declaration of the components. Moreover the device fleet is configured
 in a separate file (device.xml).

Configuring the Device Discovery
--------------------------------

The instance configuration is stored in _deploy/org.ow2.chameleon.bluetooth.discovery.BluetoothDeviceDiscovery-discovery.cfg_.

The following snippet is a basic configuration:

    bluetooth.devices=devices.xml
    bluetooth.ignore.unamed.devices=true
    bluetooth.discovery.period=60
    bluetooth.discovery.mode=GIAC
    bluetooth.discovery.onlinecheck=false
    bluetooth.discovery.unpairOnDeparture=false

*Properties:*

 * _bluetooth.devices_: indicates the fleet file (if not set, the autopairing won't work)
 * _bluetooth.ignore.unamed.devices_: sets the bridge to ignore the unamed devices (default to true)
 * _bluetooth.discovery.period_: sets the polling period in seconds (30 seconds by default)
 * _bluetooth.discovery.mode_: sets the discovery mode (GIAC or LIAC, GIAC by default)
 * _bluetooth.discovery.onlinecheck_: enables an additional online check when a device is found. This checks allows
 detecting the devices returned by the OS which are not available (Windows 7) (default to false)
 * _bluetooth.discovery.unpairOnDeparture_: if sets to true, it will try to unpair devices when they are no more
 reachable. To use in combination with the online check.
 * _bluetooth.discovery.names_: the path to the file containing the mac to device name (names.properties by default).
  This file can be populated on deployment, or will be created. On stop, the new devices are added. To disable the
  peristent support, set this property to "" or null.






