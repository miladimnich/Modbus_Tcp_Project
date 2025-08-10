function Timer({ elapsedTime }) {
  // Format the time into "minutes:seconds" format
  const formatTime = (time) => {
    if (!Number.isFinite(time) || time < 0) return "00:00:00";
    const hours = Math.floor(time / 3600);
    const minutes = Math.floor((time % 3600) / 60);
    const seconds = Math.floor(time % 60);
    return `${hours < 10 ? '0' : ''}${hours}:${minutes < 10 ? '0' : ''}${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  };

  return (
    <div>
      <span>Messung aktiv: {formatTime(elapsedTime)}</span>
    </div>
  );
}

export default Timer;