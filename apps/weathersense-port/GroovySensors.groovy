
// Here's the ENHANCED version with sensor.py features (smoothing, attributes, etc.)

// Add to your existing app's preferences (advanced section):
input name: "smoothingEnabled", type: "bool", title: "Enable Smoothing?", defaultValue: false
input name: "smoothingFactor", type: "decimal", title: "Smoothing Factor", range: "0.05..0.95"

// Enhanced state variables (add to initialize()):
state.smoothedValue = null
state.lastCalcTime = null
state.rawInputs = [:]  // Store input history

// Enhanced calculateAndUpdate() method:
def calculateAndUpdate() {
    def inputs = getCurrentInputs()
    if (!inputs.temperature || !inputs.humidity) return
    
    def result = WeatherSenseCalculator.calculateFeelsLike(
        temperature: inputs.temperature,
        humidity: inputs.humidity,
        windSpeed: inputs.windSpeed,
        pressure: inputs.pressure,
        windDirection: inputs.windDirection,
        isOutdoor: settings.isOutdoor ?: true,
        timeOfDay: new Date(),
        latitude: location.latitude,
        enableWindDirectionCorrection: settings.enableWindDirCorrection ?: false
    )
    
    // Apply EMA smoothing if enabled
    BigDecimal rawValue = convertToDisplayUnit(result.feelsLike)
    BigDecimal smoothedValue = rawValue
    
    if (settings.smoothingEnabled && state.smoothedValue != null) {
        BigDecimal alpha = (settings.smoothingFactor ?: 0.3G)
        smoothedValue = alpha * rawValue + (1 - alpha) * state.smoothedValue
    }
    
    state.smoothedValue = smoothedValue
    state.lastResult = result
    state.lastCalcTime = new Date()
    state.rawInputs = inputs
    
    updateChildDevice(smoothedValue, result)
    
    log.info "Feels Like: ${smoothedValue} ${displayUnit()} (${result.comfortLevel})"
}

// New helper methods:

def getCurrentInputs() {
    return [
        temperature: getSensorValue(settings.temperatureSensor, "temperature"),
        humidity: getSensorValue(settings.humiditySensor, "humidity"),
        windSpeed: convertWindSpeed(getSensorValue(settings.windSpeedSensor, "illuminance")),
        pressure: convertPressure(getSensorValue(settings.pressureSensor, "illuminance")),
        windDirection: getSensorValue(settings.windDirectionSensor, "illuminance")
    ]
}

def convertWindSpeed(BigDecimal value) {
    if (!value) return null
    // Hubitat typically uses illuminance attr for custom numerics
    // Add unit conversion if your sensors report differently
    return value  // Assume m/s
}

def convertPressure(BigDecimal value) {
    if (!value) return null
    // Convert hPa -> kPa if needed: value * 0.1
    return value * 0.1G  // Common conversion
}

BigDecimal convertToDisplayUnit(BigDecimal celsius) {
    switch (settings.displayUnit ?: "C") {
        case "F": return (celsius * 9/5) + 32
        default: return celsius
    }
}

String displayUnit() {
    return (settings.displayUnit ?: "C") == "F" ? "°F" : "°C"
}

// Enhanced child device updates (rich attributes):
def updateChildDevice(BigDecimal value, result) {
    def child = getChildDevice(state.childDeviceId)
    if (!child) return
    
    def consts = WeatherSenseConst
    
    child.sendEvent(name: "temperature", value: value.round(1))
    
    // Comfort attributes
    child.sendEvent(name: "comfortLevel", value: result.comfortLevel)
    child.sendEvent(name: "comfortDescription", value: consts.COMFORT_DESCRIPTIONS[result.comfortLevel])
    child.sendEvent(name: "comfortExplanation", value: consts.COMFORT_EXPLANATIONS[result.comfortLevel])
    child.sendEvent(name: "calculationMethod", value: result.method)
    
    // Input values as attributes
    child.sendEvent(name: "inputTemperature", value: state.rawInputs.temperature?.round(1))
    child.sendEvent(name: "inputHumidity", value: state.rawInputs.humidity?.round(1))
    child.sendEvent(name: "inputWindSpeed", value: state.rawInputs.windSpeed?.round(1))
    child.sendEvent(name: "inputPressure", value: state.rawInputs.pressure?.round(1))
    
    // Comfort status
    boolean isComfortable = result.comfortLevel in [consts.COMFORT_COMFORTABLE, 
                                                   consts.COMFORT_SLIGHTLY_WARM, 
                                                   consts.COMFORT_SLIGHTLY_COOL]
    child.sendEvent(name: "isComfortable", value: isComfortable)
    
    // Dynamic icon
    String icon = consts.COMFORT_ICONS[result.comfortLevel] ?: "mdi:thermometer"
    child.updateSetting("icon", [value: icon])  // If virtual device supports
    
    // Wind correction
    if (result.windDirCorrection) {
        child.sendEvent(name: "windDirCorrection", value: result.windDirCorrection.round(2))
    }
}

// Create ENHANCED Virtual Device (separate DTH):
// Save as "WeatherSense Virtual Sensor.groovy":

definition(
    name: "WeatherSense Virtual Sensor",
    namespace: "weathersense",
    author: "SMKRV",
    description: "Rich feels-like sensor"
) {
    capability "Temperature Measurement"
    capability "Refresh"
    attribute "comfortLevel", "string"
    attribute "comfortDescription", "string" 
    attribute "comfortExplanation", "string"
    attribute "calculationMethod", "string"
    attribute "isComfortable", "boolean"
    attribute "inputTemperature", "number"
    attribute "inputHumidity", "number"
    attribute "inputWindSpeed", "number"
    attribute "inputPressure", "number"
    attribute "windDirCorrection", "number"
    command "refresh"
}

def refresh() {
    parent?.calculateAndUpdate()
}
