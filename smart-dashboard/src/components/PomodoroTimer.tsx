import { useState, useRef, useEffect } from "react";
import { gsap } from "gsap";
import { Howl } from "howler";
import "../css/PomodoroTimer.scss";

interface PomodoroTimerProps {
  initialWorkMinutes?: number;
  initialBreakMinutes?: number;
}

type TimerMode = 'work' | 'break';

interface TimerState {
  minutes: number;
  seconds: number;
  mode: TimerMode;
  isActive: boolean;
  cycles: number;
}

export default function PomodoroTimer({
  initialWorkMinutes = 25,
  initialBreakMinutes = 5,
}: PomodoroTimerProps) {
  const [timerState, setTimerState] = useState<TimerState>({
    minutes: initialWorkMinutes,
    seconds: 0,
    mode: 'work',
    isActive: false,
    cycles: 0
  });
  const circleRef = useRef<SVGCircleElement>(null);
  const circumference = 2 * Math.PI * 90;
  
  // 添加桌面通知权限请求
  useEffect(() => {
    if ("Notification" in window) {
      Notification.requestPermission();
    }
  }, []);
  
  // 添加音效
  const alarmSound = useRef(new Howl({
    src: ["/sounds/timer-end.mp3"],
    volume: 0.5,
  }));

  useEffect(() => {
    if (timerState.isActive) {
      const timer = setInterval(() => {
        setTimerState((prev) => {
          if (prev.seconds === 0) {
            if (prev.minutes === 0) {
              alarmSound.current.play();
              
              // 发送桌面通知
              if ("Notification" in window && Notification.permission === "granted") {
                new Notification(prev.mode === 'work' ? "工作时间结束！" : "休息时间结束！", {
                  body: prev.mode === 'work' ? "该休息一下了" : "该开始工作了",
                  icon: "/favicon.ico"
                });
              }

              // 切换模式
              const nextMode = prev.mode === 'work' ? 'break' : 'work';
              const nextMinutes = nextMode === 'work' ? initialWorkMinutes : initialBreakMinutes;
              const nextCycles = prev.mode === 'break' ? prev.cycles + 1 : prev.cycles;

              return {
                ...prev,
                minutes: nextMinutes,
                seconds: 0,
                mode: nextMode,
                cycles: nextCycles
              };
            }
            return { ...prev, minutes: prev.minutes - 1, seconds: 59 };
          }
          return { ...prev, seconds: prev.seconds - 1 };
        });
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [timerState.isActive, initialWorkMinutes, initialBreakMinutes]);

  useEffect(() => {
    if (circleRef.current) {
      const totalSeconds = (timerState.mode === 'work' ? initialWorkMinutes : initialBreakMinutes) * 60;
      const currentSeconds = timerState.minutes * 60 + timerState.seconds;
      const progress = (totalSeconds - currentSeconds) / totalSeconds;

      gsap.to(circleRef.current, {
        strokeDashoffset: circumference * (1 - progress),
        duration: 1,
        ease: "power2.out",
      });
    }
  }, [timerState.minutes, timerState.seconds, circumference, initialWorkMinutes, initialBreakMinutes, timerState.mode]);

  const toggleTimer = () => setTimerState(prev => ({ ...prev, isActive: !prev.isActive }));
  const resetTimer = () => {
    setTimerState({
      minutes: initialWorkMinutes,
      seconds: 0,
      mode: 'work',
      isActive: false,
      cycles: 0
    });
  };
  return (
    <div className="pomodoro-timer">
      <div className="pomodoro-timer__status">
        <span className={`pomodoro-timer__status-badge pomodoro-timer__status-badge--${timerState.mode}`}>
          {timerState.mode === 'work' ? '工作中' : '休息中'}
        </span>
        <span className="pomodoro-timer__status-cycles">
          完成周期: {timerState.cycles}
        </span>
      </div>
      <div className="pomodoro-timer__display">
        <svg className="pomodoro-timer__display-ring" width="200" height="200">
          <circle
            className="pomodoro-timer__display-ring-background"
            stroke="currentColor"
            strokeWidth="8"
            fill="transparent"
            r="90"
            cx="100"
            cy="100"
          />
          <circle
            ref={circleRef}
            className="pomodoro-timer__display-ring-progress"
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
        <div className="pomodoro-timer__display-time">
          <div className="pomodoro-timer__display-time-text">
            {String(timerState.minutes).padStart(2, "0")}:
            {String(timerState.seconds).padStart(2, "0")}
          </div>
          <div className="pomodoro-timer__controls">
            <button
              onClick={toggleTimer}
              className="pomodoro-timer__controls-button pomodoro-timer__controls-button--primary"
            >
              {timerState.isActive ? "暂停" : "开始"}
            </button>
            <button
              onClick={resetTimer}
              className="pomodoro-timer__controls-button pomodoro-timer__controls-button--secondary"
            >
              重置
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
