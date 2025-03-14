import React, { useState, useEffect } from 'react';

function Timer({ isRunning }) {
  const [elapsedTime, setElapsedTime] = useState(0); // Track elapsed time

  useEffect(() => {
    if (!isRunning) return; // Exit early if not running

    const id = setInterval(() => {
      setElapsedTime((prevTime) => prevTime + 1); // Increment elapsed time
    }, 1000);

    // Cleanup function to clear the interval
    return () => clearInterval(id);
  }, [isRunning]); // Only depends on `isRunning`

  // Format the time into "hours:minutes:seconds"
  const formatTime = (time) => {
    const hours = Math.floor(time / 3600);
    const minutes = Math.floor((time % 3600) / 60);
    const seconds = time % 60;
    
    return `${hours > 0 ? hours + ':' : ''}${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  };

  return (
    <div>
      <h2>Time Elapsed: {formatTime(elapsedTime)}</h2>
    </div>
  );
}

export default Timer;
