import React from 'react';

const DeviceTable = ({ title, dataType, dataKeys, firstValues, currentValues, lastValues, difference }) => {
  return (
    <div>
      <h2>{title}</h2>
      <table className='table'>
        <thead>
          <tr>
            <th>{dataType} Type</th>
            <th>First Value</th>
            <th>Current Value</th>
            <th>Last Value</th>
            <th>Difference</th>
          </tr>
        </thead>
        <tbody>
          {/* Render all keys from EnergyCalculationType or HeatingCalculationTypes */}
          {dataKeys && dataKeys.length > 0 ? (
            dataKeys.map((key) => (
              <tr key={key}>
                <td>{key}</td>
                <td> {firstValues && firstValues[key] !== undefined ? firstValues[key] : "N/A"}</td>
               
                <td> {currentValues && currentValues[key] && currentValues[key].data !== undefined
                    ? currentValues[key].data
                    : "N/A"}</td>
                <td>{lastValues && lastValues[key] !== undefined ? lastValues[key] : "N/A"}</td>
                <td> {difference && difference[key] !== undefined ? difference[key] : "N/A"}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5">No data available</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default DeviceTable;

