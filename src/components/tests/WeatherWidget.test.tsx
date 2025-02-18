import { describe, it, expect, vi, beforeEach, Mock } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import WeatherWidget from "../WeatherWidget";

// 模拟 API 数据
const mockWeatherData = {
  name: "上海",
  main: {
    temp: 25,
    feels_like: 27,
    humidity: 65,
  },
  weather: [
    {
      id: 800,
      main: "Clear",
      description: "晴天",
      icon: "01d",
    },
  ],
  wind: {
    speed: 3.2,
  },
};

interface TestGeolocationError extends Error {
  code: number;
  PERMISSION_DENIED: 1;
  POSITION_UNAVAILABLE: 2;
  TIMEOUT: 3;
}

beforeEach(() => {
  const mockGeolocation = {
    getCurrentPosition: vi.fn(),
  };

  Object.defineProperty(navigator, "geolocation", {
    value: mockGeolocation,
    writable: true,
  });

  global.fetch = vi.fn().mockResolvedValueOnce({
    // 默认添加一个成功响应
    ok: true,
    json: () => Promise.resolve(mockWeatherData),
  });
});

describe("WeatherWidget 组件测试", () => {
  it("处理地理位置错误并显示默认位置", async () => {
    const mockError: TestGeolocationError = {
      name: "GeolocationPositionError",
      message: "地理位置不可用",
      code: 1,
      PERMISSION_DENIED: 1,
      POSITION_UNAVAILABLE: 2,
      TIMEOUT: 3,
    };

    (navigator.geolocation.getCurrentPosition as Mock).mockImplementationOnce(
      (
        _: PositionCallback,
        reject?: (error: GeolocationPositionError) => void
      ) => {
        if (reject) {
          reject(mockError as unknown as GeolocationPositionError);
        }
      }
    );

    render(<WeatherWidget />);

    await waitFor(() => {
      expect(screen.getByText(/上海/i)).toBeInTheDocument();
      expect(screen.getByText(/25°C/i)).toBeInTheDocument();
    });
  });

  it("处理API请求错误", async () => {
    // 1. 模拟地理位置获取失败
    const mockGeolocationError = {
      code: 1,
      message: "地理位置不可用",
      PERMISSION_DENIED: 1,
      POSITION_UNAVAILABLE: 2,
      TIMEOUT: 3,
    };

    (navigator.geolocation.getCurrentPosition as Mock).mockImplementationOnce(
      (
        _: PositionCallback,
        reject?: (error: GeolocationPositionError) => void
      ) => {
        if (reject) {
          reject(mockGeolocationError as unknown as GeolocationPositionError);
        }
      }
    );

    // 2. 模拟API错误响应
    (global.fetch as Mock).mockReset(); // 清除默认的成功mock
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
    });

    render(<WeatherWidget />);

    // 3. 验证错误显示
    await waitFor(
      () => {
        const errorElement = screen.getByText(/⚠️.*500/);
        expect(errorElement).toBeInTheDocument();
      },
      { timeout: 5000 }
    );
  });
});
