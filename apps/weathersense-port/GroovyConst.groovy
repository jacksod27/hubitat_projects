/*
 Constants for the WeatherSense integration (Hubitat / Groovy version).

 Original: ha-weathersense (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

class WeatherSenseConst {

    // Domain / name
    static final String DOMAIN = "weathersense"
    static final String NAME   = "WeatherSense"

    // Configuration options
    static final String CONF_TEMPERATURE_SENSOR        = "temperature_sensor"
    static final String CONF_HUMIDITY_SENSOR           = "humidity_sensor"
    static final String CONF_WIND_SPEED_SENSOR         = "wind_speed_sensor"
    static final String CONF_PRESSURE_SENSOR           = "pressure_sensor"
    static final String CONF_IS_OUTDOOR                = "is_outdoor"
    static final String CONF_SOLAR_RADIATION_SENSOR    = "solar_radiation_sensor"
    static final String CONF_TIME_OF_DAY               = "time_of_day"
    static final String CONF_DISPLAY_UNIT              = "display_unit"
    static final String CONF_WIND_DIRECTION_SENSOR     = "wind_direction_sensor"
    static final String CONF_WIND_DIRECTION_CORRECTION = "wind_direction_correction"
    static final String CONF_SMOOTHING_ENABLED         = "smoothing_enabled"
    static final String CONF_SMOOTHING_FACTOR          = "smoothing_factor"

    // Default values
    static final String DEFAULT_NAME             = "Feels Like Temperature"
    static final Boolean DEFAULT_IS_OUTDOOR      = true
    static final BigDecimal DEFAULT_SMOOTHING_FACTOR = 0.3G

    // Comfort levels (short form)
    static final String COMFORT_EXTREME_COLD = "extreme_cold"
    static final String COMFORT_VERY_COLD    = "very_cold"
    static final String COMFORT_COLD         = "cold"
    static final String COMFORT_COOL         = "cool"
    static final String COMFORT_SLIGHTLY_COOL = "slightly_cool"
    static final String COMFORT_COMFORTABLE   = "comfortable"
    static final String COMFORT_SLIGHTLY_WARM = "slightly_warm"
    static final String COMFORT_WARM          = "warm"
    static final String COMFORT_HOT           = "hot"
    static final String COMFORT_VERY_HOT      = "very_hot"
    static final String COMFORT_EXTREME_HOT   = "extreme_hot"

    // Comfort level descriptions
    static final Map<String,String> COMFORT_DESCRIPTIONS = [
        (COMFORT_EXTREME_COLD): "Extreme Cold Stress",
        (COMFORT_VERY_COLD)   : "Very Strong Cold Stress",
        (COMFORT_COLD)        : "Strong Cold Stress",
        (COMFORT_COOL)        : "Moderate Cold Stress",
        (COMFORT_SLIGHTLY_COOL): "Slight Cold Stress",
        (COMFORT_COMFORTABLE) : "No Thermal Stress (Comfort)",
        (COMFORT_SLIGHTLY_WARM): "Slight Heat Stress",
        (COMFORT_WARM)        : "Moderate Heat Stress",
        (COMFORT_HOT)         : "Strong Heat Stress",
        (COMFORT_VERY_HOT)    : "Very Strong Heat Stress",
        (COMFORT_EXTREME_HOT) : "Extreme Heat Stress"
    ]

    // Comfort level detailed explanations
    static final Map<String,String> COMFORT_EXPLANATIONS = [
        (COMFORT_EXTREME_COLD): "Extreme risk: frostbite possible in less than 5 minutes",
        (COMFORT_VERY_COLD)   : "High risk: frostbite possible in 5-10 minutes",
        (COMFORT_COLD)        : "Warning: frostbite possible in 10-30 minutes",
        (COMFORT_COOL)        : "Caution: prolonged exposure may cause discomfort",
        (COMFORT_SLIGHTLY_COOL): "Slightly cool: light discomfort for sensitive individuals",
        (COMFORT_COMFORTABLE) : "Optimal thermal conditions: most people feel comfortable",
        (COMFORT_SLIGHTLY_WARM): "Slightly warm: light discomfort for sensitive individuals",
        (COMFORT_WARM)        : "Caution: fatigue possible with prolonged exposure",
        (COMFORT_HOT)         : "Extreme caution: heat exhaustion possible",
        (COMFORT_VERY_HOT)    : "Danger: heat cramps and exhaustion likely",
        (COMFORT_EXTREME_HOT) : "Extreme danger: heat stroke imminent"
    ]

    // Comfort icons
    static final Map<String,String> COMFORT_ICONS = [
        (COMFORT_EXTREME_COLD):  "mdi:snowflake-alert",
        (COMFORT_VERY_COLD)   :  "mdi:snowflake-thermometer",
        (COMFORT_COLD)        :  "mdi:thermometer-low",
        (COMFORT_COOL)        :  "mdi:thermometer-minus",
        (COMFORT_SLIGHTLY_COOL): "mdi:thermometer-minus",
        (COMFORT_COMFORTABLE) :  "mdi:hand-okay",
        (COMFORT_SLIGHTLY_WARM): "mdi:thermometer-plus",
        (COMFORT_WARM)        :  "mdi:thermometer-high",
        (COMFORT_HOT)         :  "mdi:thermometer-alert",
        (COMFORT_VERY_HOT)    :  "mdi:heat-wave",
        (COMFORT_EXTREME_HOT) :  "mdi:fire-alert"
    ]

    // Sensor attributes
    static final String ATTR_COMFORT_LEVEL              = "comfort_level"
    static final String ATTR_COMFORT_DESCRIPTION        = "comfort_description"
    static final String ATTR_COMFORT_EXPLANATION        = "comfort_explanation"
    static final String ATTR_CALCULATION_METHOD         = "calculation_method"
    static final String ATTR_TEMPERATURE                = "temperature"
    static final String ATTR_HUMIDITY                   = "humidity"
    static final String ATTR_WIND_SPEED                 = "wind_speed"
    static final String ATTR_PRESSURE                   = "pressure"
    static final String ATTR_IS_OUTDOOR                 = "is_outdoor"
    static final String ATTR_TIME_OF_DAY                = "time_of_day"
    static final String ATTR_IS_COMFORTABLE             = "is_comfortable"
    static final String ATTR_WIND_DIRECTION             = "wind_direction"
    static final String ATTR_WIND_DIRECTION_CORRECTION  = "wind_direction_correction_applied"
}
