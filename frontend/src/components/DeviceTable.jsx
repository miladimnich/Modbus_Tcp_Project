import React, { useMemo } from "react";
import { unitMapping, displayNameMapping } from "../constants/calculationTypes";

const DeviceTable = ({
  title,
  dataType,
  dataKeys,
  firstValues,
  currentValues,
  lastValues,
  difference,
  outOfRangeState,
  mergedBorders,
}) => {
  // Memoize the rows to avoid unnecessary re-renders when `currentValues` hasn't changed
  const memoizedRows = useMemo(() => {
    return dataKeys.map((key) => {
      // Get display name from the mapping
      const displayName =
        displayNameMapping[key] ?? `No display name for ${key}`;
      console.log(`Key: ${key}, Display Name: ${displayName}`); // Log to debug

      return (
        <tr key={key}>
          {/* Display name column */}
          <td>{displayName}</td>
          {/* Unit column */}
          <td>{unitMapping[key] ?? "N/A"}</td>
          {/* First Value column */}
          <td>{firstValues?.[key] ?? "N/A"}</td>
          {/* Current Value column */}
          <td>
            {currentValues?.[key]?.data !== null &&
            currentValues?.[key]?.data !== undefined &&
            !isNaN(currentValues[key].data)
              ? currentValues[key].data
              : "N/A"}
          </td>
          {/* Last Value column */}
          <td>{lastValues?.[key] ?? "N/A"}</td>
          {/* Difference column */}
          <td>
            {difference?.[key] !== undefined && !isNaN(difference[key])
              ? difference[key]
              : "N/A"}
          </td>
          {/* Min value column */}
          <td className={outOfRangeState?.[key]?.min ? "out-of-range" : ""}>
            {mergedBorders?.[key]?.min ?? "N/A"}
          </td>
          {/* Target column */}
          <td>{mergedBorders?.[key]?.soll ?? "N/A"}</td>
          {/* Max value column */}
          <td className={outOfRangeState?.[key]?.max ? "out-of-range" : ""}>
            {mergedBorders?.[key]?.max ?? "N/A"}
          </td>
        </tr>
      );
    });
  }, [
    dataKeys,
    firstValues,
    currentValues,
    lastValues,
    difference,
    outOfRangeState,
    mergedBorders,
  ]);

  return (
    <div className="device-table">
      <h2 className="section-title">{title}</h2>
      <table className="table">
        <thead>
          <tr>
            <th>{dataType} Type</th>
            <th>Unit</th>
            <th>First Value</th>
            <th>Current Value</th>
            <th>Last Value</th>
            <th>Difference</th>
            <th>Min</th>
            <th>Target</th>
            <th>Max</th>
          </tr>
        </thead>
        <tbody>
          {dataKeys?.length > 0 ? (
            memoizedRows
          ) : (
            <tr>
              <td colSpan="9">No data available</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default DeviceTable;
