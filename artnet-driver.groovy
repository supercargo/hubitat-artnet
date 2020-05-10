import hubitat.device.*;
import hubitat.helper.*;

metadata {
    definition(name: "Art-Net Test", namespace: "ruld", author: "Adam Lewis") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
}

preferences {
    section("URIs") {
        input "ip", "text", title: "Node IP", required: true
        input "net", "number", range: "0..127", title: "Art-Net Net (0-127)", default: 0, required: true
        input "subnet", "number", range: "0..15", title: "Art-Net Sub-net (0-15)", default: 0, required: true
        input "universe", "number", range: "0..15", title: "DMX Universe (0-15)", default: 0, required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    
    def subUniPart = HexUtils.integerToHexString( (settings.subnet*16 + settings.universe) as int, 1)
    def netPart = HexUtils.integerToHexString(settings.net as int, 1)
    
    device.updateDataValue("header", "4172742D4E6574000050000E0000"+subUniPart+netPart)
    
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def on() {
    if (logEnable) log.debug "Art-Net ON";
    def frames = new ArrayList();
    
    def frame = new byte[128];
    for (i = 0; i<256; i++) {
        for (c = 0; c<frame.length; c++) {
            frame[c] = i;
        }
        frames.add(artDmx(frame));
    }

    artSend(frames);
}

def off() {
    log.info device.getDataValue("header")
    if (logEnable) log.debug "Creating Frames for Art-Net OFF";
    def frame = new byte[64];
    for (i = 255; i>=0; i--) {
        for (c = 0; c<64; c++) {
            frame[c] = i;
        }
        runInMillis(i*25, 'artSendDmx', [data: frame])
    }
    if (logEnable) log.debug "Sending Frames for Art-Net OFF";

}

def artSend(List<String> packets) {
    def actions = new HubMultiAction();
    for (packet in packets) {
       def action = new HubAction(packet, Protocol.LAN, [type: HubAction.Type.LAN_TYPE_UDPCLIENT, encoding: HubAction.Encoding.HEX_STRING, destinationAddress: "${settings.ip}:6454", ignoreResponse: true])
       actions.add(action);
    }
      
    try {                
        sendHubCommand(actions)
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def artSendDmx(byte[] frame) {
   artSend(artDmx(frame));
}

def artSend(String packet) {
    def action = new HubAction(packet, Protocol.LAN, [type: HubAction.Type.LAN_TYPE_UDPCLIENT, encoding: HubAction.Encoding.HEX_STRING, destinationAddress: "${settings.ip}:6454", ignoreResponse: true])
    try {                
        sendHubCommand(action)
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def String artDmx(byte[] frame) {
    def ret = StringBuilder.newInstance()
    ret.with {
        append device.getDataValue("header") as String
        append HexUtils.integerToHexString(frame.length, 2)
        append HexUtils.byteArrayToHexString(frame)
    }
    
    return ret.toString()
}
