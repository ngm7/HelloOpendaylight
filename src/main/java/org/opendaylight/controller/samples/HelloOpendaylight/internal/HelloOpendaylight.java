/*
 * Copyright (C) 2014 SDN Hub

 Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.
 You may not use this file except in compliance with this License.
 You may obtain a copy of the License at

    http://www.gnu.org/licenses/gpl-3.0.txt

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.

 *
 */

package org.opendaylight.controller.samples.HelloOpendaylight.internal;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.BufferedReader;
//import java.io.DataOutputStream;
//import java.net.URL;
//import java.net.HttpURLConnection;

//import org.json.simple.JSONObject;
//import org.json.simple.JSONArray;
//import org.json.simple.parser.JSONParser;

/*import com.google.gson.JsonElement;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;


public class HelloOpendaylight implements IListenDataPacket {

    private static final Logger logger = LoggerFactory
            .getLogger(HelloOpendaylight.class);
    private ISwitchManager switchManager = null;
    private IFlowProgrammerService programmer = null;
    private IDataPacketService dataPacketService = null;
    private Map<Long, NodeConnector> mac_to_port = new HashMap<Long, NodeConnector>();
    private String function = "hub";
    private Map<Long, Long> count_srcPkts = new HashMap<Long, Long>();
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.programmer = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.programmer == s) {
            this.programmer = null;
        }
    }

    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.info("Initialized");
        // Disabling the SimpleForwarding and ARPHandler bundle to not conflict with this one
      /*  BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
          for(Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("arphandler") ||
                bundle.getSymbolicName().contains("simpleforwarding")) {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    logger.error("Exception in Bundle uninstall "+bundle.getSymbolicName(), e);
                }
            }
        }*/

    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.info("Started HELLO OPENDAYLIGHT");
        String tokenJson=null;
        String token=null;
    }

    /**
     * Function called by the dependency manager before the services
     * exporceted by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.info("Stopped HELLO OPENDAYLIGHT");
    }

    private void floodPacket(RawPacket inPkt) {
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();

        Set<NodeConnector> nodeConnectors =
                this.switchManager.getUpNodeConnectors(incoming_node);

        for (NodeConnector p : nodeConnectors) {
            if (!p.equals(incoming_connector)) {
                try {
                    RawPacket destPkt = new RawPacket(inPkt);
                    destPkt.setOutgoingNodeConnector(p);
                    this.dataPacketService.transmitDataPacket(destPkt);
                } catch (ConstructionException e2) {
                    continue;
                }
            }
        }
    }



    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }

        logger.info(" *** RECEIVED PACKET BY HELLO OPENDAYLIGHT ***");
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();

        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        updatePktCount(formattedPak, incoming_connector);
        printPktCount();
        // Hub implementation
        // if (function.equals("hub")) {
        //    floodPacket(inPkt);
        /*} else {

            Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
            if (!(formattedPak instanceof Ethernet)) {
                return PacketResult.IGNORED;
            }

            learnSourceMAC(formattedPak, incoming_connector);
            NodeConnector outgoing_connector =
                knowDestinationMAC(formattedPak);
            if (outgoing_connector == null) {
                floodPacket(inPkt);
            } else {
                if (!programFlow(formattedPak, incoming_connector,
                            outgoing_connector)) {
                    return PacketResult.IGNORED;
                }
                inPkt.setOutgoingNodeConnector(outgoing_connector);
                this.dataPacketService.transmitDataPacket(inPkt);
            }
        }*/
        return PacketResult.KEEP_PROCESSING;
    }

    private void updatePktCount(Packet formattedPak, NodeConnector incoming_connector){
        byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
        Long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
        Long initVal = new Long(1);
        if(!this.count_srcPkts.containsKey(srcMAC_val))
            this.count_srcPkts.put(srcMAC_val, initVal);
        else{
            Long val = this.count_srcPkts.get(srcMAC_val);
            this.count_srcPkts.remove(srcMAC_val);
            val=val+1;
            this.count_srcPkts.put(srcMAC_val, val);
        }
    }

    private void printPktCount(){
        logger.info(this.count_srcPkts.toString());
    }

    private void learnSourceMAC(Packet formattedPak, NodeConnector incoming_connector) {
        byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
        long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
        this.mac_to_port.put(srcMAC_val, incoming_connector);
    }

    private NodeConnector knowDestinationMAC(Packet formattedPak) {
        byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();
        long dstMAC_val = BitBufferHelper.toNumber(dstMAC);
        return this.mac_to_port.get(dstMAC_val) ;
    }

    private boolean programFlow(Packet formattedPak,
            NodeConnector incoming_connector,
            NodeConnector outgoing_connector) {
        byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();

        Match match = new Match();
        match.setField( new MatchField(MatchType.IN_PORT, incoming_connector) );
        match.setField( new MatchField(MatchType.DL_DST, dstMAC.clone()) );

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(outgoing_connector));

        Flow f = new Flow(match, actions);
        f.setIdleTimeout((short)5);

        // Modify the flow on the network node
        Node incoming_node = incoming_connector.getNode();
        Status status = programmer.addFlow(incoming_node, f);

        if (!status.isSuccess()) {
            logger.warn("SDN Plugin failed to program the flow: {}. The failure is: {}",
                    f, status.getDescription());
            return false;
        } else {
            return true;
        }
    }
}
