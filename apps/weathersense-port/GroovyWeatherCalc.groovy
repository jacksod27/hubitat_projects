/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version).

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

import groovy.transform.Field

@Field static final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

class WeatherSenseCalculator {

    static BigDecimal calculateHeatIndex(BigDecimal temperature, BigDecimal humidity) {
        // Convert Celsius to Fahrenheit for calculation
        BigDecimal tF = (temperature * 9/5) + 32
        BigDecimal rh = humidity

        BigDecimal hiF
        if (rh < 40 || tF < 80) {
            hiF = 0.5 * (tF + 61.0 + ((tF - 68.0) * 1.2) + (rh * 0.094))
            hiF = (hiF + tF) / 2
        } else {
            // Full Rothfusz regression formula (works in Fahrenheit)
            hiF = -42.379G
            hiF += 2.04901523G * tF
            hiF += 10.14333127G * rh
            hiF -= 0.22475541G * tF * rh
            hiF -= 0.00683783G * tF * tF
            hiF -= 0.05481717G * rh * rh
            hiF += 0.00122874G * tF * tF * rh
            hiF += 0.00085282G * tF * rh * rh
            hiF -= 0.00000199G * tF * tF * rh * rh

            // Adjustments for extreme conditions (Fahrenheit values)
            if (rh < 13 && tF >= 80 && tF <= 112) {
                BigDecimal adjustment = ((13 - rh) / 4) * Math.sqrt((17 - Math.abs(tF - 95)) / 17)
                hiF -= adjustment
            } else if (rh > 85 && tF >= 80 && tF <= 87) {
                BigDecimal adjustment = ((rh - 85) / 10) * ((87 - tF) / 5)
                hiF += adjustment
            }
        }

        // Convert back to Celsius
        BigDecimal hiC = (hiF - 32) * 5/9

        // Sanity check - heat index should not be unreasonably high
        if (hiC > 70) {
            hiC = Math.min(hiC, temperature + 25)
        }

        return hiC
    }

    static BigDecimal calculateWindChill(BigDecimal temperature, BigDecimal windSpeed) {
        BigDecimal t = temperature
        // Convert m/s to km/h as the formula requires km/h
        BigDecimal vKmh = Math.max(windSpeed * 3.6, 4.828G)  // Minimum 4.828 km/h (≈1.34 m/s)

        BigDecimal wct = 13.12G
        wct += 0.6215G * t
        wct -= 11.37G * Math.pow(vKmh, 0.16G)
        wct += 0.3965G * t * Math.pow(vKmh, 0.16G)

        return wct
    }

    static BigDecimal calculateSteadmanApparentTemp(BigDecimal temperature, BigDecimal humidity, BigDecimal windSpeed) {
        BigDecimal t = temperature
        BigDecimal v = windSpeed

        // Calculate vapor pressure (e) in kPa
        BigDecimal e = (humidity / 100) * 6.105G * Math.exp((17.27G * t) / (237.7G + t))

        BigDecimal at = t + 0.33G * e - 0.70G * v - 4.00G
        return at
    }

    static BigDecimal applySolarCorrection(BigDecimal feelsLike, Date timeOfDay = null, BigDecimal cloudiness = 0G) {
        if (!timeOfDay) {
            timeOfDay = new Date()
        }

        Calendar cal = Calendar.instance
        cal.time = timeOfDay
        int hour = cal.get(Calendar.HOUR_OF_DAY)

        BigDecimal solarFactor = 0G

        // Daytime solar radiation effect (sunrise to sunset approximately)
        if (hour >= 6 && hour <= 18) {
            BigDecimal hoursFromSunrise = hour - 6
            BigDecimal solarIntensity = Math.sin(Math.toRadians(Math.PI * hoursFromSunrise / 12))

            BigDecimal maxSolarCorrection
            if (feelsLike >= 25) {
                maxSolarCorrection = 2.5G
            } else if (feelsLike >= 15) {
                maxSolarCorrection = 2.0G
            } else if (feelsLike >= 5) {
                maxSolarCorrection = 1.5G
            } else {
                maxSolarCorrection = 1.0G
            }

            BigDecimal cloudFactor = 1 - (cloudiness / 100)
            solarFactor = maxSolarCorrection * solarIntensity * cloudFactor
        }
        // Nighttime cooling effect (minor)
        else if (hour >= 22 || hour <= 4) {
            solarFactor = -0.5G
        }

        return feelsLike + solarFactor
    }

