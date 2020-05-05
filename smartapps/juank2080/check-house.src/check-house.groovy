/**
 *  Check House
 *
 *  Copyright 2020 Juan Romero
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Check House",
    namespace: "juank2080",
    author: "Juan Romero",
    description: "Check house lights and doors to be closed",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "pageOne", nextPage: "pageTwo", uninstall: true)
	page(name: "pageTwo", nextPage: "pageThree", uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks-active.png",
                      title: "Locks",
                      required: true,
                      "Configuration routines and checks for house locks"
		}
        section("Front door check:") {
            input "frontDoorSensor", "capability.contactSensor", title: "Front door sensor?", required: false, multiple: false
            input "frontDoorLock", "capability.lock", title: "Front door lock?", required: false, multiple: false
            input "checkFrontDoorInterval", "number", title: "Check door each: (min)", required: false, multiple: false
            input "closeFrontDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: false, multiple: false
        }
        section("Side door check:") {
            input "sideDoorSensor", "capability.contactSensor", title: "Side door sensor?", required: false, multiple: false
            input "sideDoorLock", "capability.lock", title: "Side door lock?", required: false, multiple: false
            input "checkSideDoorInterval", "number", title: "Check door each: (min)", required: false, multiple: false
            input "closeSideDoorDelayAfterSensorClosed", "number", title: "After sensor closes, lock door after: (sec)", required: false, multiple: false
        }
    }
    page(name: "pageThree", , nextPage: "pageFour", uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png",
                      title: "Go Bed Routines",
                      required: true,
                      "Routines for go bed"
		}
        section("Lights:") {
            input "lightsIndoorRef", "capability.light", multiple: true, title: "Indoor Lights as Reference"
            input "lightsOutdoorRef", "capability.light", multiple: true, title: "Outside Lights to check"
            input "timeOfDayLights", "time", title: "Time?"
            input "checkInterval", "number", title: "Check in between (min)", required: true, multiple: false
            input "maxAttempts", "number", title: "Max Attempts", required: true, multiple: false
        }
        section("Locks:") {
            input "doorLocks", "capability.lock", title: "Locks to check?", required: true, multiple: true
            input "timeOfDayLocks", "time", title: "Time?"
        }
	}
    page(name: "pageFour", install: true, uninstall: true) {
    	section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
                      title: "Temperature",
                      required: true,
                      "Routines for temperature"
		}
        section("First Floor:") {
            //input "thermostatModeFirstFloor", "capability.thermostatMode", multiple: true, title: "Thermostat Mode"
            //input "thermostatHSPFirstFloor", "capability.thermostatHeatingSetpoint", multiple: true, title: "Thermostat Heating Set Point"
            //input "thermostatCSPFirstFloor", "capability.thermostatCoolingSetpoint", multiple: true, title: "Thermostat Cooling Set Point"
            input "temperatureSensorsFirstFloor", "capability.temperatureMeasurement", multiple: true, title: "Temperature Sensors"
        }
        section("Second Floor:") {
            //input "thermostatModeSecondFloor", "capability.thermostatMode", multiple: true, title: "Thermostat Mode"
            //input "thermostatHSPSecondFloor", "capability.thermostatHeatingSetpoint", multiple: true, title: "Thermostat Heating Set Point"
            //input "thermostatCSPSecondFloor", "capability.thermostatCoolingSetpoint", multiple: true, title: "Thermostat Cooling Set Point"
            input "temperatureSensorsSecondFloor", "capability.temperatureMeasurement", multiple: true, title: "Temperature Sensors"
        }
	}
}

def pageOne() {
    dynamicPage(name: "pageOne", nextPage: "pageTwo", uninstall: true) {
        section() {
            paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                      title: "Runtime Information",
                      required: true,
                      "Shows some statistics of the app."
		}
        section("Outside lights:") {
        	paragraph "Outside lights on: ${lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }? "Yes" : "No"}"
			paragraph "Outside lights attempt: ${state?.lightAttempt != null? state.lightAttempt : "NA"}"
            paragraph "Outside lights shut at: ${state?.outsideLightsShutAt != null? formatTime(state.outsideLightsShutAt) : "NA"}"
            paragraph "Outside lights check status: ${state?.outsideLightsCheckStatus != null? state.outsideLightsCheckStatus : "NA"}"
        }
        section("Front door:") {
            paragraph "Front door closed by: ${state?.frontDoorClosedBy != null? state.frontDoorClosedBy : "NA"}"
            paragraph "Front door closed by at: ${state?.frontDoorClosedByAt != null? formatTime(state.frontDoorClosedByAt) : "NA"}"
            paragraph "Front door check status: ${state?.frontDoorCheckStatus != null? state?.frontDoorCheckStatus : "NA"}"
        }
        section("Side door:") {
            paragraph "Side door closed by: ${state?.sideDoorClosedBy != null? state.sideDoorClosedBy : "NA"}"
            paragraph "Side door closed by at: ${state?.sideDoorClosedByAt != null? formatTime(state.sideDoorClosedByAt) : "NA"}"
            paragraph "Side door check status: ${state?.sideDoorCheckStatus != null? state?.sideDoorCheckStatus : "NA"}"
		}
        section("Temperature:") {
        	paragraph "First floor: ${state?.tempInfoFirstFloor != null? state?.tempInfoFirstFloor : "NA"}"
            paragraph "Second floor: ${state?.tempInfoSecondFloor != null? state?.tempInfoSecondFloor : "NA"}"
        }
        section("General:") {
        	paragraph "Go bed locks routine: ${state?.goBedLocksStatus != null? state?.goBedLocksStatus : "NA"}"
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()

	initialize()
}

def initialize() {
	log.debug "initialize"
    
    // Init counters
    state.lightAttempt = 0
    
    state.sideDoorClosedBy = "-"
    state.sideDoorClosedByAt = null
    state.sideDoorCheckStatus = "-"
    
    state.frontDoorClosedBy = "-"
    state.frontDoorClosedByAt = null
    state.frontDoorCheckStatus = "-"
    
    state.outsideLightsShutAt = null
    state.outsideLightsCheckStatus = "-"
    
    state.goBedLocksStatus = null
    
    state.tempInfoFirstFloor = null
    state.tempInfoSecondFloor = null
  
	setUpLockRoutineById(frontDoorLock?.id)
    setUpLockRoutineById(sideDoorLock?.id)
    
    // Creating schedulers using timezone 
	def start = timeToday(timeOfDayLights, location?.timeZone)
	schedule(start, checkLights);
    
    start = timeToday(timeOfDayLocks, location?.timeZone)
	schedule(start, checkLocks);
    
    runEvery30Minutes(checkTemperature)
}

/**
* START EVENT HANDLERS
*/ 
// Side door
def onSensorClose(evt){
	def method = "onSensorClose"
    
    if (evt.device != null) {
    	def relatedElements = getDataRelatedBySensorId(evt.device.id)
        
        log.debug "[${method}] checking in ${relatedElements.interval} min"
        log.debug "[${method}] ${relatedElements.lock.label} status in ${relatedElements.lock.currentLock}"

        if (relatedElements.lock.currentLock != "locked") {
            log.debug "[${method}] Removing scheduler ${relatedElements.callbackFunction}"
            log.debug "[${method}] Closing door in ${relatedElements.closeDelay} sec"

            unschedule(relatedElements.callbackFunction)

            writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                       "closedByAt":null,
                                                       "checkStatus":"${relatedElements.sensor.label} closed, door will be closed in ${relatedElements.closeDelay} sec"])

            runIn(relatedElements.closeDelay, closeDoor, [overwrite: false, data: [deviceObj: relatedElements.lock, closedBy: "[${method}]"]])
        }
    } else {
    	log.debug "[${method}] Device obj from Evnt is null"
    }
}

