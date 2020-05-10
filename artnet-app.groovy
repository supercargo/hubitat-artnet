definition(
    name: "Art-Net Controller",
    namespace: "ruld",
    author: "Adam Lewis",
    description: "Expose Art-Net Nodes as Hubitat Devices",
    category: "Lighting",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this Art-Net Controller", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "luxSensors", "capability.illuminanceMeasurement", title: "Select Illuminance Sensors", submitOnChange: true, required: true, multiple: true
			if(luxSensors) paragraph "Current average is ${averageLux()} lux"
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	def averageDev = getChildDevice("AverageLux_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Illuminance Sensor", "AverageLux_${app.id}", null, [label: thisName, name: thisName])
	averageDev.setLux(averageLux())
	subscribe(luxSensors, "illuminance", handler)
}

def averageLux() {
	def total = 0
	def n = luxSensors.size()
	luxSensors.each {total += it.currentIlluminance}
	return (total / n).toDouble().round(0).toInteger()
}

def handler(evt) {
	def averageDev = getChildDevice("AverageLux_${app.id}")
	def avg = averageLux()
	averageDev.setLux(avg)
	log.info "Average illuminance = $avg lux"
}