    static BigDecimal applyPressureCorrection(BigDecimal feelsLike, BigDecimal pressure = null) {
        if (!pressure || pressure <= 0) {
            return feelsLike
        }

        if (pressure < 80 || pressure > 110) {
            return feelsLike
        }

        BigDecimal correction = 0.1G * (STANDARD_PRESSURE - pressure)
        return feelsLike + correction
    }

    static Map applyWindDirectionCorrection(BigDecimal feelsLike, BigDecimal windDirection, BigDecimal latitude = null, BigDecimal maxCorrection = 1.0G) {
        if (!windDirection) {
            return [feelsLike: feelsLike, correction: 0G]
        }

        // Normalize wind direction to 0-360 range
        windDirection = windDirection % 360

        // Calculate north-south component using cosine
        BigDecimal northFactor = Math.cos(Math.toRadians(windDirection))

        // Determine hemisphere from latitude (default to Northern if not specified)
        boolean isNorthernHemisphere = !latitude || latitude >= 0

        BigDecimal correction
        if (isNorthernHemisphere) {
            correction = -northFactor * maxCorrection
        } else {
            correction = northFactor * maxCorrection
        }

        return [feelsLike: feelsLike + correction, correction: correction]
    }

    static Map calculateFeelsLike(
        BigDecimal temperature,
        BigDecimal humidity,
        BigDecimal windSpeed = 0G,
        BigDecimal pressure = null,
        boolean isOutdoor = true,
        Date timeOfDay = null,
        BigDecimal cloudiness = 0G,
        BigDecimal windDirection = null,
        BigDecimal latitude = null,
        boolean enableWindDirectionCorrection = false
    ) {
        String method = ""
        BigDecimal windDirCorrection = 0G

        BigDecimal feelsLike
        String comfortLevel

        if (isOutdoor) {
            // Outdoor calculation
            if (temperature >= 27 && humidity >= 40) {
                feelsLike = calculateHeatIndex(temperature, humidity)
                method = "Heat Index"
            } else if (temperature <= 10 && windSpeed > 1.34G) {
                feelsLike = calculateWindChill(temperature, windSpeed)
                method = "Wind Chill"
            } else {
                feelsLike = calculateSteadmanApparentTemp(temperature, humidity, windSpeed)
                method = "Steadman Apparent Temperature"
            }

            // Apply solar and pressure corrections
            BigDecimal originalFeelsLike = feelsLike
            feelsLike = applySolarCorrection(feelsLike, timeOfDay, cloudiness)
            if (pressure) {
                feelsLike = applyPressureCorrection(feelsLike, pressure)
            }

            // Apply experimental wind direction correction if enabled
            if (enableWindDirectionCorrection && windDirection) {
                def result = applyWindDirectionCorrection(feelsLike, windDirection, latitude)
                feelsLike = result.feelsLike
                windDirCorrection = result.correction
            }

            // Sanity check for unreasonable values
            if (Math.abs(feelsLike - originalFeelsLike) > 0.1G) {
                log.debug "Applied corrections: original=${originalFeelsLike}°C, after corrections=${feelsLike}°C"
            }

            if (feelsLike > temperature + 25 || feelsLike < temperature - 25) {
                log.warn "Calculated feels-like temperature (${feelsLike}°C) is far from actual temperature (${temperature}°C)"
            }

            // Determine comfort level for outdoor
            comfortLevel = determineOutdoorComfort(feelsLike, method)
        } else {
            // Indoor calculation - simplified approach
            feelsLike = calculateIndoorFeelsLike(temperature, humidity)
            method = "Indoor Comfort Model"
            comfortLevel = determineIndoorComfort(feelsLike, humidity)
        }

        return [
            feelsLike: feelsLike,
            method: method,
            comfortLevel: comfortLevel,
            windDirCorrection: windDirCorrection
        ]
    }

