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


import javax.bluetooth.UUID;

public class UUIDs {

    public static final UUID SDP = new UUID(0x0001);

    public static final UUID RFCOMM = new UUID(0x0003);

    public static final UUID ATT = new UUID(0x0007);

    public static final UUID OBEX = new UUID(0x0008);

    public static final UUID HTTP = new UUID(0x000C);

    public static final UUID L2CAP = new UUID(0x0100);

    public static final UUID BNEP = new UUID(0x000F);

    public static final UUID SERIAL_PORT = new UUID(0x1101);

    public static final UUID SERVICE_DISCOVERY_SERVER_SERVICE_CLASSID = new UUID(0x1000);

    public static final UUID BROWSE_GROUP_DESCRIPTOR_SERVICE_CLASSID = new UUID(0x1001);

    public static final UUID PUBLIC_BROWSE_GROUP = new UUID(0x1002);

    public static final UUID OBEX_OBJECT_PUSH_PROFILE = new UUID(0x1105);

    public static final UUID OBEX_FILE_TRANSFER_PROFILE = new UUID(0x1106);

    public static final UUID PERSONAL_AREA_NETWORKING_USER = new UUID(0x1115);

    public static final UUID NETWORK_ACCESS_POUUID = new UUID(0x1116);

    public static final UUID GROUP_NETWORK = new UUID(0x1117);
}
