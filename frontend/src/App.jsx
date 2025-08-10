import "./App.css";
import "react-toastify/dist/ReactToastify.css";
import { ToastContainer } from "react-toastify";
import { useDeviceMonitor } from "./hooks/useDeviceMonitor"
import {
  EnergyCalculationType,
  HeatingCalculationTypes,
  GasTypes,
  ChpCalculationType,
} from "./constants/calculationTypes";
import Timer from "./components/Timer";
import DeviceTable from "./components/DeviceTable";
import Clock from "./components/Clock";




function App() {
  const monitor = useDeviceMonitor();
  const energyDataKeys = Object.keys(EnergyCalculationType);
  const heatingDataKeys = Object.keys(HeatingCalculationTypes);
  const gasDataKeys = Object.keys(GasTypes);
  const chpDataKeys = Object.keys(ChpCalculationType);

  return (
    <div className="title-container">
      <h1 className="title">Energiewerkstatt-Prüfstandsabnahme</h1>

      <div className="top-section">
        <div className="device-selector field-group">
          <label>Prüfstandswahl:</label>
          <select
            value={monitor.selectedDevice || ""}
            onChange={monitor.handleDeviceChange}
            disabled={monitor.selectedDevice}
          >
            <option value="">----Auswahl----</option>
            {monitor.testStations.length > 0 ? (
              monitor.testStations.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.testStationName}
                </option>
              ))
            ) : (
              <option value="">Keine Geräte verfügbar</option>
            )}
          </select>
        </div>

        <div className="machine-type-selector field-group">
          <label>Maschinentyp:</label>
          <select
            value={monitor.selectedMachineType}
            onChange={monitor.handleMachineTypeChange}
            disabled={monitor.isRunning || !monitor.selectedDevice || monitor.isReset}
          >
            <option value="">----Auswahl----</option>

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



          <label>Auto-Stopp-Dauer:</label>
          <select
            value={monitor.autoStopMinutes}
            onChange={monitor.handleAutoStopMinutes}
            disabled={monitor.isRunning || monitor.isReset} // disable when running
          >
            {monitor.durationOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

        </div>



        <div className="text-type-selector field-group">
          <label>Seriennummer :</label>
          <input
            type="text"
            value={monitor.serialNumber}
            onChange={monitor.handleInputChange}
            placeholder="Eingabe"
            disabled={monitor.isButtonDisabled || monitor.isRunning}
          />
          <button className="checkStatus" onClick={monitor.fetchStatus} disabled={monitor.isButtonDisabled || monitor.isRunning || !monitor.serialNumber}>Prüfe Produktstatus</button>

        </div>






        <div className="button-group">
          <button className="start"
            onClick={monitor.handleStartTask}
            disabled={monitor.isRunning || !monitor.isButtonDisabled || monitor.isReset}
          >
            Start
          </button>


          <button className="stop"
            onClick={monitor.handleStopTask}
            disabled={!monitor.isRunning}
          >
            Stopp
          </button>

          <button
            className="reset"
            onClick={monitor.handleReset}
            disabled={monitor.isRunning} // Disable reset if task is running
          >
            Zurücksetzen
          </button>

          <button
            className="ubertragen"
            onClick={monitor.handleUbertragen}
            disabled={!monitor.isButtonDisabled || monitor.isRunning || !monitor.isReset}

          >
            Übertragen
          </button>
        </div>


        <div className="clock-container">
          <Clock />
        </div>



        <div className="timer-container">
          <Timer
            elapsedTime={monitor.elapsedTime}
          />
        </div>

      </div>




      <div className="container">
        {/* Energy Table */}
        <div className="table-energy">
          <DeviceTable
            title="Energiezähler"
            dataType="Typ"
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
            title="Gaszähler und Wirkungsgrad"
            dataType="Typ"
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
            title="Wärmemengenzähler"
            dataType="Typ"
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

        <div className="chp-table">
          <DeviceTable
            title="BHKW Prozessdaten"
            dataType="Typ"
            dataKeys={chpDataKeys}
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