def onLockUnlock(evt) {
	def method = "onUnlock"
    
    if (evt.device != null) {
    	def relatedElements = getDataRelatedByLockId(evt.device.id)
    
    	if (relatedElements != null) {
            log.debug "[${method}] checking in ${relatedElements.interval} min"

			writeLogByLockId(evt.device.id, ["closedBy":null,
        									"closedByAt":null,
                                            "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${evt.device.label} has been opened, checking sensor in ${relatedElements.interval} min"])
   
            runIn(60 * relatedElements.interval, relatedElements.callbackFunction, [overwrite: false, data: [deviceObj: relatedElements.sensor]]);
     	} else {
        	log.debug "[${method}] Couldn't get related elements from ${evt.device.label}"
        }
  	} else {
    	log.debug "[${method}] Device obj from Evnt is null"
    }
}

def onLockLock(evt){
	def method = "onLock"
    if (evt.device != null) {
    	def relatedElements = getDataRelatedByLockId(evt.device.id)
        
        if (relatedElements != null) {
            log.debug "[${method}] ${evt.device.label} sensor status ${relatedElements.sensor.currentContact}"

            if (relatedElements.sensor.currentContact == "closed") {
                log.debug "[${method}] ${evt.device.label} is closed, removing ${relatedElements.callbackFunction}"
                
                unschedule(relatedElements.callbackFunction)
            }
        } else {
        	log.debug "[${method}] Couldn't get related elements from ${evt.device.label}"
        }
    } else {
    	 log.debug "[${method}] Device obj from Evnt is null"
    }
}
/*** END EVENT HANDLERS ***/

