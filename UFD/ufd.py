
from operator import attrgetter
from time import time
import numpy as np
import json
import ipaddress
from six.moves import http_client

from ryu.base import app_manager
from ryu.controller import ofp_event
# from ryu.controller import dpset
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER, DEAD_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3
from ryu.ofproto.ofproto_v1_3_parser import OFPActionOutput
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from ryu.lib.packet import arp
from ryu.lib.packet import ether_types
from ryu.lib import hub
from ryu.app.wsgi import ControllerBase
from ryu.app.wsgi import Response
from ryu.app.wsgi import route
from ryu.app.wsgi import WSGIApplication

ufd_app_name = 'ufd_app'
url_overload = '/ufd/port'
url_port_statistic = '/ufd/port-statistic'
window = 10

filename = '/home/node2/bots-remain'

class SimpleMonitor13(app_manager.RyuApp):

    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]
    _CONTEXTS = {'wsgi': WSGIApplication}
    # _CONTEXTS = {'dpset': dpset.DPSet}

    def __init__(self, *args, **kwargs):
        super(SimpleMonitor13, self).__init__(*args, **kwargs)
        self.port_stats = {}
        self.flow_stats = {}
        self.delete_ports = {}
        self.datapaths = {}
        self.monitor_thread = hub.spawn(self._monitor)

        wsgi = kwargs['wsgi']
        wsgi.register(UFDReceiver, {ufd_app_name: self})
        # self.dpset = kwargs['dpset']
        # for dp in self.dpset.get_all():
            # self.datapaths[dp[0]] = dp[1]
            # self.port_stats[dp[0]] = {}

    def get_port_statistic(self):
        body = ''
        for dp_id in self.datapaths:
            for port in self.port_stats[dp_id]:
                if port != 10001:
                    body = body + '%d '%(port) + '%d\n'%(self.port_stats[dp_id][port]['stats'])
        return body

    def clean_overload_port(self, port):
#        self.logger.info('%d', port)
        for dp_id in self.datapaths:
            if (port in self.port_stats[dp_id]) and (port != 10001):

                connections = []
                for connection in self.port_stats[dp_id][port]['users']:
                    if self.flow_stats[dp_id].has_key(connection):
                        connections.append((self.flow_stats[dp_id][connection][(connection[0], connection[1])]+self.flow_stats[dp_id][connection][(connection[1], connection[0])], connection))
                connections.sort(reverse = True)


                for i in range(0, len(connections), 2):
                    connection = connections[i][1]
                    self.port_stats[dp_id][port]['users'].discard(connection)
                    if self.flow_stats[dp_id].has_key(connection):
                        del self.flow_stats[dp_id][connection]
                    dp = self.datapaths[dp_id]
                    parser = dp.ofproto_parser
                    match_s2d = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[0], ipv4_src=connection[1])
                    match_d2s = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[1], ipv4_src=connection[0])
                    self.del_flow(dp, match_s2d, port)
                    self.del_flow(dp, match_d2s, port)

#                file = open(filename, 'w')

#                for i in range(0, len(connections), 1):
#                    connection = connections[i][1]
#                    if i % 2 == 0:
#                        self.port_stats[dp_id][port]['users'].discard(connection)
#                        if self.flow_stats[dp_id].has_key(connection):
#                            del self.flow_stats[dp_id][connection]
#                        dp = self.datapaths[dp_id]
#                        parser = dp.ofproto_parser
#                        match_s2d = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[0], ipv4_src=connection[1])
#                        match_d2s = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[1], ipv4_src=connection[0])
#                        self.del_flow(dp, match_s2d, port)
#                        self.del_flow(dp, match_d2s, port)
#                    if i % 4 != 0:
#                        if ipaddress.ip_address(unicode(connection[0])) in ipaddress.ip_network(u'192.168.95.0/24') and ipaddress.ip_address(unicode(connection[1])) not in ipaddress.ip_network(u'192.168.95.0/24'):
#                            file.write(connection[1])
#                            file.write('\n')
#                        elif ipaddress.ip_address(unicode(connection[1])) in ipaddress.ip_network(u'192.168.95.0/24') and ipaddress.ip_address(unicode(connection[0])) not in ipaddress.ip_network(u'192.168.95.0/24'):
#                            file.write(connection[0])
#                            file.write('\n')

