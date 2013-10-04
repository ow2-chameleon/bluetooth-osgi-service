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

import javax.bluetooth.DataElement;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import java.io.IOException;

public class ServiceRecordStub implements ServiceRecord {

    RemoteDevice remote;

    String name;

    public ServiceRecordStub(RemoteDevice device, String name) {
        remote = device;
        this.name = name;
    }

    @Override
    public int[] getAttributeIDs() {
        return ServiceDiscoveryAgent.attrIDs;
    }

    @Override
    public DataElement getAttributeValue(int arg0) {
        return new DataElement(DataElement.STRING, name);
    }

    @Override
    public String getConnectionURL(int arg0, boolean arg1) {
        return "obex://stub-" + remote.getBluetoothAddress() + "/" + name;
    }

    @Override
    public RemoteDevice getHostDevice() {
        return remote;
    }

    @Override
    public boolean populateRecord(int[] arg0) throws IOException {
        return false;
    }

    @Override
    public boolean setAttributeValue(int arg0, DataElement arg1) {
        return false;
    }

    @Override
    public void setDeviceServiceClasses(int arg0) {

    }

}