/**
* DOOR METHODS
*/
def closeDoor(data){
	def method = "closeDoor"
    
	if (data.deviceObj != null) {
        def relatedElements = getDataRelatedByLockId(data.deviceObj.id)
        
        if (relatedElements.sensor.currentContact != "closed") {
            writeLogByLockId(data.deviceObj.id, ["closedBy":"[${data.closedBy}]",
                                                "closedByAt":now(),
                                                "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Closing ${relatedElements.lock.label} now"])

            lockObj.lock()

            runIn(5, checkIfDoorClosed, [overwrite: false, data: [tryCount: 1, deviceObj: relatedElements.lock, closedBy: data.closedBy != null? data.closedBy : "[${method}]"]]);
      	}
  	} else {
    	log.debug "[${method}] Device obj from data is null"
    }
}

def checkIfDoorClosed(data) {
	def method = "checkIfDoorClosed"
    
    if (data.deviceObj != null) {
        def relatedElements = getDataRelatedByLockId(data.deviceObj.id)
        
        if (relatedElements.sensor.currentContact != "closed") {
        	def lockObj = relatedElements.lock
            
            if (data.tryCount <= 3) {
                if (lockObj.currentLock != "locked") {
                    writeLogByLockId(data.deviceObj.id, ["closedBy":"[${method}]",
                                                        "closedByAt":now(),
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} didn't close, try again in 5s (${data.tryCount})"])

                    lockObj.lock()

                    runIn(5, checkIfDoorClosed, [overwrite: false, data: [tryCount: data.tryCount + 1, deviceObj: lockObj]]);
                } else {
                    writeLogByLockId(data.deviceObj.id, ["closedBy":data.tryCount == 1 && data.closedBy != null? data.closedBy : "[${method}]",
                                                        "closedByAt":now(),
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} has been closed (${data.tryCount})"])
                }
            } else {
                def message = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${lockObj.label} didn't lock (!)"

                writeLogByLockId(data.deviceObj.id, ["closedBy":"[${method}]",
                                                        "closedByAt":now(),
                                                        "checkStatus":message])

                sendPush(message)
            }
     	}
  	} else {
    	log.debug "[${method}] Device obj from data is null"
    }
}

//Wrappers
def checkSideDoorSensor(data) { checkDoorSensor(data) }
def checkFrontDoorSensor(data) { checkDoorSensor(data) }

def checkDoorSensor(data) {
	if (data?.deviceObj != null) {
        def method = "checkDoorSensor"
        
        log.debug "[${method}] ${data.deviceObj}"
        
        def relatedElements = getDataRelatedBySensorId(data.deviceObj.id)
        
        log.debug "[${method}] The current value of ${relatedElements.lock.label} is ${relatedElements.sensor.currentContact}"
 
        if (relatedElements.sensor.currentContact == "closed") {
            log.debug "[${method}] Locking ${relatedElements.lock.label}"

            data.closedBy = "[${method}]"
			data.deviceObj = relatedElements.lock

            closeDoor(data)
        } else if (relatedElements.lock.currentLock != "locked") {
            log.debug "[${method}] Executing ${method} in ${relatedElements.interval} min"

			writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                        "closedByAt":null,
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${relatedElements.sensor.label} is still opened, checking sensor in ${relatedElements.interval} min"])
       
            runIn(60 * relatedElements.interval, relatedElements.callbackFunction, [overwrite: false, data: [deviceObj: relatedElements.sensor, closedBy: "[${method}]"]]);
        } else {
        	writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                        "closedByAt":now(),
                                                        "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${relatedElements.lock} Door is closed"])
        }
 	} else {
    	log.debug "[${method}] deviceObj is null (?)"
    }
}

