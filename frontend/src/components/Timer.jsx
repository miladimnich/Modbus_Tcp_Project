import React from "react";

function Timer({ elapsedTime }) {
  // Format the time into "minutes:seconds" format
  const formatTime = (time) => {
    const hours = Math.floor(time / 3600);
    const minutes = Math.floor((time % 3600) / 60);
    const seconds = Math.floor(time % 60);
    return `${hours < 10 ? "0" : ""}${hours}:${
      minutes < 10 ? "0" : ""
    }${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;
  };

  return (
    <div>
      <span>Time Elapsed: {formatTime(elapsedTime)}</span>
    </div>
  );
}

export default Timer;
