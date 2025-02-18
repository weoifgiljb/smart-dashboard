import React, { useState, useRef, useEffect } from "react";
import { gsap } from "gsap";

interface PomodoroTimerProps {
  initialMinutes?: number;
}

export default function PomodoroTimer({
  initialMinutes = 25,
}: PomodoroTimerProps) {
  const [minutes, setMinutes] = useState(initialMinutes);
  const [seconds, setSeconds] = useState(0);
  const [isActive, setIsActive] = useState(false);
  const circleRef = useRef<SVGCircleElement>(null);
  const circumference = 2 * Math.PI * 90; // 半径90px的圆周长

  useEffect(() => {
    if (isActive) {
      const timer = setInterval(() => {
        setSeconds((prev) => {
          if (prev === 0) {
            setMinutes((prevMin) => {
              if (prevMin === 0) {
                setIsActive(false);
                return initialMinutes;
              }
              return prevMin - 1;
            });
            return 59;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [isActive, initialMinutes]);

  useEffect(() => {
    if (circleRef.current) {
      const totalSeconds = initialMinutes * 60;
      const currentSeconds = minutes * 60 + seconds;
      const progress = (totalSeconds - currentSeconds) / totalSeconds;

      gsap.to(circleRef.current, {
        strokeDashoffset: circumference * (1 - progress),
        duration: 1,
        ease: "power2.out",
      });
    }
  }, [minutes, seconds, circumference, initialMinutes]);

  const toggleTimer = () => setIsActive(!isActive);
  const resetTimer = () => {
    setIsActive(false);
    setMinutes(initialMinutes);
    setSeconds(0);
  };

  return (
    <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-lg">
      <div className="relative w-48 h-48 mx-auto">
        <svg className="progress-ring" width="200" height="200">
          <circle
            className="text-gray-200 dark:text-gray-600"
            stroke="currentColor"
            strokeWidth="8"
            fill="transparent"
            r="90"
            cx="100"
            cy="100"
          />
          <circle
            ref={circleRef}
            className="progress-ring__circle text-blue-500"
            stroke="currentColor"
            strokeWidth="8"
            fill="transparent"
            r="90"
            cx="100"
            cy="100"
            strokeDasharray={`${circumference} ${circumference}`}
            strokeDashoffset={circumference}
            strokeLinecap="round"
          />
        </svg>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-center">
          <div className="text-4xl font-bold dark:text-white">
            {String(minutes).padStart(2, "0")}:
            {String(seconds).padStart(2, "0")}
          </div>
          <div className="mt-4 space-x-2">
            <button
              onClick={toggleTimer}
              className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
            >
              {isActive ? "暂停" : "开始"}
            </button>
            <button
              onClick={resetTimer}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-600 text-gray-700 dark:text-gray-200 rounded hover:bg-gray-300 dark:hover:bg-gray-500 transition-colors"
            >
              重置
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
