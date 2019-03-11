# CODE4MEC &copy;

CODE4MEC is a cooperative security defense framework for 5G mobile or multi-access edge computing (MEC).
***
## Objective
The CODE4MEC aims to adapt to defense workload changes by provisioning and de-provisioning container-carried defense resources from a cluster of MEC nodes in an automatic way.

## Guides
CODE4MEC framework contains two kinds of components: the cooperative defense orchestrator (CDO) and agents in MEC nodes. 

* The CDO (one project included) works as a module  atop the floodlight. It listens to requests from agents in MEC nodes, and launches two threads for cooperative defense scheduling and cooperative defense routing rules updating, respectively. Different from classical SDN, we use the proactive routing rule updating scheme for cooperative defense flows, where the CDO updates routing rules for the corresponding vSwitches, once provider-requester pairs are established or broken. 

* Two projects are included for agents, i.e., the UFD and CXXPure (refered to as CFE in our paper) project. CFE is independently developed, which manages local cooperative defense. UFD is implemented based on OVS, over which the flow-based traffic and context information coordination scheme has been developed. 

For the detailed illustration of the CODE4MEC framework, please refer to our paper.