    static BigDecimal calculateIndoorFeelsLike(BigDecimal temperature, BigDecimal humidity) {
        BigDecimal humidityFactor = 0G
        if (humidity < 30) {
            humidityFactor = (humidity - 30) * 0.05G
        } else if (humidity > 60) {
            humidityFactor = (humidity - 60) * 0.05G
        }

        return temperature + humidityFactor
    }

    static String determineOutdoorComfort(BigDecimal feelsLike, String method) {
        // Reference WeatherSenseConst comfort levels
        def consts = WeatherSenseConst

        // Sanity check - if feels_like is unreasonable, use more conservative estimate
        if (feelsLike > 60) {
            log.warn "Calculated feels-like temperature is unreasonably high: ${feelsLike}°C. Capping at 50°C"
            feelsLike = Math.min(feelsLike, 50G)
        }

        switch (method) {
            case "Heat Index":
                if (feelsLike >= 54) return consts.COMFORT_EXTREME_HOT
                if (feelsLike >= 41) return consts.COMFORT_VERY_HOT
                if (feelsLike >= 32) return consts.COMFORT_HOT
                if (feelsLike >= 27) return consts.COMFORT_WARM
                return consts.COMFORT_COMFORTABLE

            case "Wind Chill":
                if (feelsLike <= -48) return consts.COMFORT_EXTREME_COLD
                if (feelsLike <= -40) return consts.COMFORT_VERY_COLD
                if (feelsLike <= -27) return consts.COMFORT_COLD
                if (feelsLike <= -13) return consts.COMFORT_COOL
                if (feelsLike <= 0) return consts.COMFORT_SLIGHTLY_COOL
                return consts.COMFORT_COMFORTABLE

            default:  // Steadman or other
                if (feelsLike > 46) return consts.COMFORT_EXTREME_HOT
                if (feelsLike > 38) return consts.COMFORT_VERY_HOT
                if (feelsLike > 32) return consts.COMFORT_HOT
                if (feelsLike > 29) return consts.COMFORT_WARM
                if (feelsLike > 26) return consts.COMFORT_SLIGHTLY_WARM
                if (feelsLike > 9) return consts.COMFORT_COMFORTABLE
                if (feelsLike > 0) return consts.COMFORT_SLIGHTLY_COOL
                if (feelsLike > -13) return consts.COMFORT_COOL
                if (feelsLike > -27) return consts.COMFORT_COLD
                if (feelsLike > -40) return consts.COMFORT_VERY_COLD
                return consts.COMFORT_EXTREME_COLD
        }
    }

    static String determineIndoorComfort(BigDecimal temperature, BigDecimal humidity) {
        def consts = WeatherSenseConst

        if (temperature < 16) return consts.COMFORT_COLD
        if (temperature < 18) return consts.COMFORT_COOL
        if (temperature < 20) return consts.COMFORT_SLIGHTLY_COOL

        if (temperature <= 24) {
            if (humidity < 30) return consts.COMFORT_SLIGHTLY_COOL
            if (humidity > 70) return consts.COMFORT_SLIGHTLY_WARM
            return consts.COMFORT_COMFORTABLE
        }
        if (temperature <= 26) return consts.COMFORT_SLIGHTLY_WARM
        if (temperature <= 28) return consts.COMFORT_WARM
        return consts.COMFORT_HOT
    }
}
