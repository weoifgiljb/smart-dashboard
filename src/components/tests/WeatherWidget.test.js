import { jsx as _jsx } from "react/jsx-runtime";
import { describe, it, expect, vi, beforeEach } from "vitest";
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
        const mockError = {
            name: "GeolocationPositionError",
            message: "地理位置不可用",
            code: 1,
            PERMISSION_DENIED: 1,
            POSITION_UNAVAILABLE: 2,
            TIMEOUT: 3,
        };
        navigator.geolocation.getCurrentPosition.mockImplementationOnce((_, reject) => {
            if (reject) {
                reject(mockError);
            }
        });
        render(_jsx(WeatherWidget, {}));
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
        navigator.geolocation.getCurrentPosition.mockImplementationOnce((_, reject) => {
            if (reject) {
                reject(mockGeolocationError);
            }
        });
        // 2. 模拟API错误响应
        global.fetch.mockReset(); // 清除默认的成功mock
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 500,
        });
        render(_jsx(WeatherWidget, {}));
        // 3. 验证错误显示
        await waitFor(() => {
            const errorElement = screen.getByText(/⚠️.*500/);
            expect(errorElement).toBeInTheDocument();
        }, { timeout: 5000 });
    });
});
//# sourceMappingURL=WeatherWidget.test.js.map