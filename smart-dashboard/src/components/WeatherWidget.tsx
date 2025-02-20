// src/components/WeatherWidget.tsx
import { useState, useEffect } from "react";
import { FaLocationDot } from "react-icons/fa6";
import "../css/WeatherWidget.scss";

// 类型定义
interface WeatherData {
  name: string;
  main: {
    temp: number;
    feels_like: number;
    humidity: number;
  };
  weather: [
    {
      id: number;
      main: string;
      description: string;
      icon: string;
    },
  ];
  wind: {
    speed: number;
  };
}

interface GeoPosition {
  coords: {
    latitude: number;
    longitude: number;
  };
}

const DEFAULT_COORDS = {
  lat: Number(import.meta.env.VITE_DEFAULT_LAT || 31.2304),
  lon: Number(import.meta.env.VITE_DEFAULT_LON || 121.4737),
};

async function fetchWeather(lat: number, lon: number): Promise<WeatherData> {
  const response = await fetch(
    `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${
      import.meta.env.VITE_OPENWEATHER_API_KEY
    }&units=metric`
  );

  if (!response.ok) {
    throw new Error(`天气请求失败: ${response.status}`);
  }

  return response.json();
}

async function getGeolocation(): Promise<GeoPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject);
  });
}

export default function WeatherWidget() {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadWeather = async () => {
      try {
        let position: GeoPosition;

        try {
          position = await getGeolocation();
        } catch (geoError) {
          console.warn("使用默认坐标:", geoError);
          position = {
            coords: {
              latitude: DEFAULT_COORDS.lat,
              longitude: DEFAULT_COORDS.lon,
            },
          };
        }

        const data = await fetchWeather(
          position.coords.latitude,
          position.coords.longitude
        );

        setWeather(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : "无法获取天气数据");
      } finally {
        setLoading(false);
      }
    };

    loadWeather();
  }, []);

  if (loading) {
    return (
      <div className="weather-card loading p-4 rounded-lg">
        <div className="skeleton h-6 w-3/4 mb-4" />
        <div className="skeleton h-4 w-full mb-2" />
      </div>
    );
  }

  if (error || !weather) {
    return (
      <div className="weather-card error p-4 rounded-lg">
        <p className="text-red-600 dark:text-red-400">⚠️ {error}</p>
      </div>
    );
  }

  return (
    <div className="weather-widget">
      <div className="weather-card p-2 rounded-lg">
        <header className="location-header flex items-center gap-2 mb-2">
          <FaLocationDot className="location-icon shrink-0" />
          <h2 className="location-name">{weather.name}</h2>
        </header>

        <div className="weather-info grid grid-cols-[auto_1fr] gap-2 items-center">
          <img
            src={`https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png`}
            alt={weather.weather[0].description}
            className="weather-icon w-12 h-12"
            loading="lazy"
          />

          <div className="space-y-0.5">
            <p className="temperature">{Math.round(weather.main.temp)}°C</p>
            <p className="description">{weather.weather[0].description}</p>
          </div>
        </div>

        <div className="weather-details">
          <div className="detail-item">
            <p className="label">体感温度</p>
            <p className="value">{Math.round(weather.main.feels_like)}°C</p>
          </div>
          <div className="detail-item">
            <p className="label">湿度</p>
            <p className="value">{weather.main.humidity}%</p>
          </div>
          <div className="detail-item">
            <p className="label">风速</p>
            <p className="value">{weather.wind.speed}m/s</p>
          </div>
        </div>
      </div>
    </div>
  );
}
