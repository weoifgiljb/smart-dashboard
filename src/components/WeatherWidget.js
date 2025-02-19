import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/components/WeatherWidget.tsx
import { useState, useEffect } from "react";
import { FaLocationDot } from "react-icons/fa6";
const DEFAULT_COORDS = {
    lat: Number(import.meta.env.VITE_DEFAULT_LAT || 31.2304),
    lon: Number(import.meta.env.VITE_DEFAULT_LON || 121.4737),
};
async function fetchWeather(lat, lon) {
    const response = await fetch(`https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${import.meta.env.VITE_OPENWEATHER_API_KEY}&units=metric`);
    if (!response.ok) {
        throw new Error(`天气请求失败: ${response.status}`);
    }
    return response.json();
}
async function getGeolocation() {
    return new Promise((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject);
    });
}
export default function WeatherWidget() {
    const [weather, setWeather] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    useEffect(() => {
        const loadWeather = async () => {
            try {
                let position;
                try {
                    position = await getGeolocation();
                }
                catch (geoError) {
                    console.warn("使用默认坐标:", geoError);
                    position = {
                        coords: {
                            latitude: DEFAULT_COORDS.lat,
                            longitude: DEFAULT_COORDS.lon,
                        },
                    };
                }
                const data = await fetchWeather(position.coords.latitude, position.coords.longitude);
                setWeather(data);
            }
            catch (err) {
                setError(err instanceof Error ? err.message : "无法获取天气数据");
            }
            finally {
                setLoading(false);
            }
        };
        loadWeather();
    }, []);
    if (loading) {
        return (_jsxs("div", { className: "weather-card animate-pulse p-4 rounded-lg bg-gray-100 dark:bg-gray-800", children: [_jsx("div", { className: "h-6 bg-gray-300 dark:bg-gray-700 rounded w-3/4 mb-4" }), _jsx("div", { className: "h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2" })] }));
    }
    if (error || !weather) {
        return (_jsx("div", { className: "weather-card p-4 rounded-lg bg-red-50 dark:bg-red-900/20", children: _jsxs("p", { className: "text-red-600 dark:text-red-400", children: ["\u26A0\uFE0F ", error] }) }));
    }
    return (_jsxs("div", { className: "weather-card p-4 rounded-lg bg-white dark:bg-gray-800 shadow-md hover:shadow-lg transition-shadow", children: [_jsxs("header", { className: "flex items-center gap-2 mb-4", children: [_jsx(FaLocationDot, { className: "text-blue-500 shrink-0" }), _jsx("h2", { className: "font-semibold truncate", children: weather.name })] }), _jsxs("div", { className: "grid grid-cols-[auto_1fr] gap-4 items-center", children: [_jsx("img", { src: `https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png`, alt: weather.weather[0].description, className: "w-16 h-16", loading: "lazy" }), _jsxs("div", { className: "space-y-1", children: [_jsxs("p", { className: "text-2xl font-bold", children: [Math.round(weather.main.temp), "\u00B0C"] }), _jsx("p", { className: "text-sm text-gray-600 dark:text-gray-400 capitalize", children: weather.weather[0].description })] })] }), _jsxs("div", { className: "grid grid-cols-3 gap-4 mt-4 text-sm", children: [_jsxs("div", { className: "text-center", children: [_jsx("p", { className: "font-medium", children: "\u4F53\u611F\u6E29\u5EA6" }), _jsxs("p", { children: [Math.round(weather.main.feels_like), "\u00B0C"] })] }), _jsxs("div", { className: "text-center", children: [_jsx("p", { className: "font-medium", children: "\u6E7F\u5EA6" }), _jsxs("p", { children: [weather.main.humidity, "%"] })] }), _jsxs("div", { className: "text-center", children: [_jsx("p", { className: "font-medium", children: "\u98CE\u901F" }), _jsxs("p", { children: [weather.wind.speed, "m/s"] })] })] })] }));
}
//# sourceMappingURL=WeatherWidget.js.map