#                for i in self.port_stats[dp_id]:
#                    if i != port and i != 10001:
#                        for connection in self.port_stats[dp_id][i]['users']:
#                            if ipaddress.ip_address(unicode(connection[0])) in ipaddress.ip_network(u'192.168.95.0/24') and ipaddress.ip_address(unicode(connection[1])) not in ipaddress.ip_network(u'192.168.95.0/24'):
#                                file.write(connection[1])
#                                file.write('\n')
#                            elif ipaddress.ip_address(unicode(connection[1])) in ipaddress.ip_network(u'192.168.95.0/24') and ipaddress.ip_address(unicode(connection[0])) not in ipaddress.ip_network(u'192.168.95.0/24'):
#                                file.write(connection[0])
#                                file.write('\n')

#                file.close()




    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def switch_features_handler(self, ev):
        datapath = ev.msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        match = parser.OFPMatch()
        self.del_flow(datapath, match)
        match = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst='192.168.95.0/24')
        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER)]
#        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
        self.add_flow(datapath, 0, match, actions)
        match = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_ARP)
        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER)]
#        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
        self.add_flow(datapath, 0, match, actions)
#        match = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_ARP, arp_op=arp.ARP_REQUEST)
#        actions = [parser.OFPActionOutput(10001)]
#        self.add_flow(datapath, 2, match, actions)
#        match = parser.OFPMatch(in_port=10001, eth_type=ether_types.ETH_TYPE_ARP, arp_op=arp.ARP_REPLY)
#        actions = [parser.OFPActionOutput(ofproto.OFPP_ALL)]
#        self.add_flow(datapath, 3, match, actions)
#        match = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_ARP, arp_op=arp.ARP_REQUEST, arp_tpa='192.168.97.101')
#        actions = []
#        self.add_flow(datapath, 4, match, actions)
        req = parser.OFPPortDescStatsRequest(datapath, 0)
        datapath.send_msg(req)
        if datapath.id not in self.datapaths:
            self.logger.debug('register datapath: %016x', datapath.id)
            self.datapaths[datapath.id] = datapath
            self.port_stats.setdefault(datapath.id, {})
            self.delete_ports.setdefault(datapath.id, set())
            self.flow_stats[datapath.id] = {}

    @set_ev_cls(ofp_event.EventOFPPortDescStatsReply, MAIN_DISPATCHER)
    def port_desc_stats_reply_handler(self, ev):
        msg = ev.msg
        dp = msg.datapath
        ofp = dp.ofproto
        parser = dp.ofproto_parser
        for port in ev.msg.body:
            if port.port_no < 5000:
                match = parser.OFPMatch(in_port=port.port_no, eth_type=ether_types.ETH_TYPE_IP)
                actions = [parser.OFPActionOutput(10001)]
                self.add_flow(dp, 1, match, actions)

    @set_ev_cls(ofp_event.EventOFPStateChange,
                [MAIN_DISPATCHER, DEAD_DISPATCHER])
    def _state_change_handler(self, ev):
        datapath = ev.datapath
        if ev.state == MAIN_DISPATCHER:
            if datapath.id not in self.datapaths:
                self.logger.debug('register datapath: %016x', datapath.id)
                self.datapaths[datapath.id] = datapath
                self.flow_stats[datapath.id] = {}
                self.port_stats.setdefault(datapath.id, {})
                self.delete_ports.setdefault(datapath.id, set())
        elif ev.state == DEAD_DISPATCHER:
            if datapath.id in self.datapaths:
                self.logger.debug('unregister datapath: %016x', datapath.id)
                if self.datapaths.has_key(datapath.id):
                    del self.datapaths[datapath.id]
                if self.port_stats.has_key(datapath.id):
                    del self.port_stats[datapath.id]
                if self.delete_ports.has_key(datapath.id):
                    del self.delete_ports[datapath.id]
                if self.flow_stats.has_key(datapath.id):
                    del self.flow_stats[datapath.id]

    def add_flow(self, datapath, priority, match, actions, buffer_id=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        inst = [parser.OFPInstructionActions(ofproto.OFPIT_APPLY_ACTIONS,
                                             actions)]
        if buffer_id:
            mod = parser.OFPFlowMod(datapath=datapath, buffer_id=buffer_id,
                                    priority=priority, match=match,
                                    instructions=inst)
        else:
            mod = parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    match=match, instructions=inst)
        datapath.send_msg(mod)

    def del_flow(self, datapath, match, out=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        if not out:
            out = ofproto.OFPP_ANY
        mod = parser.OFPFlowMod(datapath=datapath,
                                command=ofproto.OFPFC_DELETE,
                                table_id=ofproto.OFPTT_ALL,
                                out_port=out,
                                out_group=ofproto.OFPG_ANY,
                                match=match)
        datapath.send_msg(mod)

    @set_ev_cls(ofp_event.EventOFPPortStatus, MAIN_DISPATCHER)
    def port_status_handler(self, ev):
        msg = ev.msg
        dp = msg.datapath
        ofp = dp.ofproto
        parser = dp.ofproto_parser
        port_no = msg.desc.port_no
        match = parser.OFPMatch(in_port=port_no, eth_type=ether_types.ETH_TYPE_IP)
        if port_no < 10000:
            if msg.reason == ofp.OFPPR_ADD:
                if port_no not in self.port_stats[dp.id]:
                    # self.port_stats[dp.id].setdefault(port_no, {'stats':[0 for i in range(window)], 'users':set()})
                    self.port_stats[dp.id].setdefault(port_no, {'stats':0, 'users':set()})
                if port_no < 5000:
                    actions = [parser.OFPActionOutput(10001)]
                    self.add_flow(dp, 1, match, actions)
            elif msg.reason == ofp.OFPPR_DELETE:
                if port_no < 5000:
                    self.del_flow(dp, match)
                match = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP)
                self.del_flow(dp, match, port_no)
                if port_no in self.port_stats[dp.id]:
                    for connection in self.port_stats[dp.id][port_no]['users']:
                        if self.flow_stats[dp.id].has_key(connection):
                            del self.flow_stats[dp.id][connection]
                    if self.port_stats[dp.id].has_key(port_no):
                        del self.port_stats[dp.id][port_no]
                self.delete_ports[dp.id].add(port_no)

        self.logger.debug('OFPPortStatus received: port_no=%d', port_no)
        self.logger.info('OFPPortStatus received: port_no=%d', port_no)
        print msg.reason

    def _monitor(self):
        while True:
            for dp in self.datapaths.values():
                self._request_stats(dp)
            hub.sleep(0.5)

    def _request_stats(self, datapath):
        self.logger.debug('send stats request: %016x', datapath.id)
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        match = parser.OFPMatch(in_port=10001, eth_type=ether_types.ETH_TYPE_IP)
        cookie = cookie_mask = 0
        req = parser.OFPFlowStatsRequest(datapath, 0, ofproto.OFPTT_ALL, ofproto.OFPP_ANY, ofproto.OFPG_ANY, cookie, cookie_mask, match)
        datapath.send_msg(req)

        req = parser.OFPPortStatsRequest(datapath, 0, ofproto.OFPP_ANY)
        datapath.send_msg(req)

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        if ev.msg.msg_len < ev.msg.total_len:
            self.logger.debug("packet truncated: only %s of %s bytes",
                              ev.msg.msg_len, ev.msg.total_len)
        msg = ev.msg
        datapath = msg.datapath
        dpid = datapath.id
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        in_port = msg.match['in_port']

        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocols(ethernet.ethernet)[0]

        if eth.ethertype == ether_types.ETH_TYPE_ARP:
            arp_pkt = pkt.get_protocols(arp.arp)[0]
            opcode = arp_pkt.opcode
            src_ip = arp_pkt.src_ip
            dst_ip = arp_pkt.dst_ip

            if (opcode == arp.ARP_REQUEST) and (in_port < 5000) and (src_ip == '192.168.97.101') and (dst_ip == '192.168.97.1'):
                actions = [parser.OFPActionOutput(10001)]
            elif (opcode == arp.ARP_REPLY) and (in_port == 10001) and (src_ip == '192.168.97.1') and (dst_ip == '192.168.97.101'):
                actions = []
                for port_no in self.port_stats[dpid].keys():
                    if port_no < 5000:
                        actions.append(parser.OFPActionOutput(port_no))
            else:
                return

            data = None
            if msg.buffer_id == ofproto.OFP_NO_BUFFER:
                data = msg.data

            out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port, actions=actions, data=data)
            datapath.send_msg(out)

        elif (eth.ethertype == ether_types.ETH_TYPE_IP) and (in_port == 10001):
            ip_pkt = pkt.get_protocols(ipv4.ipv4)[0]
            src = ip_pkt.src
            dst = ip_pkt.dst

            if len(self.port_stats[dpid].keys()) > 0:
                max_pkt = float('-inf')
                ports = self.port_stats[dpid].keys()
                ports.sort()
                max_length = max([self.port_stats[dpid][port_no]['stats']+1 for port_no in ports])
                for port_no in ports:
                    if (np.log(max_length - self.port_stats[dpid][port_no]['stats']) > min_pkt):
                        max_pkt = np.log(max_length - self.port_stats[dpid][port_no]['stats'])
                        out_port = port_no

                self.port_stats[dpid][out_port]['users'].add(tuple(sorted([src, dst])))

                actions = [parser.OFPActionSetField(eth_dst="66:66:66:66:66:66"), parser.OFPActionOutput(out_port)]

                match_s2d = parser.OFPMatch(in_port=in_port, eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=dst, ipv4_src=src)
                match_d2s = parser.OFPMatch(in_port=in_port, eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=src, ipv4_src=dst)


                self.add_flow(datapath, 1, match_d2s, actions)
                if msg.buffer_id != ofproto.OFP_NO_BUFFER:
                    self.add_flow(datapath, 1, match_s2d, actions, msg.buffer_id)
                    return
                else:
                    self.add_flow(datapath, 1, match_s2d, actions)

                data = None
                if msg.buffer_id == ofproto.OFP_NO_BUFFER:
                    data = msg.data

                out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id,
                                          in_port=in_port, actions=actions, data=data)
                datapath.send_msg(out)
