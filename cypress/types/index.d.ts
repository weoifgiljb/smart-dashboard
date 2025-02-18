// cypress/support/index.d.ts
/// <reference types="cypress" />

export type TaskStatus = "todo" | "progress" | "done";

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * 自定义拦截器（兼容最新Cypress类型）
       */
      intercept(
        method: string,
        url: string | RegExp,
        handler?: {
          statusCode?: number;
          body?: any;
          fixture?: string;
        }
      ): Chainable<null>;
    }
  }

  // 天气接口响应类型
  interface WeatherResponse {
    name: string;
    main: {
      temp: number;
      feels_like: number;
      humidity: number;
    };
    weather: Array<{
      id: number;
      main: string;
      description: string;
      icon: string;
    }>;
    wind: {
      speed: number;
    };
  }
}

export {};
export interface Task {
  id?: number;
  title: string;
  position: number;
  completed: boolean;
  status: TaskStatus;
  createdAt: Date;
  dueDate?: Date;
}

export interface DashboardDB {
  transaction: (
    mode: string,
    tables: unknown,
    callback: () => Promise<void>
  ) => Promise<void>;
  tasks: {
    toArray: () => Promise<Task[]>;
    update: (id: number, changes: Partial<Task>) => Promise<number>;
  };
}