// Go bed procedure - Locks
def checkLocks(){
	def method = "checkLocks"
	log.debug "[${method}] Locking doors..."
    
    if (doorLocks?.any { lock -> lock.currentLock != "locked" }) {
    	doorLocks.findAll { lock -> lock.currentLock != "locked" }?.each { lock -> 
                                def relatedElements = getDataRelatedByLockId(lock.id)

                                if (relatedElements.sensor.currentContact == "closed") {
                                    lock.lock()
                                }
                   		  }
                          
    	state.goBedLocksStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Doors were closed (${doorLocks.size()})"
    } else {
    	state.goBedLocksStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] All doors were already closed (${doorLocks.size()})"
    }
    
    sendEvent(name: "status", value: "updated")
}
/**
* END DOOR METHODS
*/

/**
* LIGHTS METHODS
*/
def checkLights() {
	def method = "checkLights"
	log.debug "[${method}] Checking ..."
    
	if (lightsOutdoorRef?.any { light -> light.currentSwitch == "on" }) {
    	log.debug "[${method}] Found at least one outdoor light on"
        if (state?.lightAttempt == null) {
        	state.lightAttempt = 0
      	}
            
        if (lightsIndoorRef?.any { light -> light.currentSwitch == "on" }) {
        	if (state.lightAttempt < maxAttempts) {
                state.lightAttempt = state.lightAttempt + 1
                log.debug "[${method}] It is at least one light indoor on, check again in ${checkInterval}min, attempt: ${state.lightAttempt}"

                state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Indoor lights still on, cheking in ${checkInterval} min"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")

                runIn(60 * checkInterval, checkLights, [overwrite: false]);
        	} else {
            	log.debug "[${method}] Max attempts reached, indoor lights still on, no more attempts will be done, outdoor lights remain on"
            
            	state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Indoor lights still on, no more attempts will be done, outdoor lights remain on"
                state.outsideLightsShutAt = null
                sendEvent(name: "status", value: "updated")
            }
      	} else {
        	state.lightAttempt = 0
            log.debug "[${method}] Any indoor light is on, turning lights off"
            
    		state.outsideLightsCheckStatus = "[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] Outdoor lights off"
            state.outsideLightsShutAt = now()
            sendEvent(name: "status", value: "updated")
            
            lightsOutdoorRef.findAll { light -> light.currentSwitch == "on" }?.each { light -> light.off() }
        }
        
    } else {
    	log.debug "[${method}] No outside lights on - stop checking"
        state.lightAttempt = 0
        
        state.outsideLightsShut = "Outside lights are off"
    	state.outsideLightsShutAt = now()
        sendEvent(name: "status", value: "updated")
    }
}
/**
* END LIGHTS METHODS
*/

/**
* TEMPERATURE METHODS
*/
def checkTemperature() {
	def method = "checkTemperature"
    
	state.tempInfoFirstFloor = ""
    state.tempInfoSecondFloor = ""
    
    temperatureSensorsFirstFloor.each{ state.tempInfoFirstFloor = "${state.tempInfoFirstFloor} [${it.label} ${it.currentTemperature}] " }
    temperatureSensorsSecondFloor.each{ state.tempInfoSecondFloor = "${state.tempInfoSecondFloor} [${it.label} ${it.currentTemperature}] " }
    
    log.debug "[${method}] ${state.tempInfoFirstFloor}"
    log.debug "[${method}] ${state.tempInfoSecondFloor}"
    
    sendEvent(name: "status", value: "updated")
}
/**
* END TEMPERATURE METHODS
*/

