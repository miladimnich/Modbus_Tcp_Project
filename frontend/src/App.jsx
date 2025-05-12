import React from "react";
import "./App.css";
import "react-toastify/dist/ReactToastify.css";
import { ToastContainer } from "react-toastify";
import { useDeviceMonitor } from "./hooks/useDeviceMonitor";
import {
  EnergyCalculationType,
  HeatingCalculationTypes,
  GasTypes,
  BhkwTypes,
} from "./constants/calculationTypes";
import Timer from "./components/Timer";
import DeviceTable from "./components/DeviceTable";
import Clock from "./components/Clock";

function App() {
  const monitor = useDeviceMonitor();
  const energyDataKeys = Object.keys(EnergyCalculationType);
  const heatingDataKeys = Object.keys(HeatingCalculationTypes);
  const gasDataKeys = Object.keys(GasTypes);
  const bhkwDataKeys = Object.keys(BhkwTypes);

  return (
    <div className="title-container">
      <h1 className="title">Task Control Dashboard</h1>

      <div className="top-section">
        <div className="device-selector field-group">
          <label>Select Device:</label>
          <select
            value={monitor.selectedDevice || ""}
            onChange={monitor.handleDeviceChange}
            disabled={monitor.selectedDevice}
          >
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

        <div className="machine-type-selector field-group">
          <label>Select Machine Type:</label>
          <select
            value={monitor.selectedMachineType}
            onChange={monitor.handleMachineTypeChange}
            disabled={
              monitor.isRunning || !monitor.selectedDevice || monitor.isStopped
            }
          >
            <option value="">---Select Machine Type----</option>

            {monitor.machineTypes && monitor.machineTypes.length > 0 ? (
              monitor.machineTypes.map((machineType) => (
                <option key={machineType} value={machineType}>
                  {machineType}
                </option>
              ))
            ) : (
              <option value="">No Machine Types Available</option>
            )}
          </select>
        </div>

        <div className="text-type-selector field-group">
          <label>Enter Seriennummer :</label>
          <input
            type="text"
            value={monitor.serienNummer}
            onChange={monitor.handleInputChange}
            placeholder="Enter Seriennummer"
            disabled={monitor.isButtonDisabled || monitor.isRunning}
          />
          <button
            className="checkStatus"
            onClick={monitor.fetchStatus}
            disabled={
              monitor.isButtonDisabled ||
              monitor.isRunning ||
              !monitor.serienNummer
            }
          >
            Check Status
          </button>
        </div>

        <div className="button-group">
          <button
            className="start"
            onClick={monitor.handleStartTask}
            disabled={
              monitor.isRunning ||
              monitor.isStopped ||
              !monitor.isButtonDisabled
            }
          >
            Start
          </button>

          <button
            className="stop"
            onClick={monitor.handleStopTask}
            disabled={!monitor.isRunning}
          >
            Stop
          </button>

          <button
            className="reset"
            onClick={monitor.handleReset}
            disabled={monitor.isRunning} // Disable reset if task is running
          >
            Reset
          </button>

          <button
            className="ubertragen"
            onClick={monitor.handleUbertragen}
            disabled={monitor.isRunning || !monitor.isStopped}
          >
            Übertragen
          </button>
        </div>

        <div className="clock-container">
          <Clock />
        </div>

        <div className="timer-container">
          <Timer elapsedTime={monitor.elapsedTime} />
        </div>
      </div>

      <div className="container">
        {/* Energy Table */}
        <div className="table-energy">
          <DeviceTable
            title="Energy Data"
            dataType="Energy"
            dataKeys={energyDataKeys}
            firstValues={monitor.firstValues}
            currentValues={monitor.currentValues}
            lastValues={monitor.lastValues}
            difference={monitor.difference}
            mergedBorders={{
              ...monitor.defaultBorders,
              ...monitor.machineTypeBorders,
            }}
            outOfRangeState={monitor.outOfRangeState}
          />
        </div>

        {/* Gas Table */}
        <div className="table-gas">
          <DeviceTable
            title="Gas Data"
            dataType="Gas"
            dataKeys={gasDataKeys}
            firstValues={monitor.firstValues}
            currentValues={monitor.currentValues}
            lastValues={monitor.lastValues}
            difference={monitor.gasMeterDifference}
            mergedBorders={{
              ...monitor.defaultBorders,
              ...monitor.machineTypeBorders,
            }}
            outOfRangeState={monitor.outOfRangeState}
          />
        </div>

        {/* Heating Table Outside Flex Layout */}
        <div className="table-heating">
          <DeviceTable
            title="Heating Data"
            dataType="Heating"
            dataKeys={heatingDataKeys}
            firstValues={monitor.firstValues}
            currentValues={monitor.currentValues}
            lastValues={monitor.lastValues}
            difference={monitor.difference}
            mergedBorders={{
              ...monitor.defaultBorders,
              ...monitor.machineTypeBorders,
            }}
            outOfRangeState={monitor.outOfRangeState}
          />
        </div>

        <div className="bhkw-table">
          <DeviceTable
            title="BHKW Data"
            dataType="BHKW"
            dataKeys={bhkwDataKeys}
            firstValues={monitor.firstValues}
            currentValues={monitor.currentValues}
            lastValues={monitor.lastValues}
            mergedBorders={{
              ...monitor.defaultBorders,
              ...monitor.machineTypeBorders,
            }}
            outOfRangeState={monitor.outOfRangeState}
          />
        </div>
      </div>
      <ToastContainer />
    </div>
  );
}

export default App;