#            else:
#                return
#        else:
#            return

    @set_ev_cls(ofp_event.EventOFPFlowStatsReply, MAIN_DISPATCHER)
    def _flow_stats_reply_handler(self, ev):
        body = ev.msg.body
        dp_id = ev.msg.datapath.id

        time_now = time()

        for stat in sorted([flow for flow in body if flow.priority == 1], key=lambda flow: flow.packet_count):
            if ('ipv4_src' in stat.match) and ('ipv4_dst' in stat.match):
                src = stat.match['ipv4_src']
                dst = stat.match['ipv4_dst']
                connection = tuple(sorted([src, dst]))
                direction = (src, dst)
                if connection not in self.flow_stats[dp_id]:
                    self.flow_stats[dp_id][connection] = {}
                    self.flow_stats[dp_id][connection][direction] = -1
                    self.flow_stats[dp_id][connection][(dst, src)] = -1
                    for action in stat.instructions[0].actions:
                        if isinstance(action, OFPActionOutput):
                            self.flow_stats[dp_id][connection].setdefault('port', action.port)
#                            self.logger.info("out_port:%d", action.port)
                            break

                if stat.packet_count > self.flow_stats[dp_id][connection][direction]:
                    self.flow_stats[dp_id][connection][direction] = stat.packet_count
                    self.flow_stats[dp_id][connection]['time'] = time_now

        for connection in self.flow_stats[dp_id].keys():
            if self.flow_stats[dp_id].has_key(connection):
                if time_now > self.flow_stats[dp_id][connection]['time']+float(10):
                    self.port_stats[dp_id][self.flow_stats[dp_id][connection]['port']]['users'].discard(connection)
                    if self.flow_stats[dp_id].has_key(connection):
                        del self.flow_stats[dp_id][connection]
                    dp = ev.msg.datapath
                    parser = dp.ofproto_parser
                    match_s2d = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[0], ipv4_src=connection[1])
                    match_d2s = parser.OFPMatch(eth_type=ether_types.ETH_TYPE_IP, ipv4_dst=connection[1], ipv4_src=connection[0])
                    self.del_flow(dp, match_s2d)
                    self.del_flow(dp, match_d2s)




    @set_ev_cls(ofp_event.EventOFPPortStatsReply, MAIN_DISPATCHER)
    def _port_stats_reply_handler(self, ev):
        body = ev.msg.body
        dp_id = ev.msg.datapath.id

        for stat in sorted(body, key=attrgetter('port_no')):
            port_no = stat.port_no
            if (port_no < 10000) and (port_no not in self.delete_ports[dp_id]):
                if port_no not in self.port_stats[dp_id]:
                    self.port_stats[dp_id].setdefault(port_no, {})
                    self.port_stats[dp_id][port_no].setdefault('stats', np.absolute(stat.tx_packets - stat.rx_packets))
                    self.port_stats[dp_id][port_no].setdefault('users', set())
                    # for i in range(window):
                    #     self.port_stats[dp_id][port_no]['stats'].append(stat.tx_packets)
                else:
                    self.port_stats[dp_id][port_no]['stats'] = np.absolute(stat.tx_packets - stat.rx_packets)
                    # self.port_stats[dp_id][port_no]['stats'].pop(0)
                    # self.port_stats[dp_id][port_no]['stats'].append(stat.tx_packets)