def formatTime(java.lang.Long time) {
	return new Date(time).format("MM/dd hh:mm a", location.timeZone);
}

def setUpLockRoutineById(lockId) {
	def method = "setUpLockRoutineById"
	log.debug "[${method}] setting up lock ..."
    
    if (lockId != null) {
    	def relatedElements = getDataRelatedByLockId(lockId)
        
        if (relatedElements.sensor != null) {
            subscribe(relatedElements.lock, "lock.locked", onLockLock)
            subscribe(relatedElements.lock, "lock.unlocked", onLockUnlock)
            subscribe(relatedElements.sensor, "contact.closed", onSensorClose)

            // Check if door is currently "unlocked"
            log.debug "Current values: ${relatedElements.lock.label} (${relatedElements.lock.currentLock}), ${relatedElements.sensor.label}: (${relatedElements.sensor.currentContact})"
            if (relatedElements.lock.currentLock != "locked" && relatedElements.sensor.currentContact == "closed") {
                log.debug "Setting timer to check ${relatedElements.lock.label} again in ${relatedElements.interval} min"

				writeLogByLockId(relatedElements.lock.id, ["closedBy":null,
                                                           "closedByAt":null,
                                                           "checkStatus":"[${new Date(now()).format("MM/dd hh:mm a", location.timeZone)}] ${relatedElements.lock.label} is currently opened, checking sensor in ${relatedElements.interval} min"])

                runIn(60 * relatedElements.interval, relatedElements.callbackFunction, [overwrite: false, data: [deviceObj: relatedElements.sensor]]);
            }
     	} else {
        	log.debug "[${method}] No sensor configured for ${relatedElements.lock.label} ..."
        }
  	}
}

def findDeviceById(id) {
	def myAppDevices = [];
	for (setting in settings) {
    	try {
        	def deviceId = setting.value.id
        	myAppDevices = myAppDevices + setting.value
        } catch (Exception e) {}
	}
    
    return myAppDevices.find{ it.id == id }
}

def getDataRelatedByLockId(lockId) {
	def relatedElements = null
    
	if (frontDoorLock.id == lockId) {
    	relatedElements = ["lock":frontDoorLock,,
        					"sensor": frontDoorSensor,
        					"interval": checkFrontDoorInterval,
                            "closeDelay": closeFrontDoorDelayAfterSensorClosed,
                            "callbackFunction": checkFrontDoorSensor];
    } else if (sideDoorLock.id == lockId) {
    	relatedElements = ["lock":sideDoorLock,
        					"sensor":sideDoorSensor,
        					"interval":checkSideDoorInterval,
                            "closeDelay":closeSideDoorDelayAfterSensorClosed,
                            "callbackFunction":checkSideDoorSensor];
    }
    
    return relatedElements
}

def getDataRelatedBySensorId(sensorId) {
	def relatedElements = null
    
	if (frontDoorSensor.id == sensorId) {
    	relatedElements = ["lock":frontDoorLock,
        					"sensor": frontDoorSensor,
        					"interval":checkFrontDoorInterval,
                            "closeDelay":closeFrontDoorDelayAfterSensorClosed,
                            "callbackFunction": checkFrontDoorSensor];
    } else if (sideDoorSensor.id == sensorId) {
    	relatedElements = ["lock":sideDoorLock,
        					"sensor":sideDoorSensor,
        					"interval":checkSideDoorInterval,
                            "closeDelay":closeSideDoorDelayAfterSensorClosed,
                            "callbackFunction":checkSideDoorSensor];
    }
    
    return relatedElements
}

def writeLogByLockId(lockId, logData) {
	if (frontDoorLock.id == lockId) {
    	state.frontDoorClosedBy = logData.closedBy
       	state.frontDoorClosedByAt = logData.closedByAt
      	state.frontDoorCheckStatus = logData.checkStatus
    } else if (sideDoorLock.id == lockId) {
    	state.sideDoorClosedBy = logData.closedBy
       	state.sideDoorClosedByAt = logData.closedByAt
      	state.sideDoorCheckStatus = logData.checkStatus
    }
    
    sendEvent(name: "status", value: "updated")
}