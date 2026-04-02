/*
 Hubitat WeatherSense App (converted from HA config flow).

 Original: ha-weathersense/config_flow.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV  
 Source:   https://github.com/smkrv/ha-weathersense
*/

import groovy.transform.Field

definition(
    name: "WeatherSense",
    namespace: "weathersense",
    author: "SMKRV (Hubitat port)",
    description: "Feels-like temperature with comfort levels",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
    page(name: "sensorPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "WeatherSense Setup", install: true, uninstall: true) {
        section("WeatherSense Settings") {
            input name: "appName", type: "text", title: "Name", defaultValue: "Feels Like Temperature", submitOnChange: true
            input name: "isOutdoor", type: "bool", title: "Outdoor Location?", defaultValue: true, submitOnChange: true
        }
        
        if (isOutdoor) {
            href "sensorPage", title: "Configure Sensors →", description: ""
        } else {
            section("Indoor Sensors (Required)") {
                input name: "temperatureSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, submitOnChange: true
                input name: "humiditySensor", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: true, submitOnChange: true
            }
        }
        
        section("Advanced Options") {
            input name: "displayUnit", type: "enum", title: "Display Unit", 
                   options: [["C": "Celsius (°C)"], ["F": "Fahrenheit (°F)"]], 
                   defaultValue: "C", submitOnChange: true
        }
    }
}

def sensorPage() {
    dynamicPage(name: "sensorPage", title: "Outdoor Sensors", install: true, uninstall: true) {
        section("Required Sensors") {
            input name: "temperatureSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, submitOnChange: true
            input name: "humiditySensor", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: true, submitOnChange: true
        }
        
        section("Optional Weather Sensors") {
            input name: "windSpeedSensor", type: "capability.illuminanceMeasurement", title: "Wind Speed Sensor (m/s)", required: false, submitOnChange: true
            input name: "pressureSensor", type: "capability.illuminanceMeasurement", title: "Pressure Sensor (kPa)", required: false, submitOnChange: true
            input name: "solarRadiationSensor", type: "capability.illuminanceMeasurement", title: "Solar Radiation", required: false, submitOnChange: true
            input name: "windDirectionSensor", type: "capability.illuminanceMeasurement", title: "Wind Direction (degrees)", required: false, submitOnChange: true
        }
        
        section("Advanced Features") {
            input name: "enableWindDirCorrection", type: "bool", title: "Enable Wind Direction Correction?", defaultValue: false, submitOnChange: true
            input name: "enableSmoothing", type: "bool", title: "Enable Smoothing?", defaultValue: false, submitOnChange: true
            input name: "smoothingFactor", type: "decimal", title: "Smoothing Factor (0.05-0.95)", defaultValue: 0.3, range: "0.05..0.95", submitOnChange: true
        }
        
        section {
            href "mainPage", title: "← Back to Main Settings"
        }
    }
}

// Installed callback
def installed() {
    log.debug "WeatherSense installed"
    initialize()
}

// Updated callback  
def updated(settings) {
    log.debug "WeatherSense updated: ${settings}"
    unsubscribe()
    initialize()
}

// Initialization
def initialize() {
    // Subscribe to sensor changes
    subscribe(temperatureSensor, "temperature", sensorHandler)
    subscribe(humiditySensor, "humidity", sensorHandler)
    if (windSpeedSensor) subscribe(windSpeedSensor, "illuminance", sensorHandler)  // Reuse illuminance for numeric values
    if (pressureSensor) subscribe(pressureSensor, "illuminance", sensorHandler)
    if (windDirectionSensor) subscribe(windDirectionSensor, "illuminance", sensorHandler)
    
    // Create child virtual sensor for feels-like temp
    def child = addChildDevice("hubitat", "Virtual Temperature Sensor", "${app.id}-feelslike", [name: "${app.label} FeelsLike", isComponent: false])
    state.childDeviceId = child.deviceNetworkId
    
    calculateAndUpdate()
}

// Main sensor event handler
def sensorHandler(evt) {
    log.debug "${evt.name} changed to ${evt.value}"
    runIn(2, calculateAndUpdate)  // Debounce
}

// Core calculation + state update
def calculateAndUpdate() {
    def temp = getSensorValue(temperatureSensor, "temperature")
    def humidity = getSensorValue(humiditySensor, "humidity")
    
    if (!temp || !humidity) {
        log.warn "Missing required sensors"
        return
    }
    
    def config = [
        temperature: temp,
        humidity: humidity,
        windSpeed: getSensorValue(windSpeedSensor, "illuminance"),
        pressure: getSensorValue(pressureSensor, "illuminance"),
        windDirection: getSensorValue(windDirectionSensor, "illuminance"),
        isOutdoor: isOutdoor ?: true,
        timeOfDay: new Date(),
        latitude: location.latitude,  // Hubitat location
        enableWindDirectionCorrection: enableWindDirCorrection ?: false
    ]
    
    def result = WeatherSenseCalculator.calculateFeelsLike(**config)
    
    // Update child device
    def childDni = state.childDeviceId
    def child = getChildDevice(childDni)
    if (child) {
        child.sendEvent(name: "temperature", value: result.feelsLike)
        child.sendEvent(name: "comfortLevel", value: result.comfortLevel)
        child.sendEvent(name: "calculationMethod", value: result.method)
    }
    
    // Update app state
    state.lastFeelsLike = result.feelsLike
    state.lastComfort = result.comfortLevel
    state.lastCalcTime = new Date()
    
    log.info "Feels Like: ${result.feelsLike}°C (${result.comfortLevel}) via ${result.method}"
}

// Helper to safely get sensor values
private BigDecimal getSensorValue(device, attribute) {
    if (!device) return null
    def val = device.currentValue(attribute)
    return val ? val.toBigDecimal() : null
}

// Dashboard tile support
attribute "feelsLikeTemp", "number"
attribute "comfortLevel", "string"

def getFeelsLikeTemp() { state.lastFeelsLike }
def getComfortLevel() { state.lastComfort }
