import React from 'react';
import './App.css'
import useDeviceMonitor from './hooks/useDeviceMonitor';
import { EnergyCalculationType, HeatingCalculationTypes } from './constants/calculationTypes';
import Timer from './components/Timer';
import DeviceTable from './components/DeviceTable';

function App() {
  const monitor = useDeviceMonitor(); 
  const energyDataKeys = Object.keys(EnergyCalculationType); 
  const heatingDataKeys = Object.keys(HeatingCalculationTypes);


  return (
    <div className="app-container">
      <h1 className="title">Task Control Dashboard</h1>

      {/* Energy Device Selection */}
      <div>
        <label>Select Device:</label>
        <select value={monitor.selectedDevice || ""} onChange={monitor.handleDeviceChange}>
          <option value="">---Select a Device----</option>
          {monitor.devices.length > 0 ? (
            monitor.devices.map((device) => (
              <option key={device.id} value={device.id}>
                {device.testStationName}
              </option>
            ))
          ) : (
            <option value="">No Devices Available</option>
          )}
        </select>
      </div>

      <button className="start" onClick={monitor.handleStartTask}>Start</button>
      <button className="stop" onClick={monitor.handleStopTask}>Stop</button>
      <Timer isRunning={monitor.isRunning} key={monitor.selectedDevice} />


      {/* Energy Table */}  
        <DeviceTable
          title="Energy Data"
          dataType="Energy"
          dataKeys={energyDataKeys}
          firstValues={monitor.firstValues}
          currentValues={monitor.currentValues}
          lastValues={monitor.lastValues}
          difference={{
            [EnergyCalculationType.ERZEUGTE_ENERGIE]: monitor.difference[EnergyCalculationType.ERZEUGTE_ENERGIE],
            [EnergyCalculationType.GENUTZTE_ENERGIE]: monitor.difference[EnergyCalculationType.GENUTZTE_ENERGIE],
          }}
     
    
        />

<DeviceTable
          title="Heating Data"
          dataType="Heating"
          dataKeys={heatingDataKeys}
          firstValues={monitor.firstValues}
          currentValues={monitor.currentValues}
          lastValues={monitor.lastValues}
          difference={{
            [HeatingCalculationTypes.ERZEUGTE_ENERGIE_HEATING]: monitor.difference[HeatingCalculationTypes.ERZEUGTE_ENERGIE_HEATING],
            }}
        />      
      </div>
  )
}

export default App
