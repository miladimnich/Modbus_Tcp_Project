import React, { useState, useEffect } from 'react';

// Format time as HH:MM:SS
const formatTime = (time) => {
    return time.toString().padStart(2, '0');
};

const Clock = () => {
    const [currentTime, setCurrentTime] = useState(new Date());

    useEffect(() => {
        const interval = setInterval(() => {
            setCurrentTime(new Date()); // Update time every second
        }, 1000);

        return () => clearInterval(interval); // Cleanup the interval on unmount
    }, []);

    const hours = formatTime(currentTime.getHours());
    const minutes = formatTime(currentTime.getMinutes());
    const seconds = formatTime(currentTime.getSeconds());
    const formattedTime = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    return (
        <div > {/* Container for time */}
            <span>Uhrzeit: {formattedTime}</span>
        </div>



    );
};

export default Clock;