#                    self.logger.info('%016x %8d %8d', dp_id, port_no, self.port_stats[dp_id][port_no]['stats'][-1]-self.port_stats[dp_id][port_no]['stats'][0])
        self.delete_ports[dp_id].clear()

class UFDReceiver(ControllerBase):

    def __init__(self, req, link, data, **config):
        super(UFDReceiver, self).__init__(req, link, data, **config)
        self.ufd_app = data[ufd_app_name]

    @route('ufd', url_overload, methods=['POST'], requirements=None)
    def put_port(self, req, **kwargs):

        ufd = self.ufd_app

        try:
            new_entry = req.json if req.body else {}
        except ValueError:
            raise Response(status=http_client.BAD_REQUEST)


#        ufd.clean_overload_port(new_entry['port'])
        try:
            ufd.clean_overload_port(new_entry['port'])
        except Exception as e:
            print(e)
#            return Response(status=http_client.INTERNAL_SERVER_ERROR)

    @route('ufd-statistic', url_port_statistic, methods=['POST'], requirements=None)
    def get_port_statistic(self, req, **kwargs):

        ufd = self.ufd_app

        try:
            body = ufd.get_port_statistic()
        except Exception as e:
            return Response(status=http_client.INTERNAL_SERVER_ERROR)

        if bool(body):
            return Response(status=http_client.OK, body=body)
#        else:
#            return Response(status=http_client.NO_CONTENT)
