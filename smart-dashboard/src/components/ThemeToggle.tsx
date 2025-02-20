import { useState, useEffect } from 'react';
import './ThemeToggle.scss';

export default function ThemeToggle() {
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    // 检查系统颜色模式
    const darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    setIsDark(darkModeMediaQuery.matches);

    // 监听系统颜色模式变化
    const handleChange = (e: MediaQueryListEvent) => setIsDark(e.matches);
    darkModeMediaQuery.addEventListener('change', handleChange);

    return () => darkModeMediaQuery.removeEventListener('change', handleChange);
  }, []);

  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle('dark');
  };

  return (
    <button
      onClick={toggleTheme}
      className="theme-toggle"
      aria-label="切换暗色模式"
      title="切换暗色模式"
    >
      <span className="emoji-icon">{isDark ? '🌞' : '🌙'}</span>
    </button>
  );
}